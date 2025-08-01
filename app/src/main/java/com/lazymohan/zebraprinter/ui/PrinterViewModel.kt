package com.lazymohan.zebraprinter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazymohan.zebraprinter.PrinterService
import com.lazymohan.zebraprinter.model.DiscoveredPrinterInfo
import com.lazymohan.zebraprinter.model.PrintContentModel
import com.lazymohan.zebraprinter.snacbarmessage.SnackBarMessage
import com.lazymohan.zebraprinter.utils.launchWithHandler
import com.tarkalabs.tarkaui.components.TUISnackBarType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrinterViewModel @Inject constructor(
    private val printerService: PrinterService
) : ViewModel() {

    init {
        getZPLPrinterList()
    }

    private val _uiState = MutableStateFlow(PrinterUiState())
    val uiState = _uiState.asStateFlow()


    fun updateDescription(description: String) {
        _uiState.update { currentState ->
            currentState.copy(description = description)
        }
    }

    fun updateItemNum(itemNum: String) {
        _uiState.update { currentState ->
            currentState.copy(itemNum = itemNum)
        }
    }

    fun updateNoOfCopies(noOfCopies: String) {
        _uiState.update { currentState ->
            currentState.copy(noOfCopies = noOfCopies)
        }
    }

    fun printerDiscovery() {
        viewModelScope.launchWithHandler(
            dispatcher = Dispatchers.IO,
            onCoroutineException = ::handleErrors
        ) {
            val printModel = PrintContentModel(
                itemNum = _uiState.value.itemNum.orEmpty(),
                description = _uiState.value.description.orEmpty()
            )

            if (validateData() && isValidInput()) {
                showLoading()
                printerService.connectAndPrint(
                    macAddress = _uiState.value.selectedPrinter?.macAddress.orEmpty(),
                    itemData = printModel,
                    noOfCopies = _uiState.value.noOfCopies.toInt()
                )
                hideLoading()
                clearData(_uiState.value.selectedPrinter ?: DiscoveredPrinterInfo("", ""))
            } else {
                _uiState.update {
                    it.copy(
                        snackBarMessage = SnackBarMessage.StringMessage("Please fill in all the fields")
                    )
                }
            }
        }
    }

    fun clearData(selectedPrinter: DiscoveredPrinterInfo) {
        _uiState.update {
            PrinterUiState(
                itemNum = null,
                description = null,
                noOfCopies = "",
                snackBarMessage = SnackBarMessage.StringMessage("Printing successful!"),
                snackBarType = TUISnackBarType.Success,
                selectedPrinter = selectedPrinter
            )
        }
    }

    fun handleErrors(throwable: Throwable) {
        updateSnackBarMessage(message = SnackBarMessage.StringMessage(throwable.message))
    }

    fun removeError() {
        _uiState.update { it.copy(snackBarMessage = null, snackBarType = TUISnackBarType.Error) }
    }

    fun validateData() = _uiState.value.run {
        itemNum.isNullOrEmpty().not() && description.isNullOrEmpty()
            .not() && noOfCopies.isEmpty().not()
    }

    fun isValidInput() = _uiState.value.run {
        noOfCopies.toInt() > 0
    }

    fun updateError(errorMessage: String) {
        _uiState.update {
            it.copy(
                snackBarMessage = SnackBarMessage.StringMessage(errorMessage)
            )
        }
    }

    fun showLoading() {
        _uiState.update { it.copy(isLoading = true) }
    }

    fun hideLoading() {
        _uiState.update { it.copy(isLoading = false) }
    }

    fun updateSnackBarMessage(
        message: SnackBarMessage?,
        type: TUISnackBarType = TUISnackBarType.Error
    ) {
        _uiState.update {
            it.copy(
                snackBarMessage = message,
                snackBarType = type,
                isLoading = false
            )
        }
    }

    fun updateBottomSheet(show: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(showDialog = show)
        }
    }

    fun getZPLPrinterList() {
        viewModelScope.launch(Dispatchers.IO) {
            val printers = printerService.getBondedZebraPrinters()
            _uiState.update { currentState ->
                currentState.copy(
                    discoveredPrinters = printers
                )
            }
        }
    }

    fun updateSelectedPrinter(selectedPrinter: DiscoveredPrinterInfo) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedPrinter = selectedPrinter,
                showDialog = false
            )
        }
    }
}