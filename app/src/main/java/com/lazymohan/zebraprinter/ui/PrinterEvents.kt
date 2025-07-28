package com.lazymohan.zebraprinter.ui

sealed class PrinterEvents {
    data class UpdateItemNum(val itemNum: String) : PrinterEvents()
    data class UpdateDescription(val description: String) : PrinterEvents()
    data class UpdateNoOfCopies(val noOfCopies: String) : PrinterEvents()
    data object Print : PrinterEvents()
    data object RemoveError : PrinterEvents()
    data class UpdateError(val errorMessage: String) : PrinterEvents()
    data class UpdatePrintButton(val showPrintButton: Boolean) : PrinterEvents()
}