// app/src/main/java/com/lazymohan/zebraprinter/grn/ui/GrnActivity.kt
package com.lazymohan.zebraprinter.grn.ui

import android.os.Bundle
import android.util.Log
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

        val initialPo = intent.getStringExtra("po_number")
        val scanJson = intent.getStringExtra("scan_extract_json")

        val scanImageCachePaths: ArrayList<String>? =
            intent.getStringArrayListExtra("scan_image_cache_paths")

        Log.d(
            "GrnActivity",
            "onCreate: po=$initialPo, json=${scanJson?.length ?: 0} chars, cachePaths=${scanImageCachePaths?.size ?: 0}"
        )

        setContent {
            TUITheme {
                val vm: GrnViewModel = hiltViewModel()
                val ui by vm.state.collectAsState()
                val snack = remember { SnackbarHostState() }

                LaunchedEffect(initialPo, scanJson, scanImageCachePaths) {
                    android.util.Log.d(
                        "GrnActivity",
                        "prefillFromScan(po=$initialPo, jsonLen=${scanJson?.length ?: 0}, paths=${scanImageCachePaths?.size ?: 0})"
                    )
                    vm.prefillFromScan(
                        po = initialPo,
                        scanJson = scanJson,
                        cachePaths = scanImageCachePaths ?: arrayListOf()
                    )
                }


                GrnScreens(
                    ui = ui,
                    snackbarHostState = snack,
                    onEnterPo = { vm.setPoNumber(it) },
                    onFetchPo = { vm.fetchPo() },
                    onUpdateLine = { ln, q, l, e -> vm.updateLine(ln, q, l, e) },
                    onRemoveLine = { ln -> vm.removeLine(ln) },
                    onAddLine = { vm.addLineFromPo(it) },
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
