package com.lazymohan.zebraprinter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.lazymohan.zebraprinter.ui.PrinterEvents
import com.lazymohan.zebraprinter.ui.PrinterScreenContent
import com.lazymohan.zebraprinter.ui.PrinterViewModel
import com.tarkalabs.tarkaui.theme.TUITheme

class MainActivity : ComponentActivity() {
    private lateinit var printerService: PrinterService
    private lateinit var viewModel: PrinterViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        printerService = PrinterImpl(this)
        viewModel = PrinterViewModel(printerService)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            TUITheme {
                PrinterScreenContent(
                    handleEvents = ::handleEvents,
                    uiState = uiState
                )
            }
        }
    }

    private fun handleEvents(event: PrinterEvents) {
        when (event) {
            PrinterEvents.Print -> {
                viewModel.printerDiscovery()
            }
            is PrinterEvents.UpdateDescription -> {
                viewModel.updateDescription(event.description)
            }
            is PrinterEvents.UpdateItemNum -> {
                viewModel.updateItemNum(event.itemNum)
            }
            is PrinterEvents.UpdateNoOfCopies -> {
                viewModel.updateNoOfCopies(event.noOfCopies)
            }

            PrinterEvents.RemoveError -> {
                viewModel.removeError()
            }
        }
    }
}