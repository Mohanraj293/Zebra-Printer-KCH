package com.lazymohan.zebraprinter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.lazymohan.zebraprinter.model.PrintContentModel
import com.lazymohan.zebraprinter.ui.PrinterEvents
import com.lazymohan.zebraprinter.ui.PrinterScreenContent
import com.lazymohan.zebraprinter.ui.PrinterViewModel
import com.tarkalabs.tarkaui.theme.TUITheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

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
            PrinterEvents.ClearError -> TODO()
            PrinterEvents.Print -> {
                viewModel.printerDiscovery()
            }
            is PrinterEvents.SelectPrinter -> TODO()
            PrinterEvents.StartDiscovery -> TODO()
            PrinterEvents.StopDiscovery -> TODO()
            is PrinterEvents.UpdateDescription -> {
                viewModel.updateDescription(event.description)
            }
            is PrinterEvents.UpdateItemNum -> {
                viewModel.updateItemNum(event.itemNum)
            }
            is PrinterEvents.UpdateNoOfCopies -> {
                viewModel.updateNoOfCopies(event.noOfCopies)
            }
        }
    }

    private suspend fun printerDiscovery() {
        withContext(Dispatchers.IO) {
            val printModel = PrintContentModel(
                itemNum = "Sample Item",
                description = "Sample Description"
            )
            printerService.discoverPrinters().collectLatest {
                printerService.connectAndPrint(
                    macAddress = it.macAddress,
                    itemData = printModel,
                    noOfCopies = 1
                )
            }
        }
    }
}