package com.lazymohan.zebraprinter.ui

sealed class PrinterEvents {
    data class UpdateItemNum(val itemNum: String) : PrinterEvents()
    data class UpdateDescription(val description: String) : PrinterEvents()
    data class UpdateNoOfCopies(val noOfCopies: String) : PrinterEvents()
    object Print : PrinterEvents()
    data object RemoveError : PrinterEvents()
}