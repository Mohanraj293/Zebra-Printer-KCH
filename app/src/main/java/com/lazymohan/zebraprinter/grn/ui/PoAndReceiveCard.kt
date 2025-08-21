package com.lazymohan.zebraprinter.grn.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lazymohan.zebraprinter.grn.data.PoLineItem
import com.lazymohan.zebraprinter.grn.util.ExtractedItem
import com.lazymohan.zebraprinter.grn.util.bestMatchIndex
import com.lazymohan.zebraprinter.grn.util.similarity

@Composable
fun PoAndReceiveCard(
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
        derivedStateOf {
            val extracted = ui.extractedFromScan
            val used = mutableSetOf<Int>()

            // 1. Try to match slip items against PO lines
            val poMatches = ui.allPoLines.map { line ->
                val safeItem = line.Item.trim().orEmpty()
                val poDesc = (line.Description ?: safeItem).trim()
                val idx = bestMatchIndex(extracted, poDesc, used, threshold = 0.01)
                if (idx != null) {
                    used += idx
                    MatchDetail(
                        slip = extracted[idx],
                        best = line,
                        similarity = similarity(extracted[idx].description, poDesc)
                    )
                } else {
                    // PO line didn’t get any match
                    MatchDetail(
                        slip = ExtractedItem("", 0.0, "", ""),
                        best = line,
                        similarity = 0.0
                    )
                }
            }
            poMatches
        }
    }

    val slipCount = matches.size
    val matchedCount = matches.count { it.similarity != 0.0 }
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
                    Spacer(Modifier.height(8.dp))
                    Text("Loading PO…", color = Color(0xFF6B7280))
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
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { showAddDialog = true },
                        enabled = candidates.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE3F2FD),
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Add Line", fontSize = 12.sp)
                    }
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
                } else if (isScanMode) {
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
                        val msg =
                            if (isScanMode) "No matches found between scanned items and PO lines. Use “Add Line” to include items manually."
                            else "No PO lines found."
                        Text(msg, color = Color(0xFF8B5CF6))
                    }
                    else -> {
                        ui.lines.forEach { ln ->
                            val li = ui.lineInputs.firstOrNull { it.lineNumber == ln.LineNumber }
                            LineCard(
                                ln = ln,
                                li = li,
                                onUpdateLine = onUpdateLine,
                                onRemoveLine = onRemoveLine
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                val hasAtLeastOneValid =
                    ui.lineInputs.any { it.qty > 0.0 && it.lot.isNotBlank() && it.expiry.isNotBlank() }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(52.dp)) {
                        Text("Back")
                    }
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



@Composable
fun LineCard(
    ln: PoLineItem,
    li: LineInput?,
    onUpdateLine: (Int, Double?, String?, String?) -> Unit,
    onRemoveLine: (Int) -> Unit
) {
    var expanded by rememberSaveable(ln.LineNumber) { mutableStateOf(true) }
    var confirmDelete by rememberSaveable("${ln.LineNumber}-del") { mutableStateOf(false) }

    var qtyText by rememberSaveable("${ln.LineNumber}-qty") {
        mutableStateOf(if ((li?.qty ?: 0.0) == 0.0) "" else (li?.qty?.toString() ?: ""))
    }
    var lotText by rememberSaveable("${ln.LineNumber}-lot") { mutableStateOf(li?.lot.orEmpty()) }
    var expText by rememberSaveable("${ln.LineNumber}-exp") { mutableStateOf(li?.expiry.orEmpty()) }

    Spacer(Modifier.height(10.dp))
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFDFEFF),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {

            // --- Header row with expand + delete ---
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.weight(1f).clickable { expanded = !expanded }
                ) {
                    val itemCode = ln.Item.takeIf { it.isNotBlank() } ?: "NA"
                    val title = (ln.Description ?: "").ifBlank { "Item $itemCode" }
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        color = Color(0xFF143A7B),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Chip("Item: $itemCode")
                        Chip("${fmt(ln.Quantity)} ${ln.UOM} ordered")
                    }
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = Color(0xFF334155)
                    )
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Remove line", tint = Color(0xFFB00020))
                }
            }

            // --- Expanded content ---
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                Spacer(Modifier.height(10.dp))

                if (!ln.Description.isNullOrBlank()) ReadFieldInline("Description", ln.Description)
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
                    errorText = qtyText.toDoubleOrNull()?.let { q ->
                        if (q > ln.Quantity) "Cannot exceed ordered qty." else null
                    },
                    enabled = true
                )
                Spacer(Modifier.height(8.dp))
                LabeledText(
                    lotText,
                    { new -> lotText = new; onUpdateLine(ln.LineNumber, null, new, null) },
                    "Lot Number",
                    true
                )
                Spacer(Modifier.height(8.dp))
                LabeledText(
                    expText,
                    { new -> expText = new; onUpdateLine(ln.LineNumber, null, null, new) },
                    "Expiry (YYYY-MM-DD)",
                    true
                )
            }

            // --- Delete confirmation ---
            if (confirmDelete) {
                AlertDialog(
                    onDismissRequest = { confirmDelete = false },
                    icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                    title = { Text("Remove line ${ln.LineNumber}?") },
                    text = { Text("This will remove the line from this receipt. You can add it back using “Add Line”.") },
                    confirmButton = {
                        TextButton(onClick = { confirmDelete = false; onRemoveLine(ln.LineNumber) }) {
                            Text("Remove", color = Color(0xFFB00020))
                        }
                    },
                    dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
                )
            }
        }
    }
}
