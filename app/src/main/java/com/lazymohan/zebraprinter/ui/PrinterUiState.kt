package com.lazymohan.zebraprinter.ui

import com.lazymohan.zebraprinter.model.DiscoveredPrinterInfo
import com.lazymohan.zebraprinter.model.PrintContentModel

data class PrinterUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val itemNum: String? = null,
    val description: String? = null,
    val discoveredPrinters: List<DiscoveredPrinterInfo> = emptyList(),
    val selectedPrinter: DiscoveredPrinterInfo? = null,
    val printContent: PrintContentModel? = null,
    val noOfCopies: String? = null
)