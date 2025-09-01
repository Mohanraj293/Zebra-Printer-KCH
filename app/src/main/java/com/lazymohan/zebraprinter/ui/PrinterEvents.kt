package com.lazymohan.zebraprinter.ui

import com.lazymohan.zebraprinter.model.DiscoveredPrinterInfo

sealed class PrinterEvents {
    data class UpdateNoOfCopies(val noOfCopies: Int) : PrinterEvents()
    data object Print : PrinterEvents()
    data object RemoveError : PrinterEvents()
    data class UpdateError(val errorMessage: String) : PrinterEvents()
    data class UpdateBottomSheet(val show: Boolean) : PrinterEvents()
    data class CanDiscover(
        val canDiscoverPrinter: Boolean,
    ) : PrinterEvents()
    data class UpdateSelectedPrinter(
        val selectedPrinter: DiscoveredPrinterInfo
    ) : PrinterEvents()
}