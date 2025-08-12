package com.lazymohan.zebraprinter.utils

import androidx.compose.runtime.Composable


@Composable
fun PrinterSelectionCard(
    selectedPrinter: String?,
    onPrinterSelectionClick: () -> Unit
) {
    EAMCustomSelectionCard(
        description = selectedPrinter,
        label = "Select Printer",
        onCardClicked = onPrinterSelectionClick,
        showTrailingIcon = true
    )
}