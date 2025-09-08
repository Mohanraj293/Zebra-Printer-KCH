package com.lazymohan.zebraprinter.grn.ui.add

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lazymohan.zebraprinter.grn.ui.*
import com.lazymohan.zebraprinter.utils.DateTimeConverter

enum class AddStep { ENTER_PO, SELECT_GRN, SHOW_PO, REVIEW, SUMMARY }

@Composable
fun AddToGrnScreens(
    ui: AddToGrnUiState,
    snackbarHostState: SnackbarHostState,
    dateTimeConverter: DateTimeConverter,
    onEnterPo: (String) -> Unit,
    onFindExisting: () -> Unit,
    onSelectExisting: (ExistingGrn) -> Unit,
    onUpdateLine: (Int, Int, String?, String?) -> Unit,
    onRemoveLine: (Int) -> Unit,
    onAddLine: (Int) -> Unit,
    onAddSection: (Int) -> Unit,
    onRemoveSection: (Int, Int) -> Unit,
    onUpdateSection: (Int, Int, Int, String?, String?) -> Unit,
    onReview: () -> Unit,
    onBackFromReceive: () -> Unit,
    onAddAttachments: (List<Uri>) -> Unit,
    onEditReceive: () -> Unit,
    onSubmit: () -> Unit,
    onStartOver: () -> Unit
) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF0E63FF), Color(0xFF5AA7FF)))

    Scaffold(
        containerColor = Color(0xFFF6F8FF),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {

            Header(
                gradient = gradient,
                title = when (ui.step) {
                    AddStep.ENTER_PO -> "Add to Existing GRN (PO)"
                    AddStep.SELECT_GRN -> "Select Existing GRN"
                    AddStep.SHOW_PO -> "Confirm & Receive"
                    AddStep.REVIEW -> "Review & Submit"
                    AddStep.SUMMARY -> "Success!"
                },
                subtitle = when (ui.step) {
                    AddStep.ENTER_PO -> "Enter PO to find success receipts"
                    AddStep.SELECT_GRN -> "Pick a receipt (SUCCESS) to add to"
                    AddStep.SHOW_PO -> "Enter Qty/Lot/Expiry for lines"
                    AddStep.REVIEW -> "Add to the selected GRN"
                    AddStep.SUMMARY -> "Items added to GRN"
                },
                onLogoClick = onBackFromReceive
            )

            Stepper(
                step = when (ui.step) {
                    AddStep.ENTER_PO -> 1
                    AddStep.SELECT_GRN -> 2
                    AddStep.SHOW_PO -> 3
                    AddStep.REVIEW -> 4
                    AddStep.SUMMARY -> 5
                }
            )

            when (ui.step) {
                AddStep.ENTER_PO -> {
                    EnterPoCard(
                        ui = ui.base,
                        onEnterPo = onEnterPo,
                        onFetchPo = onFindExisting,
                        onBack = onBackFromReceive
                    )
                }

                AddStep.SELECT_GRN -> {
                    SelectExistingGrnCard(
                        loading = ui.loading,
                        error = ui.error,
                        poNumber = ui.base.poNumber,
                        receipts = ui.existing,
                        onRetry = onFindExisting,
                        onPick = onSelectExisting
                    )
                }

                AddStep.SHOW_PO -> {
                    PoAndReceiveCard(
                        ui = ui.base,
                        onBack = onBackFromReceive,
                        onUpdateLine = onUpdateLine,
                        onRemoveLine = onRemoveLine,
                        onAddLine = onAddLine,
                        onAddSection = onAddSection,
                        onRemoveSection = onRemoveSection,
                        onUpdateSection = onUpdateSection,
                        onReview = onReview,
                        dateTimeConverter = dateTimeConverter,
                        snackbarHostState = snackbarHostState
                    )
                }

                AddStep.REVIEW -> {
                    ReviewCard(
                        ui = ui.base,
                        onSubmit = onSubmit,
                        onEditReceive = onEditReceive,
                        onAddAttachments = onAddAttachments)
                }

                AddStep.SUMMARY -> {
                    SummaryCard(ui.base, onStartOver)
                }
            }
        }
    }
}

@Composable
private fun SelectExistingGrnCard(
    loading: Boolean,
    error: String?,
    poNumber: String,
    receipts: List<ExistingGrn>,
    onRetry: () -> Unit,
    onPick: (ExistingGrn) -> Unit
) {
    Card(
        modifier = Modifier.padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Existing Receipts for PO: $poNumber", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            when {
                loading -> {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        CircularProgressIndicator(strokeWidth = 2.dp, color = Color(0xFF2E6BFF))
                        Spacer(Modifier.width(8.dp))
                        Text("Looking up SUCCESS GRNs…", color = Color.Gray)
                    }
                }
                error?.isNotBlank() == true -> {
                    Text(error, color = Color(0xFFB00020))
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onRetry) { Text("Retry") }
                }
                receipts.isEmpty() -> {
                    Text("No SUCCESS receipts found for this PO.", color = Color(0xFF64748B))
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onRetry) { Text("Search again") }
                }
                else -> {
                    Spacer(Modifier.height(4.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(receipts) { r ->
                            ReceiptPickRow(r) { onPick(r) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptPickRow(r: ExistingGrn, onPick: () -> Unit) {
    Surface(
        onClick = onPick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        color = Color(0xFFFDFEFF)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "Receipt #${r.receiptNumber ?: "—"}",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF143A7B)
            )
            Spacer(Modifier.height(4.dp))
            KeyValue("ReceiptHeaderId", r.receiptHeaderId?.toString() ?: "—")
            KeyValue("Vendor", r.vendorName ?: "—")
            KeyValue("Employee", r.employeeName ?: "—")
            KeyValue("Processing", r.processingStatus ?: "—")
        }
    }
}

@Composable private fun KeyValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B7280))
        Text(value, style = MaterialTheme.typography.bodySmall, color = Color(0xFF0F172A))
    }
}
