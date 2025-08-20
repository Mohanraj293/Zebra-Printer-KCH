package com.lazymohan.zebraprinter.ui

import com.lazymohan.zebraprinter.app.AppPref
import com.lazymohan.zebraprinter.model.DiscoveredPrinterInfo
import com.lazymohan.zebraprinter.snacbarmessage.SnackBarMessage
import com.tarkalabs.tarkaui.components.TUISnackBarType

data class PrinterUiState(
    val isLoading: Boolean = false,
    val showDialog: Boolean = false,
    val canDiscoverPrinter: Boolean = AppPref.canDiscover,
    val discoveredPrinters: List<DiscoveredPrinterInfo> = emptyList(),
    val selectedPrinter: DiscoveredPrinterInfo? = null,
    val noOfCopies: String = "",
    val snackBarMessage: SnackBarMessage? = null,
    val snackBarType: TUISnackBarType = TUISnackBarType.Error,
)