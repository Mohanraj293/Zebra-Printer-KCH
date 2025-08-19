package com.lazymohan.zebraprinter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lazymohan.zebraprinter.PrinterService
import com.lazymohan.zebraprinter.model.DiscoveredPrinterInfo
import com.lazymohan.zebraprinter.model.PrintContentModel
import com.lazymohan.zebraprinter.product.data.Lots
import com.lazymohan.zebraprinter.snacbarmessage.SnackBarMessage
import com.lazymohan.zebraprinter.utils.launchWithHandler
import com.tarkalabs.tarkaui.components.TUISnackBarType
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PrinterViewModel @AssistedInject constructor(
    private val printerService: PrinterService,
    @Assisted("item") private val lots: Lots,
    @Assisted("gtinNumber") private val gtinNum: String,
) : ViewModel() {

    init {
        getZPLPrinterList()
    }

    @AssistedFactory
    interface PrinterViewModelFactory {
        fun create(
            @Assisted("item") lots: Lots,
            @Assisted("gtinNumber") gtinNum: String
        ): PrinterViewModel
    }

    @Suppress("UNCHECKED_CAST")
    companion object {
        fun providesFactory(
            assistedFactory: PrinterViewModelFactory,
            lots: Lots,
            gtinNum: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>) = assistedFactory.create(
                lots = lots,
                gtinNum = gtinNum
            ) as T
        }
    }

    private val _uiState = MutableStateFlow(PrinterUiState())
    val uiState = _uiState.asStateFlow()

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
            showLoading()
            val printModel = PrintContentModel(
                itemNum = lots.itemNumber.orEmpty(),
                description = lots.itemDescription.orEmpty(),
                gtinNum = gtinNum,
                batchNo = lots.lotNumber.orEmpty(),
                expiryDate = lots.expirationDate.toString(),
            )

            if (!isValidInput()) {
                updateSnackBarMessage(
                    message = SnackBarMessage.StringMessage("Please enter a valid number of copies"),
                    type = TUISnackBarType.Error
                )
                hideLoading()
                return@launchWithHandler
            }
            if (_uiState.value.selectedPrinter == null) {
                updateSnackBarMessage(
                    message = SnackBarMessage.StringMessage("Please select a printer"),
                    type = TUISnackBarType.Error
                )
                hideLoading()
                return@launchWithHandler
            }
            printerService.connectAndPrint(
                macAddress = _uiState.value.selectedPrinter?.macAddress.orEmpty(),
                itemData = printModel,
                noOfCopies = _uiState.value.noOfCopies.toInt()
            )
            hideLoading()
            clearData(_uiState.value.selectedPrinter ?: DiscoveredPrinterInfo("", ""))
        }
    }

    fun clearData(selectedPrinter: DiscoveredPrinterInfo) {
        _uiState.update {
            PrinterUiState(
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

    fun updateCanDiscoverPrinter(canDiscover: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                canDiscoverPrinter = canDiscover
            )
        }
        getZPLPrinterList()
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