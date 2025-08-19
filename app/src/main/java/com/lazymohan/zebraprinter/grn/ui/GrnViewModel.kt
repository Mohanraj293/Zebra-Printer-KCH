// app/src/main/java/com/lazymohan/zebraprinter/grn/ui/GrnViewModel.kt
package com.lazymohan.zebraprinter.grn.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazymohan.zebraprinter.BuildConfig
import com.lazymohan.zebraprinter.grn.data.*
import com.lazymohan.zebraprinter.grn.util.bestMatchIndex
import com.lazymohan.zebraprinter.grn.util.parseToIso
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.min

enum class GrnStep { ENTER_PO, SHOW_PO, REVIEW, SUMMARY }

data class LineInput(
    val lineNumber: Int,
    val itemNumber: String,
    val uom: String,
    val maxQty: Double,
    val qty: Double = 1.0,
    val lot: String = "BATCH20250811",
    val expiry: String = "2026-08-15",
    val description: String = ""
)

data class GrnUiState(
    val step: GrnStep = GrnStep.ENTER_PO,
    val loading: Boolean = false,
    val error: String? = null,
    val poNumber: String = "",
    val po: PoItem? = null,

    // visible lines in UI (matched-only in scan mode)
    val lines: List<PoLineItem> = emptyList(),
    val lineInputs: List<LineInput> = emptyList(),

    // keep the full PO lines for add-line dialog
    val allPoLines: List<PoLineItem> = emptyList(),
    // whether lines fetch is completed (to distinguish from “loading”)
    val linesLoaded: Boolean = false,

    val payload: ReceiptRequest? = null,
    val receipt: ReceiptResponse? = null,
    val lineErrors: List<ProcessingError> = emptyList(),

    // scan payload (unchanged)
    val extractedFromScan: List<com.lazymohan.zebraprinter.grn.util.ExtractedItem> = emptyList(),

    // NEW: cache file paths for scanned images (from ScanResultScreen)
    val scanImageCachePaths: List<String> = emptyList(),

    // NEW: count of successfully uploaded attachments (informational)
    val attachmentsUploaded: Int = 0
)

@HiltViewModel
class GrnViewModel @Inject constructor(
    private val repo: GrnRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GrnUiState())
    val state: StateFlow<GrnUiState> = _state

    fun prefillPoNumber(po: String?) {
        if (!po.isNullOrBlank()) _state.value = _state.value.copy(poNumber = po)
    }

    fun setPoNumber(po: String) { _state.value = _state.value.copy(poNumber = po) }

    fun fetchPo() = viewModelScope.launch {
        val s = _state.value
        if (s.poNumber.isBlank()) return@launch
        _state.value = s.copy(loading = true, error = null)
        repo.fetchPo(s.poNumber)
            .onSuccess { po ->
                Log.d("GRN", "PO fetched: ${po.OrderNumber}, header=${po.POHeaderId}")
                _state.value = _state.value.copy(loading = false, po = po, step = GrnStep.SHOW_PO)
                prefetchPoLines(po.POHeaderId)
            }
            .onFailure { e ->
                Log.e("GRN", "fetchPo failed: ${e.message}")
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Failed to load PO")
            }
    }

    private fun prefetchPoLines(headerId: String) = viewModelScope.launch {
        Log.d("GRN", "Prefetch PO lines for headerId=$headerId")
        repo.fetchPoLines(headerId)
            .onSuccess { items ->
                val extracted = _state.value.extractedFromScan
                Log.d("GRN", "Fetched ${items.size} PO lines. Scan items=${extracted.size}")

                if (extracted.isNotEmpty()) {
                    // Scan-driven: show only matched lines
                    val used = mutableSetOf<Int>()
                    val matchedPairs = items.mapNotNull { line ->
                        val safeItem = line.Item?.trim().orEmpty()
                        val poDesc = (line.Description ?: safeItem).trim()
                        val matchIdx = bestMatchIndex(extracted, poDesc, used, threshold = 0.35)
                        if (matchIdx != null) {
                            used += matchIdx
                            val ex = extracted[matchIdx]
                            val isoExpiry = parseToIso(ex.expiryDate)
                            val clampedQty = min(ex.qtyDelivered, line.Quantity)
                            val input = LineInput(
                                lineNumber = line.LineNumber,
                                itemNumber = safeItem,
                                uom = line.UOM,
                                maxQty = line.Quantity,
                                qty = clampedQty,
                                lot = ex.batchNo,
                                expiry = isoExpiry,
                                description = line.Description ?: ""
                            )
                            line to input
                        } else null
                    }

                    _state.value = _state.value.copy(
                        allPoLines = items,
                        lines = matchedPairs.map { it.first },
                        lineInputs = matchedPairs.map { it.second },
                        linesLoaded = true,
                        step = GrnStep.SHOW_PO
                    )
                } else {
                    // Manual/default: show all lines
                    val inputs = items.map { line ->
                        val safeItem = line.Item?.trim().orEmpty()
                        LineInput(
                            lineNumber = line.LineNumber,
                            itemNumber = safeItem,
                            uom = line.UOM,
                            maxQty = line.Quantity,
                            qty = 1.0,
                            lot = "BATCH20250811",
                            expiry = "2026-08-15",
                            description = line.Description ?: ""
                        )
                    }
                    _state.value = _state.value.copy(
                        allPoLines = items,
                        lines = items,
                        lineInputs = inputs,
                        linesLoaded = true,
                        step = GrnStep.SHOW_PO
                    )
                }
            }
            .onFailure { e ->
                Log.e("GRN", "fetchPoLines failed: ${e.message}")
                _state.value = _state.value.copy(error = e.message ?: "Failed to load PO lines", linesLoaded = true)
            }
    }

    fun updateLine(lineNumber: Int, qty: Double? = null, lot: String? = null, expiry: String? = null) {
        val updated = _state.value.lineInputs.map {
            if (it.lineNumber == lineNumber) it.copy(
                qty = qty ?: it.qty,
                lot = lot ?: it.lot,
                expiry = expiry ?: it.expiry
            ) else it
        }
        _state.value = _state.value.copy(lineInputs = updated)
    }

    fun removeLine(lineNumber: Int) {
        _state.value = _state.value.copy(
            lines = _state.value.lines.filterNot { it.LineNumber == lineNumber },
            lineInputs = _state.value.lineInputs.filterNot { it.lineNumber == lineNumber }
        )
    }

    fun buildPayloadAndReview() {
        val s = _state.value
        val po = s.po ?: return
        val selectedInputs = s.lineInputs
            .filter { it.qty > 0.0 && it.lot.isNotBlank() && it.expiry.isNotBlank() }
            .associateBy { it.lineNumber }

        val linesForPayload = s.lines.filter { selectedInputs.containsKey(it.LineNumber) }
        val lines = linesForPayload.map { line ->
            val inp = selectedInputs.getValue(line.LineNumber)
            ReceiptLine(
                OrganizationCode = BuildConfig.ORGANIZATION_CODE,
                DocumentNumber = po.OrderNumber,
                DocumentLineNumber = inp.lineNumber,
                ItemNumber = inp.itemNumber,
                Quantity = inp.qty,
                UnitOfMeasure = inp.uom,
                SoldtoLegalEntity = po.SoldToLegalEntity,
                Subinventory = BuildConfig.DEFAULT_SUBINVENTORY,
                Locator = BuildConfig.DEFAULT_LOCATOR,
                lotItemLots = listOf(
                    LotItem(
                        LotNumber = inp.lot,
                        TransactionQuantity = inp.qty,
                        LotExpirationDate = inp.expiry
                    )
                )
            )
        }

        val payload = ReceiptRequest(
            OrganizationCode = BuildConfig.ORGANIZATION_CODE,
            VendorName = po.Supplier,
            VendorSiteCode = po.SupplierSite,
            BusinessUnit = po.ProcurementBU,
            EmployeeId = BuildConfig.EMPLOYEE_ID,
            lines = lines
        )
        Log.d("GRN", "Payload built: lines=${lines.size}")
        _state.value = s.copy(payload = payload, step = GrnStep.REVIEW)
    }

    fun backToReceive() { _state.value = _state.value.copy(step = GrnStep.SHOW_PO) }

    fun submitReceipt() = viewModelScope.launch {
        val s = _state.value
        val payload = s.payload ?: return@launch
        _state.value = s.copy(loading = true, error = null)
        Log.d("GRN", "Submitting receipt… lines=${payload.lines.size}")

        repo.createReceipt(payload)
            .onSuccess { resp ->
                val headerId = resp.HeaderInterfaceId.orEmpty()
                val iface = resp.lines?.firstOrNull()?.InterfaceTransactionId.orEmpty()
                Log.d("GRN", "Receipt created. HeaderInterfaceId=$headerId, ifaceId=$iface")

                var errors: List<ProcessingError> = emptyList()
                if (headerId.isNotBlank() && iface.isNotBlank()) {
                    repo.fetchProcessingErrors(headerId, iface).onSuccess { errors = it }
                    Log.d("GRN", "Fetched processing errors: ${errors.size}")
                }

                // === Upload scanned images as attachments from cache paths ===
                var uploadedCount = 0
                val paths = _state.value.scanImageCachePaths
                Log.d("GRN", "Attachments: header=$headerId, files=${paths.size}")

                if (headerId.isNotBlank() && paths.isNotEmpty()) {
                    for ((i, p) in paths.withIndex()) {
                        try {
                            val file = java.io.File(p)
                            if (!file.exists()) {
                                Log.w("GRN", "Cache file not found: $p")
                                continue
                            }
                            val bytes = file.readBytes()
                            val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                            val fileName = "delivery_slip_${i + 1}.jpg"

                            val req = AttachmentRequest(
                                UploadedFileName = fileName,
                                CategoryName = "MISC",
                                FileContents = b64,
                                Title = fileName
                            )

                            repo.uploadAttachment(headerId, req)
                                .onSuccess {
                                    uploadedCount++
                                    val deleted = file.delete()
                                    Log.d("GRN", "Uploaded $fileName (${bytes.size} bytes), deleted=$deleted")
                                }
                                .onFailure { e ->
                                    Log.e("GRN", "Attachment upload failed for $fileName: ${e.message}")
                                }
                        } catch (e: Exception) {
                            Log.e("GRN", "Error processing cache file $p", e)
                        }
                    }
                }

                _state.value = _state.value.copy(
                    loading = false,
                    receipt = resp,
                    lineErrors = errors,
                    attachmentsUploaded = uploadedCount,
                    step = GrnStep.SUMMARY
                )
            }
            .onFailure { e ->
                Log.e("GRN", "createReceipt failed: ${e.message}")
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Failed to create receipt")
            }
    }

    fun startOver() { _state.value = GrnUiState() }

    // NOTE: third param now represents CACHE PATHS, not base64
    fun prefillFromScan(po: String?, scanJson: String?, cachePaths: List<String>) {
        Log.d("GRN", "prefillFromScan(po=$po, jsonLen=${scanJson?.length ?: 0}, cachePaths=${cachePaths.size})")

        var next = _state.value
        if (!po.isNullOrBlank()) next = next.copy(poNumber = po)

        if (!scanJson.isNullOrBlank()) {
            try {
                val transfer = com.google.gson.Gson().fromJson(
                    scanJson,
                    com.lazymohan.zebraprinter.scan.ScanExtractTransfer::class.java
                )
                val normalized = transfer.items.map {
                    com.lazymohan.zebraprinter.grn.util.ExtractedItem(
                        description = it.description,
                        qtyDelivered = it.qty,
                        expiryDate = it.expiry,
                        batchNo = it.batchNo
                    )
                }
                val poNum = if (!po.isNullOrBlank()) po!! else transfer.poNumber
                next = next.copy(
                    poNumber = poNum,
                    extractedFromScan = normalized,
                    scanImageCachePaths = cachePaths
                )
            } catch (e: Exception) {
                Log.e("GRN", "Failed to read scan payload: ${e.message}")
                next = next.copy(error = "Failed to read scan payload: ${e.message}", scanImageCachePaths = cachePaths)
            }
        } else {
            // no scan json, still store images if any
            if (cachePaths.isNotEmpty()) next = next.copy(scanImageCachePaths = cachePaths)
        }

        _state.value = next

        // Auto-fetch if we have a PO number
        if (_state.value.poNumber.isNotBlank() && _state.value.step == GrnStep.ENTER_PO) {
            fetchPo()
        }
    }

    fun addLineFromPo(lineNumber: Int) {
        val s = _state.value
        val already = s.lineInputs.any { it.lineNumber == lineNumber }
        if (already) return

        val poLine = s.allPoLines.firstOrNull { it.LineNumber == lineNumber } ?: return
        val safeItem = poLine.Item?.trim().orEmpty()

        val newInput = LineInput(
            lineNumber = poLine.LineNumber,
            itemNumber = safeItem,
            uom = poLine.UOM,
            maxQty = poLine.Quantity,
            qty = 1.0,
            lot = "",
            expiry = "",
            description = poLine.Description ?: ""
        )

        _state.value = s.copy(
            lines = s.lines + poLine,
            lineInputs = s.lineInputs + newInput
        )
    }
}
