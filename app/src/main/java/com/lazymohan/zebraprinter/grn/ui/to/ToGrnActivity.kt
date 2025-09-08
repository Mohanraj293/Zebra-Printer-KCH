package com.lazymohan.zebraprinter.grn.ui.to

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import com.lazymohan.zebraprinter.grn.ui.Header
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ToGrnActivity : ComponentActivity() {
    private val vm: ToViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.getStringExtra("to_number")?.let { preset ->
            vm.onEnterToNumber(preset)
        }

        setContent {
            MaterialTheme {
                val ui by vm.ui.collectAsState()
                val snack = remember { SnackbarHostState() }
                val step = ui.step
                val scope = rememberCoroutineScope()

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
                                ToStep.ENTER   -> "Create Receipt — TO"
                                ToStep.RECEIVE -> "Receive Items — TO"
                                ToStep.REVIEW  -> "Review & Submit"
                                ToStep.SUMMARY -> "Receipt Summary"
                            },
                            subtitle = when (step) {
                                ToStep.ENTER   -> "Fetch Transfer Order"
                                ToStep.RECEIVE -> "Enter quantities & lots"
                                ToStep.REVIEW  -> "Confirm details"
                                ToStep.SUMMARY -> "Completed"
                            },
                            onLogoClick = { finish() },
                            step = when (step) {
                                ToStep.ENTER   -> 1
                                ToStep.RECEIVE -> 2
                                ToStep.REVIEW  -> 3
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
            Surface(color = bg, shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)) {
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
