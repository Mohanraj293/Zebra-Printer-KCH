package com.lazymohan.zebraprinter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lazymohan.zebraprinter.effects.RequestBluetoothPermission
import com.lazymohan.zebraprinter.ui.PrinterEvents
import com.lazymohan.zebraprinter.ui.PrinterScreenContent
import com.lazymohan.zebraprinter.ui.PrinterViewModel
import com.tarkalabs.tarkaui.theme.TUITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var printerService: PrinterService
    private val viewModel: PrinterViewModel by viewModels()
    private var requestPrint = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        printerService = PrinterImpl(this)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            TUITheme {
                PrinterScreenContent(
                    handleEvents = ::handleEvents,
                    uiState = uiState
                )
                RequestBluetoothPermission(
                    trigger = requestPrint.value,
                    onResult = { granted ->
                        requestPrint.value = false
                        if (granted) {
                            viewModel.printerDiscovery()
                        } else {
                            viewModel.updateError("Bluetooth permission denied")
                        }
                    }
                )
            }
        }
    }

    private fun handleEvents(event: PrinterEvents) {
        when (event) {
            is PrinterEvents.Print -> {
                requestPrint.value = event.canDiscoverPrinter
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

            is PrinterEvents.UpdateError -> {
                viewModel.updateError(event.errorMessage)
            }

            is PrinterEvents.UpdateBottomSheet -> {
                viewModel.updateBottomSheet(event.show)
            }

            is PrinterEvents.UpdateSelectedPrinter -> {
                viewModel.updateSelectedPrinter(event.selectedPrinter)
            }
        }
    }
}