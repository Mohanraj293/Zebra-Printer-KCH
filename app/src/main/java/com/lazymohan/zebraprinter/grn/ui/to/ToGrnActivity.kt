package com.lazymohan.zebraprinter.grn.ui.to

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lazymohan.zebraprinter.grn.ui.Header
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ToGrnActivity : ComponentActivity() {
    private val vm: ToViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialTo = intent!!.getStringExtra("to_number")
        val scanJson = intent!!.getStringExtra("scan_extract_json")
        val scanImageCachePaths: ArrayList<String>? =
            intent!!.getStringArrayListExtra("scan_image_cache_paths")
        vm.onEnterToNumber(initialTo.orEmpty())
        setContent {
            MaterialTheme {
                val ui by vm.ui.collectAsState()
                val snack = remember { SnackbarHostState() }
                val step = ui.step
                val scope = rememberCoroutineScope()

                LaunchedEffect(initialTo, scanJson, scanImageCachePaths) {
                    Log.d(
                        "GrnActivity",
                        "prefillFromScan(po=$initialTo, jsonLen=${scanJson?.length ?: 0}, paths=${scanImageCachePaths?.size ?: 0})"
                    )
                    vm.prefillFromScan(
                        to = initialTo,
                        scanJson = scanJson,
                        cachePaths = scanImageCachePaths ?: arrayListOf()
                    )
                }

                Scaffold(
                    containerColor = Color(0xFFF6F8FF),
                    snackbarHost = { SnackbarHost(hostState = snack) }
                ) { inner ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(inner)
                    ) {
                        ToHeader(
                            title = when (step) {
                                ToStep.ENTER -> "Create Receipt — TO"
                                ToStep.RECEIVE -> "Receive Items — TO"
                                ToStep.REVIEW -> "Review & Submit"
                                ToStep.SUMMARY -> "Receipt Summary"
                            },
                            subtitle = when (step) {
                                ToStep.ENTER -> "Fetch Transfer Order"
                                ToStep.RECEIVE -> "Enter quantities & lots"
                                ToStep.REVIEW -> "Confirm details"
                                ToStep.SUMMARY -> "Completed"
                            },
                            onLogoClick = { finish() },
                            step = when (step) {
                                ToStep.ENTER -> 1
                                ToStep.RECEIVE -> 2
                                ToStep.REVIEW -> 3
                                ToStep.SUMMARY -> 4
                            }
                        )

                        when (step) {
                            ToStep.ENTER -> EnterToCard(
                                ui = ui,
                                onEnter = vm::onEnterToNumber,
                                onFetch = vm::fetchTo,
                                onBack = { finish() }
                            )

                            ToStep.RECEIVE -> ToAndReceiveCard(
                                header = ui.header,
                                linesLoaded = ui.linesLoaded,
                                allLines = ui.lines,
                                inputs = ui.lineInputs,
                                shipment = ui.shipment,
                                onBack = { vm.restart() },
                                onAddLine = vm::addLine,
                                onRemoveLine = vm::removeLine,
                                onAddSection = vm::addSection,
                                onRemoveSection = vm::removeSection,
                                onUpdateQty = vm::updateSectionQty,
                                onUpdateLot = vm::updateSectionLot,
                                onReview = {
                                    if (vm.canReview()) {
                                        vm.goToReview()
                                    } else {
                                        val msg = vm.reviewBlockReason()
                                        scope.launch { snack.showSnackbar(message = msg) }
                                    }
                                }
                            )

                            ToStep.REVIEW -> ReviewCardTO(
                                header = ui.header!!,
                                allLines = ui.lines,
                                inputs = ui.lineInputs,
                                submitting = ui.submitting,
                                submitError = ui.submitError,
                                onBack = vm::backToReceive,
                                onSubmit = vm::submitReceipt
                            )

                            ToStep.SUMMARY -> SummaryCardTO(
                                ui = ui,                     // ⚠️ pass the whole ui (like PO)
                                onStartOver = { vm.restart() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToHeader(
    title: String,
    subtitle: String,
    onLogoClick: () -> Unit,
    step: Int
) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF0E63FF), Color(0xFF5AA7FF)))
    Column {
        Header(
            gradient = gradient,
            title = title,
            subtitle = subtitle,
            onLogoClick = onLogoClick
        )
        ToStepper(step)
    }
}

@Composable
private fun ToStepper(step: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .offset(y = (-10).dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("TO", "Receive", "Review", "Done").forEachIndexed { idx, label ->
            val active = (idx + 1) <= step
            val bg = if (active) Color(0xFFE8F0FF) else Color(0xFFF2F4F7)
            val fg = if (active) Color(0xFF0E63FF) else Color(0xFF64748B)
            Surface(
                color = bg,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = fg,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
