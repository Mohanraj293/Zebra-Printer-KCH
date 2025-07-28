package com.lazymohan.zebraprinter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazymohan.zebraprinter.PrinterService
import com.lazymohan.zebraprinter.model.PrintContentModel
import com.lazymohan.zebraprinter.snacbarmessage.SnackBarMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PrinterViewModel(
    private val printerService: PrinterService
) : ViewModel() {

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
        viewModelScope.launch(Dispatchers.IO) {
            val printModel = PrintContentModel(
                itemNum = _uiState.value.itemNum.orEmpty(),
                description = _uiState.value.description.orEmpty()
            )
            if (validateData()) printerService.discoverPrinters().collectLatest {
                printerService.connectAndPrint(
                    macAddress = it.macAddress,
                    itemData = printModel,
                    noOfCopies = _uiState.value.noOfCopies?.toIntOrNull() ?: 1
                )
            }
            else _uiState.update {
                it.copy(
                    snackBarMessage = SnackBarMessage.StringMessage("Please fill in all the fields")
                )
            }
        }
    }

    fun removeError() {
        _uiState.update { it.copy(snackBarMessage = null) }
    }

    fun validateData() = _uiState.value.run {
        itemNum.isNullOrEmpty().not() && description.isNullOrEmpty()
            .not() && noOfCopies.isNullOrEmpty().not()
    }

    fun updateError(errorMessage: String) {
        _uiState.update {
            it.copy(
                snackBarMessage = SnackBarMessage.StringMessage(errorMessage)
            )
        }
    }

    fun updatePrintButton(showPrintButton: Boolean) {
        _uiState.update {
            it.copy(showPrintButton = showPrintButton)
        }
    }
}