// app/src/main/java/com/lazymohan/zebraprinter/grn/ui/to/ToViewModel.kt
package com.lazymohan.zebraprinter.grn.ui.to

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.lazymohan.zebraprinter.grn.data.GrnRepository
import com.lazymohan.zebraprinter.grn.data.LotEntryTo
import com.lazymohan.zebraprinter.grn.data.ReceiptLineTo
import com.lazymohan.zebraprinter.grn.data.ReceiptRequestTo
import com.lazymohan.zebraprinter.grn.data.ReceiptResponse
import com.lazymohan.zebraprinter.grn.data.ShipmentLine
import com.lazymohan.zebraprinter.grn.data.ToConfig
import com.lazymohan.zebraprinter.grn.data.ToReceiveLineInput
import com.lazymohan.zebraprinter.grn.data.TransferOrderHeader
import com.lazymohan.zebraprinter.grn.data.TransferOrderLine
import com.lazymohan.zebraprinter.grn.domain.usecase.CreateToReceiptUseCase
import com.lazymohan.zebraprinter.grn.domain.usecase.FetchToBundleUseCase
import com.lazymohan.zebraprinter.grn.util.ExtractedItem
import com.lazymohan.zebraprinter.scan.ScanExtractTransfer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/* ---------- UI state & models ---------- */

data class ToLineSection(
    val section: Int,
    val qty: Int,
    val lot: String,
    val expiry: String? = null
)

data class ToLineInput(
    val lineId: Long,
    val sections: List<ToLineSection> = listOf(ToLineSection(section = 1, qty = 0, lot = ""))
)

enum class ToStep { ENTER, RECEIVE, REVIEW, SUMMARY }
enum class PartStatus { PENDING, SUBMITTING, SUCCESS, FAILED }

data class ToProgressPart(
    val sectionIndex: Int = 1,
    val status: PartStatus = PartStatus.PENDING,
    val returnStatus: String? = null,
    val message: String? = null,
    val httpCode: Int? = null,
    val url: String? = null,
    val errorBody: String? = null,
    val errors: List<String> = emptyList(),
    val exception: String? = null
)

data class ToUiState(
    val toNumber: String = "",
    val loading: Boolean = false,
    val error: String? = null,

    val header: TransferOrderHeader? = null,
    val lines: List<TransferOrderLine> = emptyList(),
    val linesLoaded: Boolean = false,

    val lineInputs: List<ToLineInput> = emptyList(),
    val shipment: ShipmentLine? = null,

    val submitting: Boolean = false,
    val submitError: String? = null,
    val reviewError: String? = null,

    val receipt: ReceiptResponse? = null,
    val progress: List<ToProgressPart> = emptyList(),

    // scan payload (unchanged)
    val extractedFromScan: List<ExtractedItem> = emptyList(),

    // cache file paths for scanned images (from ScanResultScreen)
    val scanImageCachePaths: List<String> = emptyList(),

    val step: ToStep = ToStep.ENTER
)

@HiltViewModel
class ToViewModel @Inject constructor(
    private val fetchToBundle: FetchToBundleUseCase,
    private val createReceipt: CreateToReceiptUseCase,
    private val repo: GrnRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(ToUiState())
    val ui: StateFlow<ToUiState> = _ui

    private val cfg = ToConfig(
        fromOrganizationCode = "KDH",
        organizationCode = "KDJ",
        employeeId = 300000068190418
    )

    fun onEnterToNumber(value: String) {
        _ui.value = _ui.value.copy(toNumber = value.trim(), error = null)
    }

    fun fetchTo() {
        val number = _ui.value.toNumber
        if (number.isBlank()) {
            _ui.value = _ui.value.copy(error = "Enter a Transfer Order number")
            return
        }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null, linesLoaded = false)
            val res = fetchToBundle(number)
            _ui.value = res.fold(
                onSuccess = { b ->
                    viewModelScope.launch {
                        val enrichedInputs = buildInputsWithShipments(
                            toNumber = number,
                            headerId = b.header.headerId,
                            lines = b.lines
                        )
                        _ui.value = _ui.value.copy(
                            loading = false,
                            header = b.header,
                            lines = b.lines,
                            shipment = b.shipment,
                            linesLoaded = true,
                            lineInputs = enrichedInputs,
                            step = ToStep.RECEIVE
                        )
                    }
                    _ui.value
                },
                onFailure = { e ->
                    _ui.value.copy(
                        loading = false,
                        error = e.message ?: "TO not found or API error"
                    )
                }
            )
        }
    }

    /** Build ToLineInput list creating multiple sections when multiple shipment lots are present */
    private suspend fun buildInputsWithShipments(
        toNumber: String,
        headerId: Long,
        lines: List<TransferOrderLine>
    ): List<ToLineInput> = coroutineScope {
        lines.map { line ->
            async {
                val shipments = repo.getShipmentLinesForOrderAndItem(toNumber, line.itemNumber)
                    .getOrElse { emptyList() }
                    .filter { !it.lotNumber.isNullOrBlank() }

                val baseSections = if (shipments.isEmpty()) {
                    listOf(ToLineSection(1, 0, lot = ""))
                } else {
                    shipments.mapIndexed { idx, s ->
                        val defQty = (s.shippedQuantity ?: 0.0).toInt()
                        ToLineSection(
                            section = idx + 1,
                            qty = defQty,
                            lot = s.lotNumber.orEmpty(),
                            expiry = null
                        )
                    }
                }

                val withExpiry = baseSections.map { sec ->
                    if (sec.lot.isBlank()) sec
                    else {
                        val exp = repo.getLotExpiry(sec.lot).getOrNull()
                        sec.copy(expiry = exp)
                    }
                }

                ToLineInput(
                    lineId = stableLineId(headerId, line),
                    sections = withExpiry
                )
            }
        }.awaitAll()
    }

    /* ----- id helpers ----- */

    private fun stableLineId(headerId: Long?, line: TransferOrderLine): Long {
        val raw = line.transferOrderLineId
        if (raw != 0L) return raw
        val hid = headerId ?: 0L
        val ln = (line.lineNumber ?: 0)
        return (hid shl 20) + ln.toLong()
    }

    private fun findLineById(id: Long): TransferOrderLine? {
        val hid = _ui.value.header?.headerId
        return _ui.value.lines.firstOrNull { l ->
            l.transferOrderLineId == id || stableLineId(hid, l) == id
        }
    }

    /* ----- line selection ----- */

    fun addLine(lineId: Long) {
        val inputs = _ui.value.lineInputs
        if (inputs.any { it.lineId == lineId }) return
        _ui.value = _ui.value.copy(lineInputs = inputs + ToLineInput(lineId))
    }

    fun removeLine(lineId: Long) {
        _ui.value = _ui.value.copy(
            lineInputs = _ui.value.lineInputs.filterNot { it.lineId == lineId }
        )
    }

    fun addSection(lineId: Long) {
        val li = _ui.value.lineInputs.find { it.lineId == lineId } ?: return
        val next = (li.sections.maxOfOrNull { it.section } ?: 0) + 1
        val upd = li.copy(sections = li.sections + ToLineSection(next, 0, "", null))
        replaceInput(upd)
    }

    fun removeSection(lineId: Long, section: Int) {
        val li = _ui.value.lineInputs.find { it.lineId == lineId } ?: return
        val upd = li.copy(
            sections = li.sections
                .filterNot { it.section == section }
                .ifEmpty { listOf(ToLineSection(1, 0, "", null)) }
        )
        replaceInput(upd)
    }

    fun updateSectionQty(lineId: Long, section: Int, qty: Int) {
        val li = _ui.value.lineInputs.find { it.lineId == lineId } ?: return
        val upd = li.copy(sections = li.sections.map {
            if (it.section == section) it.copy(qty = qty.coerceAtLeast(0)) else it
        })
        replaceInput(upd)
    }

    fun updateSectionLot(lineId: Long, section: Int, lot: String) {
        val li = _ui.value.lineInputs.find { it.lineId == lineId } ?: return
        replaceInput(
            li.copy(
                sections = li.sections.map {
                    if (it.section == section) it.copy(lot = lot, expiry = null) else it
                }
            )
        )
        if (lot.isNotBlank()) {
            viewModelScope.launch {
                val expiry = repo.getLotExpiry(lot).getOrNull()
                val li2 = _ui.value.lineInputs.find { it.lineId == lineId } ?: return@launch
                replaceInput(
                    li2.copy(
                        sections = li2.sections.map {
                            if (it.section == section) it.copy(expiry = expiry) else it
                        }
                    )
                )
            }
        }
    }

    private fun replaceInput(newLi: ToLineInput) {
        _ui.value = _ui.value.copy(
            lineInputs = _ui.value.lineInputs.map { if (it.lineId == newLi.lineId) newLi else it }
        )
    }

    /* ----- review & submit ----- */

    fun canReview(): Boolean {
        val header = _ui.value.header ?: return false
        val hasAnyQty = _ui.value.lineInputs.any { it.sections.any { s -> s.qty > 0 } }
        return hasAnyQty && header.headerId > 0
    }

    /** Reason string used by ToGrnActivity when review is blocked */
    fun reviewBlockReason(): String {
        val s = _ui.value
        val reasons = buildString {
            if (s.header == null) appendLine("• Missing Transfer Order details.")
            val hasAnyQty = s.lineInputs.any { it.sections.any { sec -> sec.qty > 0 } }
            if (!hasAnyQty) appendLine("• Enter at least one Quantity.")
        }.trim()
        return if (reasons.isBlank()) "Nothing to review." else reasons
    }

    fun goToReview() {
        if (!canReview()) {
            _ui.value = _ui.value.copy(reviewError = reviewBlockReason())
            return
        }
        _ui.value = _ui.value.copy(reviewError = null, step = ToStep.REVIEW)
    }

    fun backToReceive() {
        _ui.value = _ui.value.copy(step = ToStep.RECEIVE)
    }

    fun submitReceipt() {
        val header = _ui.value.header ?: return
        val toNumber = _ui.value.toNumber

        val toInputs: List<ToReceiveLineInput> =
            _ui.value.lineInputs.flatMap { li ->
                val line = findLineById(li.lineId) ?: return@flatMap emptyList()
                li.sections
                    .filter { it.qty > 0 }
                    .map { s ->
                        ToReceiveLineInput(
                            line = line,
                            quantity = s.qty,
                            lotNumber = s.lot.ifBlank { null },
                            lotExpirationDate = s.expiry
                        )
                    }
            }

        viewModelScope.launch {
            _ui.value = _ui.value.copy(
                submitting = true,
                submitError = null,
                progress = listOf(ToProgressPart(status = PartStatus.SUBMITTING))
            )

            val request = buildToReceiptRequest(
                cfg = cfg,
                header = header,
                toNumber = toNumber,
                inputs = toInputs
            )



            if (request.shipmentNumber == null) {
                _ui.value = _ui.value.copy(
                    submitting = false,
                    submitError = "No Shipment Number found for this TO. Please ensure shipments exist.",
                    progress = listOf(ToProgressPart(status = PartStatus.FAILED)),
                    step = ToStep.SUMMARY
                )
                return@launch
            }

            val res = createReceipt(request)

            _ui.value = res.fold(
                onSuccess = { r ->
                    val isSuccess = r.ReturnStatus.equals("SUCCESS", true)
                    val headerIface = r.HeaderInterfaceId
                    val ifaceTxn = r.lines?.firstOrNull()?.InterfaceTransactionId
                    val inlineErrors = mutableListOf<String>()

                    if (!isSuccess) inlineErrors += "ReturnStatus: ${r.ReturnStatus}"

                    if (!headerIface.isNullOrBlank() && !ifaceTxn.isNullOrBlank()) {
                        repo.fetchProcessingErrors(headerIface, ifaceTxn).onSuccess { fetched ->
                            inlineErrors += fetched.mapNotNull { it.ErrorMessage?.trim() }
                                .filter { it.isNotBlank() }
                        }.onFailure { pe ->
                            inlineErrors += "ProcessingErrors fetch failed: ${pe.message}"
                        }
                    } else {
                        r.Message?.let { inlineErrors += it }
                        r.ReturnMessage?.let { inlineErrors += it }
                    }

                    val part = ToProgressPart(
                        status = if (isSuccess) PartStatus.SUCCESS else PartStatus.FAILED,
                        returnStatus = r.ReturnStatus,
                        message = r.Message ?: r.ReturnMessage ?: r.toString(),
                        errors = inlineErrors
                    )
                    _ui.value.copy(
                        submitting = false,
                        receipt = r,
                        progress = listOf(part),
                        step = ToStep.SUMMARY
                    )
                },
                onFailure = { e ->
                    val part = ToProgressPart(
                        status = PartStatus.FAILED,
                        message = e.message,
                        exception = e.toString()
                    )
                    _ui.value.copy(
                        submitting = false,
                        submitError = e.message ?: "Submit failed",
                        progress = listOf(part),
                        step = ToStep.SUMMARY
                    )
                }
            )
        }
    }

    fun restart() {
        _ui.value = ToUiState()
    }

    /* ---------- request builder with shipment mapping ---------- */

    private suspend fun buildToReceiptRequest(
        cfg: ToConfig,
        header: TransferOrderHeader,
        toNumber: String,
        inputs: List<ToReceiveLineInput>
    ): ReceiptRequestTo {
        val shipmentsByItem: Map<String, List<ShipmentLine>> =
            inputs.map { it.line.itemNumber }.distinct().associateWith { item ->
                repo.getShipmentLinesForOrderAndItem(toNumber, item).getOrElse { emptyList() }
            }

        val headerShipmentNumber: Long? =
            shipmentsByItem.values
                .asSequence()
                .flatten()
                .mapNotNull { it.shipmentNumber }
                .firstOrNull()

        val lines = inputs.mapIndexed { idx, it ->
            val itemShipments = shipmentsByItem[it.line.itemNumber].orEmpty()

            val docShipment: Long? = when {
                !it.lotNumber.isNullOrBlank() ->
                    itemShipments.firstOrNull { s ->
                        s.lotNumber?.equals(it.lotNumber, ignoreCase = true) == true
                    }?.shipmentNumber
                else ->
                    itemShipments.firstOrNull { s -> s.shipmentNumber != null }?.shipmentNumber
            } ?: headerShipmentNumber

            ReceiptLineTo(
                sourceDocumentCode = "TRANSFER ORDER",
                receiptSourceCode = "TRANSFER ORDER",
                transactionType = "RECEIVE",
                autoTransactCode = "DELIVER",
                documentNumber = docShipment,                    // per-line shipment number
                documentLineNumber = idx + 1,
                itemNumber = it.line.itemNumber,
                organizationCode = cfg.organizationCode,
                quantity = it.quantity,
                unitOfMeasure = it.line.unitOfMeasure ?: "EA",
                subinventory = it.line.subinventory,             // destination when present
                transferOrderHeaderId = header.headerId,
                transferOrderLineId = it.line.transferOrderLineId,
                lotItemLots = it.lotNumber?.let { lot ->
                    listOf(
                        LotEntryTo(
                            lotNumber = lot,
                            transactionQuantity = it.quantity,
                            lotExpirationDate = it.lotExpirationDate
                        )
                    )
                }
            )
        }

        return ReceiptRequestTo(
            fromOrganizationCode = cfg.fromOrganizationCode,
            organizationCode = cfg.organizationCode,
            employeeId = cfg.employeeId,
            receiptSourceCode = "TRANSFER ORDER",
            shipmentNumber = headerShipmentNumber,
            lines = lines
        )
    }

    fun prefillFromScan(to: String?, scanJson: String?, cachePaths: List<String>) {
        Log.d(
            "GRN",
            "prefillFromScan(po=$to, jsonLen=${scanJson?.length ?: 0}, cachePaths=${cachePaths.size})"
        )

        var next = _ui.value
        if (!to.isNullOrBlank()) next = next.copy(toNumber = to)

        if (!scanJson.isNullOrBlank()) {
            try {
                val transfer = Gson().fromJson(
                    scanJson,
                    ScanExtractTransfer::class.java
                )
                val normalized = transfer.items.map {
                    ExtractedItem(
                        description = it.description,
                        qtyDelivered = it.qty,
                        expiryDate = it.expiry,
                        batchNo = it.batchNo
                    )
                }
                val poNum = if (!to.isNullOrBlank()) to else transfer.poNumber
                next = next.copy(
                    toNumber = poNum,
                    extractedFromScan = normalized,
                    scanImageCachePaths = cachePaths
                )
            } catch (e: Exception) {
                Log.e("GRN", "Failed to read scan payload: ${e.message}")
                next = next.copy(
                    error = "Failed to read scan payload: ${e.message}",
                    scanImageCachePaths = cachePaths
                )
            }
        } else {
            if (cachePaths.isNotEmpty()) next = next.copy(scanImageCachePaths = cachePaths)
        }

        _ui.value = next
        if (_ui.value.toNumber.isNotBlank() && _ui.value.step == ToStep.ENTER) {
            fetchTo()
        }
    }
}
