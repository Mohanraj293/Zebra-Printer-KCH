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
import com.lazymohan.zebraprinter.product.data.Lots
import com.lazymohan.zebraprinter.ui.PrinterEvents
import com.lazymohan.zebraprinter.ui.PrinterScreenContent
import com.lazymohan.zebraprinter.ui.PrinterViewModel
import com.lazymohan.zebraprinter.utils.DateTimeConverter
import com.tarkalabs.tarkaui.theme.TUITheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ZPLPrinterActivity : ComponentActivity() {
    private lateinit var printerService: PrinterService

    @Inject
    lateinit var printerViewModelFactory: PrinterViewModel.PrinterViewModelFactory

    @Inject
    lateinit var dateTimeConverter: DateTimeConverter

    private val viewModel: PrinterViewModel by viewModels {
        PrinterViewModel.providesFactory(
            assistedFactory = printerViewModelFactory,
            lots = intent.getSerializableExtra("product") as Lots,
            gtinNum = intent.getStringExtra("gtinNumber")!!
        )
    }

    private var requestPrint = mutableStateOf(false)

    companion object {
        fun getCallingIntent(
            context: Context,
            gtinNumber: String,
            product: Lots,
            qty: String? = null
        ): Intent {
            return Intent(context, ZPLPrinterActivity::class.java).apply {
                putExtra("product", product)
                putExtra("gtinNumber", gtinNumber)
                if (!qty.isNullOrEmpty()) {
                    putExtra("qty", qty)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        printerService = PrinterImpl(this)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val product = intent.getSerializableExtra("product") as? Lots
            val gtinNumber: String? = intent.getStringExtra("gtinNumber")
            val qty: String? = intent.getStringExtra("qty")
            if (!qty.isNullOrEmpty()) {
                viewModel.updateNoOfCopies(qty)
            }
            TUITheme {
                PrinterScreenContent(
                    handleEvents = ::handleEvents,
                    uiState = uiState,
                    lots = product,
                    gtinNumber = gtinNumber,
                    dateTimeConverter = dateTimeConverter,
                    onBackPressed = { onBackPressedDispatcher.onBackPressed() }
                )
                RequestBluetoothPermission(
                    trigger = requestPrint.value,
                    onResult = { granted ->
                        requestPrint.value = false
                        if (granted) {
                            viewModel.updateCanDiscoverPrinter(true)
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
                viewModel.printerDiscovery()
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

            is PrinterEvents.CanDiscover -> {
                requestPrint.value = event.canDiscoverPrinter
            }
        }
    }
}