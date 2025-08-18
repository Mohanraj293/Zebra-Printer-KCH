// app/src/main/java/com/lazymohan/zebraprinter/grn/ui/GrnScreens.kt
package com.lazymohan.zebraprinter.grn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lazymohan.zebraprinter.grn.data.PoLineItem
import java.text.NumberFormat
import java.util.*

@Composable
fun GrnScreens(
    ui: GrnUiState,
    snackbarHostState: SnackbarHostState,
    onEnterPo: (String) -> Unit,
    onFetchPo: () -> Unit,
    onUpdateLine: (Int, Double?, String?, String?) -> Unit,
    onRemoveLine: (Int) -> Unit,
    onAddLine: (Int) -> Unit,
    onEditReceive: () -> Unit,
    onReview: () -> Unit,
    onSubmit: () -> Unit,
    onStartOver: () -> Unit,
    onBack: () -> Unit
) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF0E63FF), Color(0xFF5AA7FF)))
    Scaffold(
        containerColor = Color(0xFFF6F8FF),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            Header(
                gradient,
                when (ui.step) {
                    GrnStep.ENTER_PO -> "Enter PO Number"
                    GrnStep.SHOW_PO  -> "Confirm & Receive"
                    GrnStep.REVIEW   -> "Review & Submit"
                    GrnStep.SUMMARY  -> "Success!"
                },
                when (ui.step) {
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

            when (ui.step) {
                GrnStep.ENTER_PO -> EnterPoCard(ui, onEnterPo, onFetchPo, onBack)
                GrnStep.SHOW_PO  -> PoAndReceiveCard(ui, onBack, onUpdateLine, onRemoveLine, onAddLine, onReview)
                GrnStep.REVIEW   -> ReviewCard(ui, onSubmit, onEditReceive)
                GrnStep.SUMMARY  -> SummaryCard(ui, onStartOver)
            }
        }
    }
}

@Composable
private fun Header(
    gradient: Brush,
    title: String,
    subtitle: String,
    onLogoClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient)
            .padding(top = 28.dp, start = 24.dp, end = 24.dp, bottom = 18.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).clickable { onLogoClick() },
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("KCH", color = Color(0xFF0E63FF), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold))
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.95f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun Stepper(step: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .offset(y = (-10).dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("PO", "Receive", "Review", "Done").forEachIndexed { idx, label ->
            val active = (idx + 1) <= step
            val bg = if (active) Color(0xFFE8F0FF) else Color(0xFFF2F4F7)
            val fg = if (active) Color(0xFF0E63FF) else Color(0xFF64748B)
            Surface(color = bg, shape = RoundedCornerShape(18.dp)) {
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


@Composable
private fun EnterPoCard(
    ui: GrnUiState,
    onEnterPo: (String) -> Unit,
    onFetchPo: () -> Unit,
    onBack: () -> Unit
) {
    Card(
        modifier = Modifier.padding(20.dp).offset(y = (-12).dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            OutlinedTextField(
                value = ui.poNumber,
                onValueChange = onEnterPo,
                label = { Text("PO Number") },
                placeholder = { Text("e.g., KHQ/PO/99387") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(52.dp)) { Text("Back") }
                Button(
                    onClick = onFetchPo,
                    enabled = ui.poNumber.isNotBlank() && !ui.loading,
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                ) { if (ui.loading) CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White) else Text("Next", color = Color.White) }
            }
            if (ui.error != null) { Spacer(Modifier.height(8.dp)); Text(ui.error, color = Color(0xFFB00020)) }
        }
    }
}

@Composable
private fun PoAndReceiveCard(
    ui: GrnUiState,
    onBack: () -> Unit,
    onUpdateLine: (Int, Double?, String?, String?) -> Unit,
    onRemoveLine: (Int) -> Unit,
    onAddLine: (Int) -> Unit,
    onReview: () -> Unit
) {
    val isScanMode = ui.extractedFromScan.isNotEmpty()
    val taken = ui.lineInputs.map { it.lineNumber }.toSet()
    val candidates = ui.allPoLines.filter { it.LineNumber !in taken }

    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.verticalScroll(rememberScrollState())) {

        // --- PO header card ---
        Card(
            modifier = Modifier.padding(horizontal = 20.dp).offset(y = (-12).dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                SectionHeader("Purchase Order Details", badge = "READ-ONLY", badgeColor = Color(0xFFDFF7E6))
                val po = ui.po
                if (po == null) {
                    Spacer(Modifier.height(8.dp)); Text("Loading PO…", color = Color(0xFF6B7280))
                } else {
                    ReadField("PO Number:", po.OrderNumber)
                    ReadField("Vendor:", po.Supplier)
                    ReadField("Site:", po.SupplierSite)
                    ReadField("Business Unit:", po.ProcurementBU)
                    ReadField("Sold To:", po.SoldToLegalEntity)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // --- Lines card (with add + no-match handling) ---
        Card(
            modifier = Modifier.padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    SectionHeader("Lines", badge = "ENTER DETAILS", badgeColor = Color(0xFFE3F2FD))
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = { showAddDialog = true },
                        enabled = candidates.isNotEmpty()
                    ) { Text("Add Line") }
                }

                if (isScanMode) {
                    Text(
                        "Showing only lines matched from scanned slip.",
                        color = Color(0xFF334155),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.height(4.dp))
                }

                when {
                    !ui.linesLoaded -> {
                        Spacer(Modifier.height(8.dp))
                        Text("Loading lines…", color = Color(0xFF6B7280))
                    }
                    ui.lines.isEmpty() -> {
                        Spacer(Modifier.height(8.dp))
                        val msg = if (isScanMode) "No matches found between scanned items and PO lines. Use “Add Line” to include items manually."
                        else "No PO lines found."
                        Text(msg, color = Color(0xFF8B5CF6))
                    }
                    else -> {
                        ui.lines.forEach { ln ->
                            var expanded by rememberSaveable(ln.LineNumber) { mutableStateOf(false) }
                            var confirmDelete by rememberSaveable("${ln.LineNumber}-del") { mutableStateOf(false) }

                            val li = ui.lineInputs.firstOrNull { it.lineNumber == ln.LineNumber }
                            var qtyText by rememberSaveable("${ln.LineNumber}-qty") {
                                mutableStateOf(if ((li?.qty ?: 0.0) == 0.0) "" else (li?.qty?.toString() ?: ""))
                            }
                            var lotText by rememberSaveable("${ln.LineNumber}-lot") { mutableStateOf(li?.lot.orEmpty()) }
                            var expText by rememberSaveable("${ln.LineNumber}-exp") { mutableStateOf(li?.expiry.orEmpty()) }

                            Spacer(Modifier.height(10.dp))
                            Surface(tonalElevation = 2.dp, shadowElevation = 2.dp, shape = RoundedCornerShape(16.dp), color = Color(0xFFFDFEFF), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp)) {

                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.weight(1f).clickable { expanded = !expanded }) {
                                            val itemCode = ln.Item?.takeIf { it.isNotBlank() } ?: "NA"
                                            val title = (ln.Description ?: "").ifBlank { "Item $itemCode" }
                                            Text(title, style = MaterialTheme.typography.titleMedium, color = Color(0xFF143A7B), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Spacer(Modifier.height(2.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Chip("Item: $itemCode"); Chip("${fmt(ln.Quantity)} ${ln.UOM} ordered")
                                            }
                                        }
                                        IconButton(onClick = { expanded = !expanded }) {
                                            Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null, tint = Color(0xFF334155))
                                        }
                                        IconButton(onClick = { confirmDelete = true }) {
                                            Icon(Icons.Outlined.Delete, contentDescription = "Remove line", tint = Color(0xFFB00020))
                                        }
                                    }

                                    if (expanded) {
                                        Spacer(Modifier.height(10.dp)); Divider(); Spacer(Modifier.height(10.dp))

                                        if (!ln.Description.isNullOrBlank()) ReadFieldInline("Description", ln.Description!!)
                                        ReadFieldInline("Line #", ln.LineNumber.toString())
                                        ReadFieldInline("UOM", ln.UOM)

                                        Spacer(Modifier.height(10.dp))

                                        LabeledNumber(
                                            value = qtyText,
                                            onChange = {
                                                qtyText = it
                                                onUpdateLine(ln.LineNumber, it.toDoubleOrNull(), null, null)
                                            },
                                            label = "Quantity (≤ ${fmt(ln.Quantity)})",
                                            errorText = qtyText.toDoubleOrNull()?.let { q -> if (q > ln.Quantity) "Cannot exceed ordered qty." else null },
                                            enabled = true
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        LabeledText(lotText, { new -> lotText = new; onUpdateLine(ln.LineNumber, null, new, null) }, "Lot Number", true)
                                        Spacer(Modifier.height(8.dp))
                                        LabeledText(expText, { new -> expText = new; onUpdateLine(ln.LineNumber, null, null, new) }, "Expiry (YYYY-MM-DD)", true)
                                    }

                                    if (confirmDelete) {
                                        AlertDialog(
                                            onDismissRequest = { confirmDelete = false },
                                            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                                            title = { Text("Remove line ${ln.LineNumber}?") },
                                            text = { Text("This will remove the line from this receipt. You can add it back using “Add Line”.") },
                                            confirmButton = {
                                                TextButton(onClick = { confirmDelete = false; onRemoveLine(ln.LineNumber) }) { Text("Remove", color = Color(0xFFB00020)) }
                                            },
                                            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                val hasAtLeastOneValid =
                    ui.lineInputs.any { it.qty > 0.0 && it.lot.isNotBlank() && it.expiry.isNotBlank() }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(52.dp)) { Text("Back") }
                    Button(
                        onClick = onReview,
                        enabled = hasAtLeastOneValid,
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                    ) { Text("Review & Submit", color = Color.White) }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
    }

    // --- Add Line dialog ---
    if (showAddDialog) {
        AddLineDialog(
            candidates = candidates,
            onPick = { ln -> showAddDialog = false; onAddLine(ln) },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun AddLineDialog(
    candidates: List<PoLineItem>,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selected: Int? by rememberSaveable { mutableStateOf(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Line Item") },
        text = {
            Column {
                if (candidates.isEmpty()) {
                    Text("All PO lines are already included.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 360.dp)) {
                        items(candidates) { ln ->
                            val itemCode = ln.Item?.takeIf { it.isNotBlank() } ?: "NA"
                            Surface(
                                onClick = { selected = ln.LineNumber },
                                shape = RoundedCornerShape(12.dp),
                                tonalElevation = 1.dp
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = selected == ln.LineNumber, onClick = { selected = ln.LineNumber })
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text((ln.Description ?: "Item $itemCode"), fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.height(2.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Chip("Line: ${ln.LineNumber}")
                                            Chip("Item: $itemCode")
                                            Chip("${fmt(ln.Quantity)} ${ln.UOM}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { selected?.let(onPick) }, enabled = selected != null) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}


@Composable
private fun ReviewCard(
    ui: GrnUiState,
    onSubmit: () -> Unit,
    onEditReceive: () -> Unit
) {
    val p = ui.payload ?: return
    val totalQty = p.lines.sumOf { it.Quantity }
    val totalLines = p.lines.size

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Card(
            modifier = Modifier.padding(horizontal = 20.dp).offset(y = (-12).dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(10.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                // Receipt-style heading
                Text("Receipt Preview", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = Color(0xFF143A7B)))
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(12.dp))

                // Header block (like a receipt)
                ReceiptRow("PO Number", p.lines.firstOrNull()?.DocumentNumber ?: "-")
                ReceiptRow("Vendor", p.VendorName)
                ReceiptRow("Site", p.VendorSiteCode)
                ReceiptRow("Business Unit", p.BusinessUnit)
                Spacer(Modifier.height(10.dp))
                Divider(thickness = 1.dp, color = Color(0xFFE7EAF3))
                Spacer(Modifier.height(10.dp))

                Text("Lines to Receive", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF143A7B)))
                Spacer(Modifier.height(6.dp))

                if (p.lines.isEmpty()) {
                    Text("No lines selected.", color = Color(0xFF6B7280))
                } else {
                    p.lines.forEach { ln ->
                        val lot = ln.lotItemLots.firstOrNull()?.LotNumber ?: "-"
                        val exp = ln.lotItemLots.firstOrNull()?.LotExpirationDate ?: "-"
                        Column {
                            // one compact row
                            Text(
                                text = "Line ${ln.DocumentLineNumber}  •  ${ln.ItemNumber}  •  Qty ${fmt(ln.Quantity)} ${ln.UnitOfMeasure}",
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Lot $lot   Exp $exp",
                                color = Color(0xFF475569),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.height(10.dp))
                Divider(thickness = 1.dp, color = Color(0xFFE7EAF3))
                Spacer(Modifier.height(10.dp))

                // Totals
                ReceiptRow("Total Lines", totalLines.toString())
                ReceiptRow("Total Quantity", fmt(totalQty))

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onEditReceive,
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) { Text("Back to Receive") }   // takes user to edit lines
                    Button(
                        onClick = onSubmit,
                        enabled = !ui.loading && p.lines.isNotEmpty(),
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                    ) {
                        if (ui.loading) CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White)
                        else Text("Create GRN in Oracle", color = Color.White)
                    }
                }

                if (ui.error != null) { Spacer(Modifier.height(8.dp)); Text(ui.error, color = Color(0xFFB00020)) }
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), color = Color(0xFF6B7280))
        Text(value, modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun SummaryCard(ui: GrnUiState, onStartOver: () -> Unit) {
    val r = ui.receipt
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF2ECC71), modifier = Modifier.size(96.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text("GRN Successfully Created", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall.copy(color = Color(0xFF0E9F6E), fontWeight = FontWeight.ExtraBold))
        Spacer(Modifier.height(10.dp))
        Card(
            modifier = Modifier.padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                SectionHeader("Transaction Summary")
                ReadField("GRN Number:", r?.ReceiptNumber ?: "-")
                ReadField("Status:", r?.ReturnStatus ?: "-")
                if (ui.lineErrors.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Line-Level Errors", color = Color(0xFFB00020), fontWeight = FontWeight.Bold)
                    ui.lineErrors.forEach { Text("• ${it.ItemDescription ?: "-"}: ${it.ErrorMessage ?: ""}", color = Color(0xFFB00020)) }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onStartOver, modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))) { Text("Start New Receipt", color = Color.White) }
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable private fun SectionHeader(title: String, badge: String? = null, badgeColor: Color = Color(0xFFEFF4FF)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.padding(start = 20.dp, top = 12.dp).size(40.dp).clip(CircleShape).background(Color(0xFFEFF4FF)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Description, contentDescription = null, tint = Color(0xFF2E6BFF))
        }
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleLarge.copy(color = Color(0xFF143A7B), fontWeight = FontWeight.ExtraBold))
        if (badge != null) {
            Spacer(Modifier.width(8.dp))
            Surface(color = badgeColor, shape = RoundedCornerShape(20.dp)) {
                Text(badge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = Color(0xFF495057), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable private fun Chip(text: String) {
    Surface(color = Color(0xFFEFF4FF), shape = RoundedCornerShape(20.dp)) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), color = Color(0xFF2E6BFF), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable private fun ReadField(label: String, value: String) {
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(140.dp), color = Color(0xFF6B7280))
        Text(value, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
    }
}

@Composable private fun ReadFieldInline(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("$label:", modifier = Modifier.width(120.dp), color = Color(0xFF6B7280), style = MaterialTheme.typography.labelMedium)
        Text(value, color = Color(0xFF0F172A), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable private fun LabeledText(value: String, onChange: (String) -> Unit, label: String, enabled: Boolean) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true, enabled = enabled, modifier = Modifier.fillMaxWidth())
}

@Composable private fun LabeledNumber(value: String, onChange: (String) -> Unit, label: String, errorText: String?, enabled: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        label = { Text(label) },
        isError = errorText != null,
        singleLine = true,
        enabled = enabled,
        supportingText = { if (errorText != null) Text(errorText, color = Color(0xFFB00020)) },
        modifier = Modifier.fillMaxWidth()
    )
}

private fun fmt(d: Double): String = NumberFormat.getNumberInstance(Locale.US).format(d)
