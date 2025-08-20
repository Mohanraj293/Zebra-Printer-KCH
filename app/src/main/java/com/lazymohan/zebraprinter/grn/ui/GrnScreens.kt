// app/src/main/java/com/lazymohan/zebraprinter/grn/ui/GrnScreens.kt
package com.lazymohan.zebraprinter.grn.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import kotlin.math.max
import kotlin.math.roundToInt
import com.lazymohan.zebraprinter.grn.util.ExtractedItem as SlipItem

import java.util.Locale


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
fun Header(
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

/* ------------------------- ENTER PO ------------------------- */

@Composable
private fun EnterPoCard(
    ui: GrnUiState,
    onEnterPo: (String) -> Unit,
    onFetchPo: () -> Unit,
    onBack: () -> Unit
) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val hasError = ui.error?.isNotBlank() == true

    Card(
        modifier = Modifier
            .padding(20.dp)
            .offset(y = (-12).dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(Modifier.padding(20.dp)) {

            OutlinedTextField(
                value = ui.poNumber,
                onValueChange = { onEnterPo(it.trim()) },
                label = { Text("PO Number") },
                placeholder = { Text("e.g., KHQ/PO/99387") },
                singleLine = true,
                isError = hasError,
                supportingText = {
                    if (hasError) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = ui.error.orEmpty(),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        Text(
                            "Enter the exact Oracle PO number.",
                            color = Color(0xFF64748B)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            AnimatedVisibility(visible = hasError) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ErrorOutline, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "We couldn’t find that PO",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SmallTipChip("Check typos")
                            SmallTipChip("Remove spaces")
                            SmallTipChip("Use exact slashes")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) { Text("Back") }

                Button(
                    onClick = onFetchPo,
                    enabled = ui.poNumber.isNotBlank() && !ui.loading,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                ) {
                    if (ui.loading) {
                        CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Next", color = Color.White)
                    }
                }
            }
            if (ui.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(ui.error, color = Color(0xFFB00020))
            }

        }
    }
}

@Composable
private fun SmallTipChip(text: String) {
    Surface(
        color = Color.White.copy(alpha = 0.35f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/* --------------------- CONFIRM & RECEIVE --------------------- */

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
    var showMatchDialog by rememberSaveable { mutableStateOf(false) }
    val matches by remember(ui.extractedFromScan, ui.allPoLines) {
        mutableStateOf(computeSlipMatches(ui.extractedFromScan, ui.allPoLines))
    }
    val slipCount = matches.size
    val matchedCount = matches.count { it.matched }
    val unmatchedCount = slipCount - matchedCount

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

                if (isScanMode && slipCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    SlipMatchBar(
                        slipCount = slipCount,
                        matchedCount = matchedCount,
                        hasUnmatched = unmatchedCount > 0,
                        onClick = { showMatchDialog = true }
                    )
                    Spacer(Modifier.height(6.dp))
                } else if (isScanMode && slipCount == 0) {
                    Spacer(Modifier.height(4.dp))
                    InfoBanner("No items detected on the uploaded delivery slip.")
                    Spacer(Modifier.height(6.dp))
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

    // dialogs
    if (showAddDialog) {
        AddLineDialog(
            candidates = candidates,
            onPick = { ln -> showAddDialog = false; onAddLine(ln) },
            onDismiss = { showAddDialog = false }
        )
    }
    if (showMatchDialog) {
        MatchBreakdownDialog(matches = matches, onDismiss = { showMatchDialog = false })
    }
}

/* --------------------------- REVIEW --------------------------- */


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

    val matches by remember(ui.extractedFromScan, ui.allPoLines) {
        mutableStateOf(computeSlipMatches(ui.extractedFromScan, ui.allPoLines))
    }
    val slipCount = matches.size
    val matchedCount = matches.count { it.matched }
    var showMatchDialog by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Card(
            modifier = Modifier.padding(horizontal = 20.dp).offset(y = (-12).dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(10.dp)
        ) {
            Column(Modifier.padding(20.dp)) {

                if (slipCount > 0) {
                    SlipMatchBar(
                        slipCount = slipCount,
                        matchedCount = matchedCount,
                        hasUnmatched = slipCount > matchedCount,
                        compact = true,
                        onClick = { showMatchDialog = true }
                    )
                    Spacer(Modifier.height(12.dp))
                }

                Text("Receipt Preview", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = Color(0xFF143A7B)))
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(12.dp))

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

                ReceiptRow("Total Lines", totalLines.toString())
                ReceiptRow("Total Quantity", fmt(totalQty))

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onEditReceive,
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) { Text("Back to Receive") }
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

                if (ui.error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(ui.error, color = Color(0xFFB00020))
                }
            }
        }
    }

    if (showMatchDialog) {
        MatchBreakdownDialog(matches = matches, onDismiss = { showMatchDialog = false })
    }
}

/* -------------------- Banners, Dialog & helpers -------------------- */

@Composable
private fun SlipMatchBar(
    slipCount: Int,
    matchedCount: Int,
    hasUnmatched: Boolean,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val bg = if (hasUnmatched) Color(0xFFFFF4E5) else Color(0xFFE9F9EF)
    val fg = if (hasUnmatched) Color(0xFF92400E) else Color(0xFF166534)
    Surface(
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        val base = "Delivery slip items: $slipCount • Matched to PO: $matchedCount"
        val tail = if (hasUnmatched) " • Tap to see what didn’t match" else " • All mapped ✓"
        Text(
            text = base + if (compact) "" else tail,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
        )
    }
}

private data class MatchDetail(
    val slip: SlipItem,
    val best: PoLineItem?,
    val similarity: Double
) {
    val matched: Boolean get() = best != null
}

/** Lightweight similarity with item-code boosting. */
private fun computeSlipMatches(
    slipItems: List<SlipItem>,
    poLines: List<PoLineItem>,
    threshold: Double = 0.42
): List<MatchDetail> {
    fun tokens(s: String?): Set<String> =
        (s ?: "")
            .lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 }
            .toSet()

    fun jaccard(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val inter = a.intersect(b).size.toDouble()
        val union = a.union(b).size.toDouble()
        return if (union == 0.0) 0.0 else inter / union
    }

    return slipItems.map { slip ->
        val slipTok = tokens(slip.description)
        var best: PoLineItem? = null
        var bestScore = 0.0
        for (po in poLines) {
            val base = jaccard(slipTok, tokens(po.Description))
            // boost if PO item code appears in slip text
            val code = (po.Item ?: "").trim()
            val boost = if (code.isNotBlank() && (slip.description?.contains(code, ignoreCase = true) == true)) 0.95 else 0.0
            val score = max(base, boost)
            if (score > bestScore) {
                bestScore = score
                best = po
            }
        }
        MatchDetail(slip = slip, best = best.takeIf { bestScore >= threshold }, similarity = bestScore)
    }
}

@Composable
private fun MatchBreakdownDialog(
    matches: List<MatchDetail>,
    onDismiss: () -> Unit
) {
    val matched = matches.count { it.matched }
    val total = matches.size
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delivery Slip Match Details") },
        text = {
            Column {
                Text(
                    "$matched of $total items matched to PO lines.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF334155)
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(matches) { m ->
                        Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 1.dp) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    m.slip.description ?: "(no description)",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                if (m.matched) {
                                    val line = m.best!!
                                    val pct = (m.similarity * 100).roundToInt()
                                    Text(
                                        "Matched → ${line.Item ?: "—"} • ${line.Description ?: ""}",
                                        color = Color(0xFF0F766E)
                                    )
                                    LinearProgressIndicator(
                                        progress = (pct / 100f).coerceIn(0f, 1f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp),
                                        color = Color(0xFF0EA5E9),
                                        trackColor = Color(0xFFE0F2FE)
                                    )
                                    Text("$pct% similarity", style = MaterialTheme.typography.labelSmall, color = Color(0xFF155E75))
                                } else {
                                    Text("No PO match above threshold.", color = Color(0xFF9A3412))
                                    Text(
                                        "Tip: Add the correct PO line manually and enter Qty/Lot/Expiry.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF9A3412)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun InfoBanner(text: String) {
    Surface(color = Color(0xFFEFF4FF), shape = RoundedCornerShape(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Description, contentDescription = null, tint = Color(0xFF2E6BFF))
            Spacer(Modifier.width(8.dp))
            Text(text, color = Color(0xFF143A7B))
        }
    }
}

/* -------------------- Review/Summary helpers -------------------- */

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
        Text(
            "GRN Successfully Created", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall.copy(color = Color(0xFF0E9F6E), fontWeight = FontWeight.ExtraBold)
        )
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
                Button(
                    onClick = onStartOver, modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                ) { Text("Start New Receipt", color = Color.White) }
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable private fun SectionHeader(title: String, badge: String? = null, badgeColor: Color = Color(0xFFEFF4FF)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.padding(start = 20.dp, top = 12.dp).size(40.dp).clip(CircleShape).background(Color(0xFFEFF4FF)),
            contentAlignment = Alignment.Center
        ) {
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
