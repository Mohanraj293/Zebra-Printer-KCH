package com.lazymohan.zebraprinter.ui

import com.lazymohan.zebraprinter.model.DiscoveredPrinterInfo

sealed class PrinterEvents {
    data class UpdateItemNum(val itemNum: String) : PrinterEvents()
    data class UpdateDescription(val description: String) : PrinterEvents()
    data class UpdateNoOfCopies(val noOfCopies: String) : PrinterEvents()
    data class Print(val canDiscoverPrinter: Boolean) : PrinterEvents()
    data object RemoveError : PrinterEvents()
    data class UpdateError(val errorMessage: String) : PrinterEvents()
    data class UpdateBottomSheet(val show: Boolean) : PrinterEvents()
    data class UpdateSelectedPrinter(val selectedPrinter: DiscoveredPrinterInfo) : PrinterEvents()
}