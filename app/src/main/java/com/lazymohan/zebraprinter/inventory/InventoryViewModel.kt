package com.lazymohan.zebraprinter.inventory

import android.app.Application
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazymohan.zebraprinter.inventory.data.OnHandCsv
import com.lazymohan.zebraprinter.inventory.data.OnHandRow
import com.lazymohan.zebraprinter.inventory.util.parseGs1
import com.lazymohan.zebraprinter.graph.drive.OneDriveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val app: Application,
    private val drive: OneDriveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState

    val snackHost = SnackbarHostState()

    @Volatile private var templateHeader: String = "Transaction Id,Physical Onhand"

    private fun normalizeCsvExpiry(raw: String): String {
        val digits = raw.replace(Regex("[^0-9]"), "")
        return if (digits.length == 8 && raw.startsWith("00-")) {
            val dd = digits.substring(0, 2)
            val mm = digits.substring(2, 4)
            val yyyy = digits.substring(4, 8)
            "$yyyy-$mm-$dd"
        } else raw
    }

    private var onHandByGtinLotExpiry: Map<Triple<String, String, String>, OnHandRow> = emptyMap()

    private val countFile: File by lazy {
        File(app.filesDir, "inventory/PhysicalCountTemplate.csv")
    }

    init {
        // Load master and header template
        loadOnHandFromDriveOrFallback()
        loadHeaderFromDriveOrFallback()
        // loadCounts()
    }

    /** Clear all locally saved counts and reset UI state. */
    fun clearCounts() {
        _uiState.value = _uiState.value.copy(counts = emptyList(), matchDialog = null)
        runCatching {
            if (countFile.exists()) countFile.delete()
        }
    }

    private fun loadOnHandFromDriveOrFallback() = viewModelScope.launch(Dispatchers.IO) {
        fun indexRows(rows: List<OnHandRow>) {
            onHandByGtinLotExpiry = rows
                .asSequence()
                .filter { !it.gtin.isNullOrBlank() && it.lotNumber.isNotBlank() && it.expiryIso.isNotBlank() }
                .associateBy {
                    Triple(
                        it.gtin!!.trim(),
                        it.lotNumber.trim(),
                        normalizeCsvExpiry(it.expiryIso)
                    )
                }
        }

        runCatching {
            val bytes = drive.downloadMasterCsv()
            OnHandCsv.readFromBytes(bytes)
        }.onSuccess { rows ->
            indexRows(rows)
            _uiState.value = _uiState.value.copy(onHandLoaded = true)
            postSnack("Loaded ${onHandByGtinLotExpiry.size} items from OneDrive")
        }.onFailure { e ->
            runCatching {
                val rows = OnHandCsv.readFromAssets(app, "inventory/OnHandExport.csv")
                indexRows(rows)
                _uiState.value = _uiState.value.copy(onHandLoaded = true)
                postSnack("Loaded ${onHandByGtinLotExpiry.size} items from assets (drive error: ${e.message})")
            }.onFailure { ex ->
                postSnack("Failed to load items: ${ex.message}")
            }
        }
    }

    private fun loadHeaderFromDriveOrFallback() = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            val bytes = drive.downloadCountTemplateCsv()
            val header = bytes.toString(Charset.defaultCharset())
                .lineSequence()
                .firstOrNull()
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            if (!header.isNullOrEmpty()) templateHeader = header
        }.onFailure {
        }
    }

    fun handleRawScan(raw: String) = viewModelScope.launch {
        val r = runCatching { parseGs1(raw) }.getOrNull()

        val gtin = r?.gtin?.trim()
        val lot = r?.lot?.trim()
        val expiryIsoKey = r?.expiryYmd?.let { normalizeCsvExpiry(it) }

        // Strict requirement: GTIN + Lot + Expiry must be present in the QR
        if (gtin.isNullOrBlank() || lot.isNullOrBlank() || expiryIsoKey.isNullOrBlank()) {
            postSnack("QR not recognised. Expecting GS1 with GTIN, Lot and Expiry.")
            return@launch
        }

        val row = onHandByGtinLotExpiry[Triple(gtin, lot, expiryIsoKey)]
        if (row == null) {
            postSnack("Product does not exist (gtin=$gtin, lot=$lot, exp=$expiryIsoKey).")
            return@launch
        }

        _uiState.value = _uiState.value.copy(
            matchDialog = MatchDialogData(
                transactionId = row.transactionId,
                description = row.description,
                gtin = gtin,
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
        runCatching {
            countFile.parentFile?.mkdirs()
            countFile.printWriter().use { w ->
                w.println(templateHeader)
                data.forEach { (txn, qty) -> w.println("$txn,$qty") }
            }
        }.onFailure { postSnack("Failed to persist counts: ${it.message}") }
    }

    fun dismissDialog() { _uiState.value = _uiState.value.copy(matchDialog = null) }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun exportCsv() = viewModelScope.launch {
        val data = _uiState.value.counts
        val scanned = data.size
        if (scanned == 0) {
            snackHost.showSnackbar("No items scanned.")
            return@launch
        }

        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
        val fileName = "PhysicalCount_${timestamp}.csv"
        val csv = withContext(Dispatchers.Default) {
            buildString {
                appendLine(templateHeader)
                data.forEach { (txn, qty) -> appendLine("$txn,$qty") }
            }
        }

        // 1) Upload to OneDrive
        val uploadResult = runCatching {
            drive.uploadCountsFromString(csv, fileName)
        }

        if (uploadResult.isFailure) {
            postSnack("Upload failed: ${uploadResult.exceptionOrNull()?.message}")
            return@launch
        }

        // 2) Also save to local Downloads for user convenience
        runCatching {
            val resolver = app.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Unable to create CSV file in Downloads.")

            resolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os).use { w ->
                    w.appendLine(templateHeader)
                    data.forEach { (txn, qty) -> w.appendLine("$txn,$qty") }
                }
            }
        }

        // 3) Reset counts after successful export
        clearCounts()

        snackHost.showSnackbar("Exported $scanned items • Uploaded to OneDrive as $fileName")
    }

    fun postSnack(msg: String) = viewModelScope.launch { snackHost.showSnackbar(msg) }
}
