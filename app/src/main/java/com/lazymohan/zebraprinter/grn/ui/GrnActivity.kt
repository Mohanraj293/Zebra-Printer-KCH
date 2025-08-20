// app/src/main/java/com/lazymohan/zebraprinter/grn/ui/GrnActivity.kt
package com.lazymohan.zebraprinter.grn.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.tarkalabs.tarkaui.theme.TUITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GrnActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
// inside onCreate, before setContent { ... } or keep your structure and put inside setContent using LaunchedEffect
        val initialPo = intent.getStringExtra("po_number")
        val scanJson = intent.getStringExtra("scan_extract_json")

        setContent {
            TUITheme {
                val vm: GrnViewModel = hiltViewModel()
                val ui by vm.state.collectAsState()
                val snack = remember { SnackbarHostState() }

                LaunchedEffect(initialPo, scanJson) {
                    vm.prefillFromScan(initialPo, scanJson)
                }

                GrnScreens(
                    ui = ui,
                    snackbarHostState = snack,
                    onEnterPo = { vm.setPoNumber(it) },
                    onFetchPo = { vm.fetchPo() },
                    onUpdateLine = { ln, q, l, e -> vm.updateLine(ln, q, l, e) },
                    onRemoveLine = { ln -> vm.removeLine(ln) },
                    onAddLine = { vm.addLineFromPo(it) },   // NEW
                    onEditReceive = { vm.backToReceive() },
                    onReview = { vm.buildPayloadAndReview() },
                    onSubmit = { vm.submitReceipt() },
                    onStartOver = { vm.startOver() },
                    onBack = { finish() }
                )

            }
        }

    }
}
