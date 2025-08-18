package com.lazymohan.zebraprinter.scan.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.lazymohan.zebraprinter.scan.model.ExtractedHeader
import com.lazymohan.zebraprinter.scan.model.ExtractedItem
import com.tarkalabs.tarkaui.theme.TUITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScanGrnActivity : ComponentActivity() {

    companion object {
        const val EXTRA_HEADER = "extra_scan_grn_header"
        const val EXTRA_ITEMS  = "extra_scan_grn_items"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TUITheme {
                val vm: ScanGrnViewModel = hiltViewModel()
                val ui by vm.ui.collectAsState()
                val snack = remember { SnackbarHostState() }

                // bootstrap once from intent
                LaunchedEffect(Unit) {
                    val header = intent.getParcelableExtra<ExtractedHeader>(EXTRA_HEADER)
                    val items  = intent.getParcelableArrayListExtra<ExtractedItem>(EXTRA_ITEMS) ?: arrayListOf()
                    vm.bootstrapFromScan(header, items)
                }

                ScanGrnScreens(
                    ui = ui,
                    snackbarHostState = snack,
                    // Results
                    onUpdateExtractedItem = vm::updateExtractedItem,
                    onConfirmResults      = vm::confirmResultsToEnterPo,
                    // PO
                    onEnterPo             = vm::enterPo,
                    onFetchPo             = vm::fetchPo,
                    onBackToResults       = vm::backToResults,
                    // Receive
                    onUpdateLine          = vm::updateLine,
                    onRemoveLine          = vm::removeLine,
                    onReview              = vm::goToReview,
                    // Review
                    onSubmit              = vm::submitReceipt,
                    onEditReceive         = vm::backToReceive,
                    // Summary
                    onStartOver           = vm::startOver,
                    // Exit
                    onExitFlow            = { finish() }
                )
            }
        }
    }
}
