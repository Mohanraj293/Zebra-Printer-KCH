package com.lazymohan.zebraprinter.grn.ui

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.lazymohan.zebraprinter.utils.DateTimeConverter

@Composable
fun GrnScreens(
    ui: GrnUiState,
    toUiState: ToUiState,
    isFromPickSlip: Boolean,
    snackbarHostState: SnackbarHostState,
    dateTimeConverter: DateTimeConverter,
    onEnterPo: (String) -> Unit,
    onFetchPo: () -> Unit,
    onUpdateLine: (Int, Double?, String?, String?) -> Unit,
    onRemoveLine: (Int) -> Unit,
    onAddLine: (Int) -> Unit,
    onAddSection: (Int) -> Unit,
    onRemoveSection: (Int, Int) -> Unit,
    onUpdateSection: (Int, Int, Double?, String?, String?) -> Unit,
    onEditReceive: () -> Unit,
    onReview: () -> Unit,
    onSubmit: () -> Unit,
    onStartOver: () -> Unit,
    onBack: () -> Unit,
    onAddAttachments: (List<Uri>) -> Unit
) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF0E63FF), Color(0xFF5AA7FF)))

    Scaffold(
        containerColor = Color(0xFFF6F8FF),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            Header(
                gradient = gradient,
                title = when (ui.step) {
                    GrnStep.ENTER_PO -> "Enter PO Number"
                    GrnStep.SHOW_PO  -> "Confirm & Receive"
                    GrnStep.REVIEW   -> "Review & Submit"
                    GrnStep.SUMMARY  -> "Success!"
                },
                subtitle = when (ui.step) {
                    GrnStep.ENTER_PO -> "Start a new GRN by entering the PO number"
                    GrnStep.SHOW_PO  -> "Expand a line, enter Qty/Lot/Expiry"
                    GrnStep.REVIEW   -> "Send the receipt request to Oracle"
                    GrnStep.SUMMARY  -> "Goods Receipt Note Created"
                },
                onLogoClick = onBack
            )

            Stepper(
                step = when (ui.step) {
                    GrnStep.ENTER_PO -> 1
                    GrnStep.SHOW_PO  -> 2
                    GrnStep.REVIEW   -> 3
                    GrnStep.SUMMARY  -> 4
                }
            )

            val steps = if (isFromPickSlip) toUiState.step else ui.step
            when (steps) {
                GrnStep.ENTER_PO -> EnterPoCard(
                    isFromPickSlip = isFromPickSlip,
                    ui = ui,
                    onEnterPo = onEnterPo,
                    onFetchPo = onFetchPo,
                    onBack = onBack,
                    toUiState = toUiState
                )
                GrnStep.SHOW_PO  -> PoAndReceiveCard(
                    ui = ui,
                    onBack = onBack,
                    onUpdateLine = onUpdateLine,
                    onRemoveLine = onRemoveLine,
                    onAddLine = onAddLine,
                    onAddSection = onAddSection,
                    onRemoveSection = onRemoveSection,
                    onUpdateSection = onUpdateSection,
                    onReview = onReview,
                    snackbarHostState = snackbarHostState,
                    dateTimeConverter = dateTimeConverter,
                    isFromPickSlip = isFromPickSlip,
                    toUi = toUiState
                )
                GrnStep.REVIEW   -> ReviewCard(
                    ui = ui,
                    onSubmit = onSubmit,
                    onEditReceive = onEditReceive,
                    onAddAttachments = onAddAttachments
                )
                GrnStep.SUMMARY  -> SummaryCard(ui, onStartOver)
            }
        }
    }
}
