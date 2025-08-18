package com.lazymohan.zebraprinter.scan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazymohan.zebraprinter.scan.data.ScanGrnRepository
import com.lazymohan.zebraprinter.scan.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanGrnViewModel @Inject constructor(
    private val repo: ScanGrnRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(ScanGrnUiState(step = ScanGrnStep.RESULTS))
    val ui: StateFlow<ScanGrnUiState> = _ui

    /* -------- BOOTSTRAP (from intent) -------- */
    fun bootstrapFromScan(header: ExtractedHeader?, items: List<ExtractedItem>) {
        if (_ui.value.extractedHeader == null && _ui.value.extractedItems.isEmpty()) {
            _ui.update {
                it.copy(
                    extractedHeader = header,
                    extractedItems  = items,
                    poNumber        = header?.poGuess.orEmpty(),
                    step            = ScanGrnStep.RESULTS
                )
            }
        }
    }

    /* -------- RESULTS -------- */
    fun updateExtractedItem(index: Int, qty: Double?, lot: String?, exp: String?) {
        _ui.update { s ->
            val list = s.extractedItems.toMutableList()
            val old = list.getOrNull(index) ?: return
            list[index] = old.copy(
                qtyDelivered = qty ?: old.qtyDelivered,
                batchNo = lot ?: old.batchNo,
                expiryDate = exp ?: old.expiryDate
            )
            s.copy(extractedItems = list)
        }
    }
    fun confirmResultsToEnterPo() {
        _ui.update { s -> s.copy(step = ScanGrnStep.ENTER_PO, poNumber = s.poNumber.ifBlank { s.extractedHeader?.poGuess.orEmpty() }) }
    }

    /* -------- ENTER PO -------- */
    fun enterPo(text: String) { _ui.update { it.copy(poNumber = text) } }

    fun fetchPo() {
        val number = _ui.value.poNumber
        if (number.isBlank()) return
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val res = repo.fetchPo(number)
            res.onSuccess { (header, lines) ->
                _ui.update { s ->
                    s.copy(
                        loading = false,
                        step = ScanGrnStep.SHOW_PO,
                        po = header,
                        lines = lines,
                        lineInputs = prefillFromExtracted(lines, s.extractedItems)
                    )
                }
            }.onFailure { e ->
                _ui.update { it.copy(loading = false, error = e.message ?: "Failed to fetch PO.") }
            }
        }
    }

    private fun prefillFromExtracted(lines: List<PoLine>, extracted: List<ExtractedItem>): List<LineInput> {
        val out = mutableListOf<LineInput>()
        lines.forEach { ln ->
            val m = extracted.firstOrNull { ei ->
                (ei.itemNumber != null && ei.itemNumber.equals(ln.Item, ignoreCase = true)) ||
                        ei.description.equals(ln.Description, ignoreCase = true)
            }
            if (m != null) {
                out += LineInput(
                    lineNumber = ln.LineNumber,
                    qty = m.qtyDelivered ?: 0.0,
                    lot = m.batchNo.orEmpty(),
                    expiry = m.expiryDate.orEmpty()
                )
            }
        }
        return out
    }

    fun backToResults() { _ui.update { it.copy(step = ScanGrnStep.RESULTS, error = null) } }

    /* -------- RECEIVE -------- */
    fun updateLine(lineNumber: Int, qty: Double?, lot: String?, exp: String?) {
        _ui.update { s ->
            val cur = s.lineInputs.toMutableList()
            val idx = cur.indexOfFirst { it.lineNumber == lineNumber }
            if (idx >= 0) {
                val old = cur[idx]
                cur[idx] = old.copy(
                    qty = qty ?: old.qty,
                    lot = lot ?: old.lot,
                    expiry = exp ?: old.expiry
                )
            } else {
                cur += LineInput(lineNumber, qty ?: 0.0, lot.orEmpty(), exp.orEmpty())
            }
            s.copy(lineInputs = cur)
        }
    }
    fun removeLine(lineNumber: Int) {
        _ui.update { it.copy(lineInputs = it.lineInputs.filterNot { li -> li.lineNumber == lineNumber }) }
    }
    fun goToReview() {
        val s = _ui.value
        val po = s.po ?: return
        val valid = s.lineInputs.filter { it.qty > 0.0 && it.lot.isNotBlank() && it.expiry.isNotBlank() }
        val byNo = s.lines.associateBy { it.LineNumber }
        val lines = valid.mapNotNull { inp ->
            val pl = byNo[inp.lineNumber] ?: return@mapNotNull null
            ReceiptLine(
                DocumentLineNumber = pl.LineNumber,
                ItemNumber = pl.Item ?: "NA",
                Quantity = inp.qty,
                UnitOfMeasure = pl.UOM,
                lotItemLots = listOf(LotInfo(inp.lot, inp.expiry))
            )
        }
        val payload = ReceiptPayload(
            VendorName = po.Supplier,
            VendorSiteCode = po.SupplierSite,
            BusinessUnit = po.ProcurementBU,
            lines = lines
        )
        _ui.update { it.copy(step = ScanGrnStep.REVIEW, payload = payload) }
    }

    /* -------- REVIEW -------- */
    fun submitReceipt() {
        val p = _ui.value.payload ?: return
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val res = repo.submitReceipt(p)
            res.onSuccess { (receipt, errors) ->
                _ui.update { it.copy(loading = false, step = ScanGrnStep.SUMMARY, receipt = receipt, lineErrors = errors) }
            }.onFailure { e ->
                _ui.update { it.copy(loading = false, error = e.message ?: "Failed to submit receipt.") }
            }
        }
    }
    fun backToReceive() { _ui.update { it.copy(step = ScanGrnStep.SHOW_PO) } }
    fun startOver() {
        _ui.update {
            it.copy(
                step = ScanGrnStep.RESULTS,
                po = null, lines = emptyList(), lineInputs = emptyList(),
                payload = null, receipt = null, lineErrors = emptyList(), error = null, loading = false
            )
        }
    }
}
