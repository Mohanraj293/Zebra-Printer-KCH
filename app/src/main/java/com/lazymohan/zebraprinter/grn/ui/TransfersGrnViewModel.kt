package com.lazymohan.zebraprinter.grn.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazymohan.zebraprinter.BuildConfig
import com.lazymohan.zebraprinter.app.AppPref
import com.lazymohan.zebraprinter.grn.data.AttachmentRequest
import com.lazymohan.zebraprinter.grn.data.GrnRepository
import com.lazymohan.zebraprinter.grn.data.ReceiptResponse
import com.lazymohan.zebraprinter.grn.data.TOLotItem
import com.lazymohan.zebraprinter.grn.data.TransferOrderItem
import com.lazymohan.zebraprinter.grn.data.TransferOrderLineItem
import com.lazymohan.zebraprinter.grn.data.TransferReceiptLine
import com.lazymohan.zebraprinter.grn.data.TransferReceiptRequest
import com.lazymohan.zebraprinter.grn.util.ExtractedItem
import com.lazymohan.zebraprinter.grn.util.bestMatchIndex
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class ToUiState(
    val step: GrnStep = GrnStep.ENTER_PO,
    val loading: Boolean = false,
    val error: String? = null,

    val toNumber: String = "",
    val header: TransferOrderItem? = null,
    val lines: List<TransferOrderLineItem> = emptyList(),
    val lineInputs: List<LineInput> = emptyList(),    // reuse existing LineInput
    val linesLoaded: Boolean = false,

    // progress (reuse PartProgress/PartStatus from GrnViewModel.kt)
    val progress: List<PartProgress> = emptyList(),

    val receipt: ReceiptResponse? = null,
    val attachmentsUploaded: Int = 0,

    // OCR payload + image cache
    val extractedFromScan: List<ExtractedItem> = emptyList(),
    val scanImageCachePaths: List<String> = emptyList()
)

// Internal staged holder for TO requests
private data class ToStaged(val sectionIndex: Int, val request: TransferReceiptRequest)

@HiltViewModel
class TransferGrnViewModel @Inject constructor(
    private val repo: GrnRepository,
    private val appPref: AppPref
) : ViewModel() {

    private val _state = MutableStateFlow(ToUiState())
    val state = _state.asStateFlow()

    // keep TO staged list internally
    private var _toStagedInternal: List<ToStaged> = emptyList()

    fun setToNumber(to: String) {
        _state.value = _state.value.copy(toNumber = to)
    }

    fun fetchTransferOrder() = viewModelScope.launch {
        val s = _state.value
        if (s.toNumber.isBlank()) return@launch
        _state.value = s.copy(loading = true, error = null)

        repo.fetchTransferOrder(s.toNumber)
            .onSuccess { header ->
                Log.d("GRN-TO", "TO fetched: ${header.HeaderNumber}, id=${header.HeaderId}")
                _state.value = _state.value.copy(loading = false, header = header, step = GrnStep.SHOW_PO)
                prefetchToLines(header.HeaderId)
            }
            .onFailure { e ->
                Log.e("GRN-TO", "fetchTransferOrder failed: ${e.message}")
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Failed to load Transfer Order")
            }
    }

    private fun TransferOrderLineItem.toLineInput(initial: LineSectionInput? = null) = LineInput(
        lineNumber = TransferOrderLineId.toInt(), // unique key for UI
        itemNumber = ItemNumber.trim(),
        uom = UnitOfMeasure ?: "EA",
        maxQty = Int.MAX_VALUE, // adjust if you fetch remaining qty
        description = ItemNumber,
        gtin = null,
        sections = listOf(initial ?: LineSectionInput(section = 1))
    )

    private fun prefetchToLines(headerId: Long) = viewModelScope.launch {
        Log.d("GRN-TO", "Prefetch TO lines for headerId=$headerId")
        repo.fetchTransferOrderLines(headerId)
            .onSuccess { items ->
                val extracted = _state.value.extractedFromScan
                Log.d("GRN-TO", "Fetched ${items.size} TO lines. Scan items=${extracted.size}")

                if (extracted.isNotEmpty()) {
                    val used = mutableSetOf<Int>()
                    val matchedInputs = items.mapNotNull { line ->
                        val matchIdx = bestMatchIndex(
                            extracted = extracted,
                            targetDesc = line.ItemDescription.trim(),
                            used = used,
                            threshold = 0.01
                        )
                        if (matchIdx != null) {
                            used += matchIdx
                            val ex = extracted[matchIdx]
                            val sec1 = LineSectionInput(
                                section = 1,
                                qty = ex.qtyDelivered,
                                lot = ex.batchNo,
                                expiry = ex.expiryDate // not used in TO payload by default
                            )
                            line.toLineInput(initial = sec1)
                        } else null
                    }

                    _state.value = _state.value.copy(
                        lines = items,
                        lineInputs = matchedInputs,
                        linesLoaded = true,
                        step = GrnStep.SHOW_PO
                    )
                } else {
                    _state.value = _state.value.copy(
                        lines = items,
                        lineInputs = items.map { it.toLineInput() },
                        linesLoaded = true,
                        step = GrnStep.SHOW_PO
                    )
                }
            }
            .onFailure { e ->
                Log.e("GRN-TO", "fetchTransferOrderLines failed: ${e.message}")
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to load TO lines",
                    linesLoaded = true
                )
            }
    }

    // --- Section helpers ---
    fun addSection(lineNumber: Int) {
        val s = _state.value
        val updated = s.lineInputs.map { li ->
            if (li.lineNumber == lineNumber) {
                val nextIdx = (li.sections.maxOfOrNull { it.section } ?: 0) + 1
                li.copy(sections = li.sections + LineSectionInput(section = nextIdx))
            } else li
        }
        _state.value = s.copy(lineInputs = updated)
    }

    fun removeSection(lineNumber: Int, sectionIndex: Int) {
        val s = _state.value
        val updated = s.lineInputs.map { li ->
            if (li.lineNumber == lineNumber) {
                val pruned = li.sections
                    .filterNot { it.section == sectionIndex }
                    .mapIndexed { idx, sec -> sec.copy(section = idx + 1) }
                li.copy(sections = if (pruned.isEmpty()) listOf(LineSectionInput(1)) else pruned)
            } else li
        }
        _state.value = s.copy(lineInputs = updated)
    }

    fun updateLineSection(
        lineNumber: Int,
        sectionIndex: Int = 1,
        qty: Double? = null,
        lot: String? = null,
        expiry: String? = null
    ) {
        val s = _state.value
        val updated = s.lineInputs.map { li ->
            if (li.lineNumber != lineNumber) return@map li
            val secs = li.sections.map { sec ->
                if (sec.section != sectionIndex) sec else sec.copy(
                    qty = qty ?: sec.qty,
                    lot = lot ?: sec.lot,
                    expiry = expiry ?: sec.expiry
                )
            }
            li.copy(sections = secs)
        }
        _state.value = s.copy(lineInputs = updated)
    }

    fun removeLine(lineNumber: Int) {
        _state.value = _state.value.copy(
            lines = _state.value.lines.filterNot { it.TransferOrderLineId.toInt() == lineNumber },
            lineInputs = _state.value.lineInputs.filterNot { it.lineNumber == lineNumber }
        )
    }

    fun addLineFromTo(lineId: Int) {
        val s = _state.value
        if (s.lineInputs.any { it.lineNumber == lineId.toInt() }) return
        val toLine = s.lines.firstOrNull { it.TransferOrderLineId == lineId } ?: return
        val newInput = toLine.toLineInput()
        _state.value = s.copy(
            lines = s.lines + toLine,
            lineInputs = s.lineInputs + newInput
        )
    }

    private fun LineSectionInput.isValidForTO(): Boolean =
        qty > 0.0 && lot.isNotBlank()

    fun buildTOPayloadsAndReview() = viewModelScope.launch {
        val s = _state.value
        val header = s.header ?: return@launch

        // 1) Find a shipment for this TO
        val shipments = repo.fetchShipmentForOrder(s.toNumber).getOrElse {
            _state.value = s.copy(error = "Shipment lookup failed: ${it.message}")
            return@launch
        }
        val shipmentNumber = shipments.firstOrNull()?.ShipmentNumber?.trim()
        if (shipmentNumber.isNullOrBlank()) {
            _state.value = s.copy(error = "No shipment found for TO ${s.toNumber}")
            return@launch
        }

        // 2) Stage by section
        val maxSection = s.lineInputs.maxOfOrNull { it.sections.size } ?: 0
        if (maxSection == 0) return@launch

        val staged = mutableListOf<ToStaged>()
        val progress = mutableListOf<PartProgress>()

        for (secIdx in 1..maxSection) {
            val linesForThisSection = s.lines.mapNotNull { line ->
                val li = s.lineInputs.firstOrNull { it.lineNumber == line.TransferOrderLineId.toInt() }
                    ?: return@mapNotNull null
                val sec = li.sections.firstOrNull { it.section == secIdx } ?: return@mapNotNull null
                if (!sec.isValidForTO()) return@mapNotNull null

                val docLineNo = line.LineNumber ?: 1
                val lotForThisLine = sec.lot

                TransferReceiptLine(
                    DocumentNumber = shipmentNumber,
                    DocumentLineNumber = docLineNo,
                    ItemNumber = line.ItemNumber,
                    OrganizationCode = "", // required
                    Quantity = sec.qty,
                    UnitOfMeasure = line.UnitOfMeasure ?: "EA",
                    Subinventory = line.Subinventory,
                    TransferOrderHeaderId = header.HeaderId,
                    TransferOrderLineId = line.TransferOrderLineId,
                    lotItemLots = listOf(
                        TOLotItem(
                            LotNumber = lotForThisLine,
                            TransactionQuantity = sec.qty
                        )
                    )
                )
            }

            if (linesForThisSection.isNotEmpty()) {
                staged += ToStaged(
                    sectionIndex = secIdx,
                    request = TransferReceiptRequest(
                        FromOrganizationCode = BuildConfig.ORGANIZATION_CODE,
                        OrganizationCode = "",
                        EmployeeId = appPref.personId.toString(),
                        ShipmentNumber = shipmentNumber,
                        ReceiptHeaderId = null,
                        lines = linesForThisSection
                    )
                )
                progress += PartProgress(sectionIndex = secIdx, lines = linesForThisSection.size)
            }
        }

        _toStagedInternal = staged
        _state.value = s.copy(progress = progress, step = GrnStep.REVIEW)
    }

    fun backToReceive() { _state.value = _state.value.copy(step = GrnStep.SHOW_PO) }

    fun submitTransferReceipt() = viewModelScope.launch {
        val s = _state.value
        val staged = _toStagedInternal
        if (staged.isEmpty()) return@launch

        _state.value = s.copy(loading = true, error = null)
        var receiptHeaderId: Long? = null
        var lastResp: ReceiptResponse? = null
        var progress = _state.value.progress.toMutableList()
        var headerInterfaceId: String? = null

        staged.forEachIndexed { idx, part ->
            progress = progress.map {
                if (it.sectionIndex == part.sectionIndex) it.copy(status = PartStatus.SUBMITTING) else it
            }.toMutableList()
            _state.value = _state.value.copy(progress = progress)

            val body = if (idx == 0) {
                part.request
            } else {
                part.request.copy(ReceiptHeaderId = receiptHeaderId)
            }
            Log.d("GRN-TO", "Submitting TO part ${part.sectionIndex} with ${body.lines.size} lines; header=$receiptHeaderId")

            val result = repo.createTransferReceipt(body)
            result.onSuccess { resp ->
                lastResp = resp
                receiptHeaderId = receiptHeaderId ?: resp.ReceiptHeaderId

                val isSuccess = resp.ReturnStatus.equals("SUCCESS", true)
                if (headerInterfaceId.isNullOrBlank() && !resp.HeaderInterfaceId.isNullOrBlank()) {
                    headerInterfaceId = resp.HeaderInterfaceId
                }

                progress = progress.map {
                    if (it.sectionIndex == part.sectionIndex)
                        it.copy(
                            status = if (isSuccess) PartStatus.SUCCESS else PartStatus.FAILED,
                            message = if (isSuccess) "Part ${part.sectionIndex} completed" else "Part ${part.sectionIndex} failed",
                            receiptHeaderId = receiptHeaderId,
                            returnStatus = resp.ReturnStatus
                        )
                    else it
                }.toMutableList()
                _state.value = _state.value.copy(progress = progress)
            }.onFailure { e ->
                val httpCode: Int?
                val url: String?
                val bodyText: String?

                if (e is HttpException) {
                    httpCode = e.code()
                    url = e.response()?.raw()?.request?.url?.toString()
                    bodyText = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                } else {
                    httpCode = null
                    url = null
                    bodyText = null
                }

                val failMsg = buildString {
                    appendLine("TO Receipt API failed${httpCode?.let { " ($it)" } ?: ""}")
                    if (!url.isNullOrBlank()) appendLine("URL: $url")
                    if (!bodyText.isNullOrBlank()) appendLine(bodyText)
                    e.message?.let { appendLine("Exception: $it") }
                }.trim()

                progress = progress.map {
                    if (it.sectionIndex == part.sectionIndex)
                        it.copy(
                            status = PartStatus.FAILED,
                            message = "Part ${part.sectionIndex} failed",
                            returnStatus = "ERROR",
                            errors = listOf(failMsg),
                            httpCode = httpCode,
                            url = url,
                            errorBody = bodyText,
                            exception = e::class.java.simpleName + (e.message?.let { ": $it" } ?: "")
                        )
                    else it
                }.toMutableList()
                _state.value = _state.value.copy(progress = progress)
            }
        }

        // Upload scanned images as attachments once against the HeaderInterfaceId
        val paths = _state.value.scanImageCachePaths
        var uploadedCount = 0
        val finalHeaderInterfaceId = headerInterfaceId
        if (finalHeaderInterfaceId != null && paths.isNotEmpty()) {
            for ((i, p) in paths.withIndex()) {
                try {
                    val file = java.io.File(p)
                    if (!file.exists()) continue
                    val bytes = file.readBytes()
                    val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    val fileName = "delivery_slip_${i + 1}.jpg"
                    val req = AttachmentRequest(
                        UploadedFileName = fileName,
                        CategoryName = "MISC",
                        FileContents = b64,
                        Title = fileName
                    )
                    repo.uploadAttachment(finalHeaderInterfaceId, req).onSuccess {
                        uploadedCount++
                        file.delete()
                    }
                } catch (e: Exception) {
                    Log.e("GRN-TO", "Attachment error for ${paths[i]}", e)
                }
            }
        }

        _state.value = _state.value.copy(
            loading = false,
            receipt = lastResp,
            attachmentsUploaded = uploadedCount,
            step = GrnStep.SUMMARY
        )
    }

    fun startOver() { _state.value = ToUiState() }

    // Same contract as PO prefill, but for TO number
    fun prefillFromScan(toNumber: String?, scanJson: String?, cachePaths: List<String>) {
        Log.d("GRN-TO", "prefillFromScan(to=$toNumber, jsonLen=${scanJson?.length ?: 0}, cachePaths=${cachePaths.size})")

        var next = _state.value
        if (!toNumber.isNullOrBlank()) next = next.copy(toNumber = toNumber)

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
                val num = if (!toNumber.isNullOrBlank()) toNumber else transfer.poNumber // reuse field if same
                next = next.copy(
                    toNumber = num,
                    extractedFromScan = normalized,
                    scanImageCachePaths = cachePaths
                )
            } catch (e: Exception) {
                Log.e("GRN-TO", "Failed to read scan payload: ${e.message}")
                next = next.copy(
                    error = "Failed to read scan payload: ${e.message}",
                    scanImageCachePaths = cachePaths
                )
            }
        } else if (cachePaths.isNotEmpty()) {
            next = next.copy(scanImageCachePaths = cachePaths)
        }

        _state.value = next
        if (_state.value.toNumber.isNotBlank() && _state.value.step == GrnStep.ENTER_PO) {
            fetchTransferOrder()
        }
    }
}