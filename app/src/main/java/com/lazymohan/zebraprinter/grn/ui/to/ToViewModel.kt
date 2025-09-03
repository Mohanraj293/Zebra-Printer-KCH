package com.lazymohan.zebraprinter.grn.ui.to

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazymohan.zebraprinter.grn.data.*
import com.lazymohan.zebraprinter.grn.domain.ToConfig
import com.lazymohan.zebraprinter.grn.domain.ToReceiveLineInput
import com.lazymohan.zebraprinter.grn.domain.buildToReceiptRequest
import com.lazymohan.zebraprinter.grn.domain.usecase.CreateToReceiptUseCase
import com.lazymohan.zebraprinter.grn.domain.usecase.FetchToBundleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/* ---------- UI state & models ---------- */

data class ToLineSection(
    val section: Int,
    val qty: Int,
    val lot: String
)

data class ToLineInput(
    val lineId: Long,
    val sections: List<ToLineSection> = listOf(ToLineSection(section = 1, qty = 0, lot = ""))
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

    // review/submit
    val submitting: Boolean = false,
    val submitError: String? = null,
    val receipt: ReceiptResponse? = null
)

/* ---------- steps (for Activity to drive screens) ---------- */
enum class ToStep { ENTER, RECEIVE, REVIEW, SUMMARY }

/* ---------- ViewModel ---------- */

@HiltViewModel
class ToViewModel @Inject constructor(
    private val fetchToBundle: FetchToBundleUseCase,
    private val createReceipt: CreateToReceiptUseCase
) : ViewModel() {

    private val _ui = MutableStateFlow(ToUiState())
    val ui: StateFlow<ToUiState> = _ui

    // Keep as public so Activity can read it
    var step: ToStep = ToStep.ENTER

    private val cfg = ToConfig(
        fromOrganizationCode = "KDH",
        organizationCode = "KDJ",
        employeeId = 300000068190418
    )

    /* ----- enter & fetch ----- */

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
                    step = ToStep.RECEIVE
                    _ui.value.copy(
                        loading = false,
                        header = b.header,
                        lines = b.lines,
                        shipment = b.shipment,
                        linesLoaded = true,
                        lineInputs = emptyList()
                    )
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

    /* ----- line selection ----- */

    fun addLine(lineId: Long) {
        val inputs = _ui.value.lineInputs.toMutableList()
        if (inputs.any { it.lineId == lineId }) return
        inputs += ToLineInput(lineId)
        _ui.value = _ui.value.copy(lineInputs = inputs)
    }

    fun removeLine(lineId: Long) {
        _ui.value = _ui.value.copy(
            lineInputs = _ui.value.lineInputs.filterNot { it.lineId == lineId }
        )
    }

    fun addSection(lineId: Long) {
        val li = _ui.value.lineInputs.find { it.lineId == lineId } ?: return
        val next = (li.sections.maxOfOrNull { it.section } ?: 0) + 1
        val upd = li.copy(sections = li.sections + ToLineSection(next, 0, ""))
        replaceInput(upd)
    }

    fun removeSection(lineId: Long, section: Int) {
        val li = _ui.value.lineInputs.find { it.lineId == lineId } ?: return
        val upd = li.copy(
            sections = li.sections
                .filterNot { it.section == section }
                .ifEmpty { listOf(ToLineSection(1, 0, "")) }
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
        val upd = li.copy(sections = li.sections.map {
            if (it.section == section) it.copy(lot = lot) else it
        })
        replaceInput(upd)
    }

    private fun replaceInput(newLi: ToLineInput) {
        _ui.value = _ui.value.copy(
            lineInputs = _ui.value.lineInputs.map { if (it.lineId == newLi.lineId) newLi else it }
        )
    }

    /* ----- review & submit ----- */

    fun canReview(): Boolean {
        val header = _ui.value.header ?: return false
        if (_ui.value.lineInputs.isEmpty()) return false
        val hasAnyQty = _ui.value.lineInputs.any { it.sections.any { s -> s.qty > 0 } }
        return hasAnyQty && header.headerId > 0
    }

    fun goToReview() {
        if (!canReview()) return
        step = ToStep.REVIEW
    }

    fun submitReceipt() {
        val header = _ui.value.header ?: return
        val allLinesMap = _ui.value.lines.associateBy { it.transferOrderLineId }

        val toInputs: List<ToReceiveLineInput> =
            _ui.value.lineInputs.flatMap { li ->
                val line = allLinesMap[li.lineId] ?: return@flatMap emptyList()
                li.sections
                    .filter { it.qty > 0 }
                    .map { s ->
                        ToReceiveLineInput(
                            line = line,
                            quantity = s.qty,
                            lotNumber = s.lot.ifBlank { null }
                        )
                    }
            }

        val request = buildToReceiptRequest(
            cfg = cfg,
            header = header,
            shipment = _ui.value.shipment,
            inputs = toInputs
        )

        viewModelScope.launch {
            _ui.value = _ui.value.copy(submitting = true, submitError = null)
            val res = createReceipt(request)
            _ui.value = res.fold(
                onSuccess = { r ->
                    step = ToStep.SUMMARY
                    _ui.value.copy(submitting = false, receipt = r)
                },
                onFailure = { e ->
                    _ui.value.copy(submitting = false, submitError = e.message ?: "Submit failed")
                }
            )
        }
    }

    fun restart() {
        step = ToStep.ENTER
        _ui.value = ToUiState()
    }
}
