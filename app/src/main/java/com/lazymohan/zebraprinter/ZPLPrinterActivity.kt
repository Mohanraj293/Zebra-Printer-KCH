package com.lazymohan.zebraprinter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lazymohan.zebraprinter.effects.RequestBluetoothPermission
import com.lazymohan.zebraprinter.product.data.Item
import com.lazymohan.zebraprinter.ui.PrinterEvents
import com.lazymohan.zebraprinter.ui.PrinterScreenContent
import com.lazymohan.zebraprinter.ui.PrinterViewModel
import com.tarkalabs.tarkaui.theme.TUITheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ZPLPrinterActivity : ComponentActivity() {
    private lateinit var printerService: PrinterService

    @Inject
    lateinit var printerViewModelFactory: PrinterViewModel.PrinterViewModelFactory

    private val viewModel: PrinterViewModel by viewModels {
        PrinterViewModel.providesFactory(
            assistedFactory = printerViewModelFactory,
            item = intent.getSerializableExtra("product") as Item
        )
    }

    private var requestPrint = mutableStateOf(false)

    companion object {
        fun getCallingIntent(context: Context, product: Item): Intent {
            return Intent(context, ZPLPrinterActivity::class.java).apply {
                putExtra("product", product)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        printerService = PrinterImpl(this)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val product = intent.getSerializableExtra("product") as? Item
            TUITheme {
                PrinterScreenContent(
                    handleEvents = ::handleEvents,
                    uiState = uiState,
                    item = product,
                    onBackPressed = { onBackPressedDispatcher.onBackPressed() }
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