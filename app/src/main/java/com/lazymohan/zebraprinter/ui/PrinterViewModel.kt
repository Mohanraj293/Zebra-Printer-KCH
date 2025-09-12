package com.lazymohan.zebraprinter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.lazymohan.zebraprinter.PrinterService
import com.lazymohan.zebraprinter.app.AppPref
import com.lazymohan.zebraprinter.model.DiscoveredPrinterInfo
import com.lazymohan.zebraprinter.model.PrintContentModel
import com.lazymohan.zebraprinter.product.data.Lots
import com.lazymohan.zebraprinter.snacbarmessage.SnackBarMessage
import com.lazymohan.zebraprinter.utils.DateTimeConverter
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
    private val appPref: AppPref,
    private val dateTimeConverter: DateTimeConverter,
    @Assisted("item") private val lots: Lots,
    @Assisted("gtinNumber") private val gtinNum: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PrinterUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launchWithHandler(
            dispatcher = Dispatchers.IO,
            onCoroutineException = ::handleErrors
        ) {
            _uiState.update { currentState ->
                currentState.copy(
                    canDiscoverPrinter = appPref.canDiscover
                )
            }
            if (appPref.canDiscover)
                getZPLPrinterList()
        }
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

    fun updateNoOfCopies(noOfCopies: Int) {
        _uiState.update { currentState ->
            if (noOfCopies > 0) {
                currentState.copy(noOfCopies = noOfCopies.toString())
            } else {
                currentState.copy(noOfCopies = "")
            }
        }
    }

    fun printerDiscovery() {
        viewModelScope.launchWithHandler(
            dispatcher = Dispatchers.IO,
            onCoroutineException = ::handleErrors
        ) {
            showLoading()
            val trimmedDescription = if (lots.itemDescription.orEmpty().length > 25) {
                lots.itemDescription?.take(30) + "…"
            } else {
                lots.itemDescription
            }
            val printModel = PrintContentModel(
                itemNum = lots.itemNumber.orEmpty(),
                description = trimmedDescription.orEmpty(),
                gtinNum = gtinNum,
                batchNo = lots.lotNumber.orEmpty(),
                expiryDate = dateTimeConverter.getGS1DisplayDate(lots.expirationDate),
                labelExpiryDate = dateTimeConverter.getLabelDisplayDate(lots.expirationDate)
            )

            if (gtinNum.isEmpty()) {
                updateSnackBarMessage(
                    message = SnackBarMessage.StringMessage("GTIN Number is missing, cannot print label"),
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
            if (!isValidInput()) {
                updateSnackBarMessage(
                    message = SnackBarMessage.StringMessage("Please enter a valid number of copies"),
                    type = TUISnackBarType.Error
                )
                hideLoading()
                return@launchWithHandler
            }
            printerService.connectAndPrint(
                macAddress = _uiState.value.selectedPrinter?.macAddress.orEmpty(),
                itemData = printModel,
                noOfCopies = _uiState.value.noOfCopies.toDouble().toInt()
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
        Firebase.crashlytics.recordException(throwable)
        updateSnackBarMessage(message = SnackBarMessage.StringMessage(throwable.message))
    }

    fun removeError() {
        _uiState.update { it.copy(snackBarMessage = null, snackBarType = TUISnackBarType.Error) }
    }

    fun isValidInput() = _uiState.value.run {
        (noOfCopies.toIntOrNull() ?: 0) > 0
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
        appPref.canDiscover = canDiscover
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