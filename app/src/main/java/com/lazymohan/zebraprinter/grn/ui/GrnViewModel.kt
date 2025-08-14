// app/src/main/java/com/lazymohan/zebraprinter/grn/ui/GrnViewModel.kt
package com.lazymohan.zebraprinter.grn.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazymohan.zebraprinter.BuildConfig
import com.lazymohan.zebraprinter.grn.data.*
import com.lazymohan.zebraprinter.grn.util.ExtractedItem
import com.lazymohan.zebraprinter.grn.util.bestMatchIndex
import com.lazymohan.zebraprinter.grn.util.parseToIso
import com.lazymohan.zebraprinter.grn.util.toDoubleSafe
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
    val lines: List<PoLineItem> = emptyList(),
    val lineInputs: List<LineInput> = emptyList(),
    val payload: ReceiptRequest? = null,
    val receipt: ReceiptResponse? = null,
    val lineErrors: List<ProcessingError> = emptyList()
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
                _state.value = _state.value.copy(loading = false, po = po, step = GrnStep.SHOW_PO)
                prefetchPoLines(po.POHeaderId)
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Failed to load PO")
            }
    }

    private fun prefetchPoLines(headerId: String) = viewModelScope.launch {
        repo.fetchPoLines(headerId)
            .onSuccess { items ->
                // Simulated extracted items for matching
                // In a real scenario, this would come from extraction API
                val extractedItems = listOf(
                    ExtractedItem(
                        description = "NUTRYELT 10ML AMP (TRACE EL.) ADULT-10'S",
                        qtyDelivered = toDoubleSafe("3"),
                        expiryDate = "09/01/2026",
                        batchNo = "D0615A04"
                    ),
                    ExtractedItem(
                        description = "TACHYBEN (URAPIDIL) 50MG/10ML-AMP-5'S",
                        qtyDelivered = toDoubleSafe("3"),
                        expiryDate = "31/01/2026",
                        batchNo = "F7201-03"
                    )
                )

                val used = mutableSetOf<Int>()
                val inputs = items.map { line ->
                    val safeItem = line.Item?.trim().orEmpty()
                    val poDesc = (line.Description ?: safeItem).trim()
                    val matchIdx = bestMatchIndex(extractedItems, poDesc, used, threshold = 0.35)

                    if (matchIdx != null) {
                        used += matchIdx
                        val ex = extractedItems[matchIdx]
                        val isoExpiry = parseToIso(ex.expiryDate)
                        val clampedQty = min(ex.qtyDelivered, line.Quantity)
                        LineInput(
                            lineNumber = line.LineNumber,
                            itemNumber = safeItem,
                            uom = line.UOM,
                            maxQty = line.Quantity,
                            qty = clampedQty,
                            lot = ex.batchNo,
                            expiry = isoExpiry,
                            description = line.Description ?: ""
                        )
                    } else {
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
                }

                _state.value = _state.value.copy(lines = items, lineInputs = inputs)
            }
            .onFailure { e -> _state.value = _state.value.copy(error = e.message ?: "Failed to load PO lines") }
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
        _state.value = s.copy(payload = payload, step = GrnStep.REVIEW)
    }

    fun backToReceive() { _state.value = _state.value.copy(step = GrnStep.SHOW_PO) }  // NEW

    fun submitReceipt() = viewModelScope.launch {
        val s = _state.value
        val payload = s.payload ?: return@launch
        _state.value = s.copy(loading = true, error = null)
        repo.createReceipt(payload)
            .onSuccess { resp ->
                val headerId = resp.HeaderInterfaceId.orEmpty()
                val iface = resp.lines?.firstOrNull()?.InterfaceTransactionId.orEmpty()
                var errors: List<ProcessingError> = emptyList()
                if (headerId.isNotBlank() && iface.isNotBlank()) {
                    repo.fetchProcessingErrors(headerId, iface).onSuccess { errors = it }
                }
                _state.value = _state.value.copy(
                    loading = false,
                    receipt = resp,
                    lineErrors = errors,
                    step = GrnStep.SUMMARY
                )
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Failed to create receipt")
            }
    }

    fun startOver() { _state.value = GrnUiState() }
}
