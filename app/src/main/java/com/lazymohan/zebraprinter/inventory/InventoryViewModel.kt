package com.lazymohan.zebraprinter.inventory

import android.app.Application
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics

import com.lazymohan.zebraprinter.inventory.data.OnHandCsv
import com.lazymohan.zebraprinter.inventory.data.OnHandRow
import com.lazymohan.zebraprinter.inventory.util.parseGs1
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStreamWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val app: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState

    val snackHost = SnackbarHostState()

    private fun normalizeCsvExpiry(raw: String): String {
        val digits = raw.replace(Regex("[^0-9]"), "")
        return if (digits.length == 8 && raw.startsWith("00-")) {
            // Case: CSV = "00-03-2027" -> want "2027-03-00"
            val dd = digits.substring(0, 2)
            val mm = digits.substring(2, 4)
            val yyyy = digits.substring(4, 8)
            "$yyyy-$mm-$dd"
        } else raw
    }

    private var onHandByLotExpiry: Map<Pair<String, String>, OnHandRow> = emptyMap()

    // Local file to persist counts
    private val countFile: File by lazy {
        File(app.filesDir, "inventory/PhysicalCountTemplate.csv")
    }




    init {
        loadOnHandCsv()
        loadCounts() // restore persisted counts
    }

    fun goToActions() {
        _uiState.value = _uiState.value.copy(stage = InventoryStage.Actions)
    }

    fun backToChoose() {
        _uiState.value = _uiState.value.copy(stage = InventoryStage.Choose)
    }

    private fun loadOnHandCsv() = viewModelScope.launch {
        try {
            val rows = OnHandCsv.readFromAssets(app, "inventory/OnHandExport.csv")
            onHandByLotExpiry = rows.associateBy {
                it.lotNumber.trim() to normalizeCsvExpiry(it.expiryIso)
            }
            _uiState.value = _uiState.value.copy(onHandLoaded = true)
        } catch (e: Exception) {
            Firebase.crashlytics.recordException(e)
            postSnack("Failed to load OnHandExport.csv: ${e.message}")
        }
    }

    private fun loadCounts() {
        if (!countFile.exists()) return
        try {
            val list = countFile.readLines()
                .drop(1) // skip header
                .mapNotNull { line ->
                    val parts = line.split(",")
                    if (parts.size == 2) {
                        val txn = parts[0].toLongOrNull()
                        val qty = parts[1].toIntOrNull()
                        if (txn != null && qty != null) txn to qty else null
                    } else null
                }
            _uiState.value = _uiState.value.copy(counts = list)
        } catch (e: Exception) {
            Firebase.crashlytics.recordException(e)
            postSnack("Failed to load saved counts: ${e.message}")
        }
    }

    fun handleRawScan(raw: String) = viewModelScope.launch {
        val r = runCatching { parseGs1(raw) }.getOrNull()
        val lot = r?.lot
        val expiryIso = r?.expiryYmd
        if (lot.isNullOrBlank() || expiryIso.isNullOrBlank()) {
            postSnack("QR code not recognised (expecting GS1 with Lot and Expiry).")
            return@launch
        }

        val row = onHandByLotExpiry[lot to expiryIso]
        if (row == null) {
            postSnack("Product does not exist (lot=$lot, exp=$expiryIso).")
            return@launch
        }

        _uiState.value = _uiState.value.copy(
            matchDialog = MatchDialogData(
                transactionId = row.transactionId,
                description = row.description,
                gtin = r.gtin,
                lot = row.lotNumber,
                expiryIso = row.expiryIso
            )
        )
    }

    fun saveQty(transactionId: Long, qty: Int) {
        val newList = _uiState.value.counts.toMutableList()
        newList.add(transactionId to qty)
        _uiState.value = _uiState.value.copy(counts = newList, matchDialog = null)
        persistCounts(newList)
        viewModelScope.launch { snackHost.showSnackbar("Saved: $transactionId -> $qty EA") }
    }

    private fun persistCounts(data: List<Pair<Long, Int>>) {
        try {
            countFile.parentFile?.mkdirs()
            countFile.printWriter().use { w ->
                w.println("Transaction Id,Physical Onhand")
                data.forEach { (txn, qty) -> w.println("$txn,$qty") }
            }
        } catch (e: Exception) {
            Firebase.crashlytics.recordException(e)
            postSnack("Failed to persist counts: ${e.message}")
        }
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(matchDialog = null)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun exportCsv() = viewModelScope.launch {
        val data = _uiState.value.counts
        val fileName = "PhysicalCountTemplate_${
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
        }.csv"

        val resolver = app.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: run {
                postSnack("Unable to create CSV file.")
                return@launch
            }

        resolver.openOutputStream(uri)?.use { os ->
            OutputStreamWriter(os).use { w ->
                w.appendLine("Transaction Id,Physical Onhand")
                data.forEach { (txn, qty) -> w.appendLine("$txn,$qty") }
            }
        }
        snackHost.showSnackbar("Exported ${data.size} rows → $fileName")
    }

    fun postSnack(msg: String) = viewModelScope.launch { snackHost.showSnackbar(msg) }
}
