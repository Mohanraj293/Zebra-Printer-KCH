package com.lazymohan.zebraprinter.ui

import com.lazymohan.zebraprinter.model.DiscoveredPrinterInfo

sealed class PrinterEvents {
    data class SelectPrinter(val printer: DiscoveredPrinterInfo) : PrinterEvents()
    data class UpdateItemNum(val itemNum: String) : PrinterEvents()
    data class UpdateDescription(val description: String) : PrinterEvents()
    data class UpdateNoOfCopies(val noOfCopies: String) : PrinterEvents()
    object StartDiscovery : PrinterEvents()
    object StopDiscovery : PrinterEvents()
    object Print : PrinterEvents()
    object ClearError : PrinterEvents()
}