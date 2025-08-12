// app/src/main/java/com/lazymohan/zebraprinter/grn/ui/GrnViewModel.kt
package com.lazymohan.zebraprinter.grn.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazymohan.zebraprinter.BuildConfig
import com.lazymohan.zebraprinter.grn.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class GrnStep { ENTER_PO, SHOW_PO, RECEIVE_LINES, REVIEW, SUMMARY }

data class LineInput(
    val lineNumber: Int,
    val itemNumber: String,
    val uom: String,
    val maxQty: Double,
    var qty: Double = 0.0,
    var lot: String = "",
    var expiry: String = "" // ISO yyyy-MM-dd
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

    fun setPoNumber(po: String) {
        _state.value = _state.value.copy(poNumber = po)
    }

    fun fetchPo() = viewModelScope.launch {
        val s = _state.value
        if (s.poNumber.isBlank()) return@launch
        _state.value = s.copy(loading = true, error = null)
        repo.fetchPo(s.poNumber)
            .onSuccess { po ->
                _state.value = _state.value.copy(loading = false, po = po, step = GrnStep.SHOW_PO)
                fetchPoLines(po.POHeaderId)
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Failed to load PO")
            }
    }

    private fun fetchPoLines(headerId: String) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        repo.fetchPoLines(headerId)
            .onSuccess { items ->
                val inputs = items.map {
                    LineInput(
                        lineNumber = it.LineNumber,
                        itemNumber = it.Item,
                        uom = it.UOM,
                        maxQty = it.Quantity,
                        qty = it.Quantity,
                        lot = "BATCH20250811",
                        expiry = "2026-08-15"
                    )
                }
                _state.value = _state.value.copy(
                    loading = false,
                    lines = items,
                    lineInputs = inputs,
                    step = GrnStep.RECEIVE_LINES
                )
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
    }

    fun updateLine(
        lineNumber: Int,
        qty: Double? = null,
        lot: String? = null,
        expiry: String? = null
    ) {
        val updated = _state.value.lineInputs.map {
            if (it.lineNumber == lineNumber) it.copy(
                qty = qty ?: it.qty,
                lot = lot ?: it.lot,
                expiry = expiry ?: it.expiry
            ) else it
        }
        _state.value = _state.value.copy(lineInputs = updated)
    }

    fun buildPayloadAndReview() {
        val s = _state.value
        val po = s.po ?: return
        val lines = s.lineInputs
            .filter { it.qty > 0.0 }
            .map { inp ->
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

    fun startOver() {
        _state.value = GrnUiState()
    }
}
