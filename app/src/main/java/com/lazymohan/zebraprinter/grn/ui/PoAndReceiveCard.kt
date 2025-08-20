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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lazymohan.zebraprinter.grn.data.PoLineItem
import com.lazymohan.zebraprinter.grn.util.toIsoYmd

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

    // Track already added lines
    val takenByInputs = ui.lineInputs.map { it.lineNumber }.toSet()
    val takenByVisible = ui.lines.map { it.LineNumber }.toSet()
    val taken = takenByInputs + takenByVisible

    val candidates = ui.allPoLines.filter { it.LineNumber !in taken }

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showMatchDialog by rememberSaveable { mutableStateOf(false) }

    // slip matches
    val matches by remember(ui.extractedFromScan, ui.allPoLines) {
        derivedStateOf { computeSlipMatches(ui.extractedFromScan, ui.allPoLines) }
    }
    val slipCount = matches.size
    val matchedCount = matches.count { it.matched }
    val unmatchedCount = slipCount - matchedCount

    // Prefill best slip data
    val prefillByLine: Map<Int, com.lazymohan.zebraprinter.grn.util.ExtractedItem> =
        remember(matches) {
            matches
                .filter { it.matched && it.best?.LineNumber != null }
                .groupBy { it.best!!.LineNumber }
                .mapValues { (_, list) -> list.maxBy { it.similarity }.slip }
        }

    // decide what to render
    val matchedLineNos by remember(matches) {
        derivedStateOf { matches.mapNotNull { it.best?.LineNumber }.toSet() }
    }
    val manualLineNos = remember(ui.lines) { ui.lines.map { it.LineNumber }.toSet() }
    val renderSet = remember(matchedLineNos, manualLineNos) { matchedLineNos + manualLineNos }

    val linesToRender = remember(isScanMode, renderSet, ui.allPoLines, ui.lines) {
        if (isScanMode) ui.allPoLines.filter { it.LineNumber in renderSet } else ui.lines
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {

        /* --- PO Header --- */
        Card(
            modifier = Modifier.padding(horizontal = 20.dp).offset(y = (-12).dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                SectionHeader(
                    "Purchase Order Details",
                    badge = "READ-ONLY",
                    badgeColor = Color(0xFFDFF7E6)
                )
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

        /* --- PO Lines --- */
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = { showAddDialog = true },
                        enabled = candidates.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE3F2FD),
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) { Text("Add Line", fontSize = 12.sp) }
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
                        "Showing matched lines plus any you add manually.",
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
                    linesToRender.isEmpty() -> {
                        Spacer(Modifier.height(8.dp))
                        val msg = if (isScanMode)
                            "No matches found between scanned items and PO lines. Use “Add Line” to include items manually."
                        else
                            "No PO lines found."
                        Text(msg, color = Color(0xFF8B5CF6))
                    }
                    else -> {
                        linesToRender.forEach { ln ->
                            LineCard(
                                line = ln,
                                ui = ui,
                                slip = prefillByLine[ln.LineNumber],
                                onUpdateLine = onUpdateLine,
                                onRemoveLine = onRemoveLine
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                val hasAtLeastOneValid =
                    ui.lineInputs.any { it.qty > 0.0 && it.lot.isNotBlank() && it.expiry.isNotBlank() }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) { Text("Back") }
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
private fun LineCard(
    line: PoLineItem,
    ui: GrnUiState,
    slip: com.lazymohan.zebraprinter.grn.util.ExtractedItem?,
    onUpdateLine: (Int, Double?, String?, String?) -> Unit,
    onRemoveLine: (Int) -> Unit
) {
    var expanded by rememberSaveable(line.LineNumber) { mutableStateOf(true) }
    var confirmDelete by rememberSaveable("${line.LineNumber}-del") { mutableStateOf(false) }

    val li = ui.lineInputs.firstOrNull { it.lineNumber == line.LineNumber }

    val prefillQty = (slip?.qtyDelivered ?: 0.0).coerceAtMost(line.Quantity)
    val prefillLot = slip?.batchNo.orEmpty()
    val prefillExp = slip?.expiryDate?.let { toIsoYmd(it) }.orEmpty()

    var qtyText by rememberSaveable("${line.LineNumber}-qty") {
        mutableStateOf(
            when {
                (li?.qty ?: 0.0) > 0.0 -> li!!.qty.toString()
                prefillQty > 0.0 -> prefillQty.toString()
                else -> ""
            }
        )
    }
    var lotText by rememberSaveable("${line.LineNumber}-lot") {
        mutableStateOf(
            when {
                !li?.lot.isNullOrBlank() -> li.lot
                prefillLot.isNotBlank() -> prefillLot
                else -> ""
            }
        )
    }
    var expText by rememberSaveable("${line.LineNumber}-exp") {
        mutableStateOf(
            when {
                !li?.expiry.isNullOrBlank() -> toIsoYmd(li.expiry)
                prefillExp.isNotBlank() -> prefillExp
                else -> ""
            }
        )
    }

    // prefill injection
    LaunchedEffect(line.LineNumber, slip) {
        val needQty = (li?.qty ?: 0.0) <= 0.0 && prefillQty > 0.0
        val needLot = li?.lot.isNullOrBlank() && prefillLot.isNotBlank()
        val needExp = li?.expiry.isNullOrBlank() && prefillExp.isNotBlank()
        if (needQty || needLot || needExp) {
            onUpdateLine(
                line.LineNumber,
                if (needQty) prefillQty else null,
                if (needLot) prefillLot else null,
                if (needExp) prefillExp else null
            )
            if (needQty) qtyText = prefillQty.toString()
            if (needLot) lotText = prefillLot
            if (needExp) expText = prefillExp
        }
    }

    Spacer(Modifier.height(10.dp))
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFDFEFF),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f).clickable { expanded = !expanded }) {
                    val itemCode = line.Item.takeIf { it.isNotBlank() } ?: "NA"
                    val title = (line.Description ?: "").ifBlank { "Item $itemCode" }
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        color = Color(0xFF143A7B),
                        maxLines = 2
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Chip("Item: $itemCode")
                        Chip("${fmt(line.Quantity)} ${line.UOM} ordered")
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

            if (expanded) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                Spacer(Modifier.height(10.dp))

                if (!line.Description.isNullOrBlank()) ReadFieldInline("Description", line.Description)
                ReadFieldInline("Line #", line.LineNumber.toString())
                ReadFieldInline("UOM", line.UOM)

                Spacer(Modifier.height(10.dp))

                LabeledNumber(
                    value = qtyText,
                    onChange = {
                        qtyText = it
                        onUpdateLine(line.LineNumber, it.toDoubleOrNull(), null, null)
                    },
                    label = "Quantity (≤ ${fmt(line.Quantity)})",
                    errorText = qtyText.toDoubleOrNull()
                        ?.let { q -> if (q > line.Quantity) "Cannot exceed ordered qty." else null },
                    enabled = true
                )
                Spacer(Modifier.height(8.dp))
                LabeledText(
                    value = lotText,
                    onChange = { new -> lotText = new; onUpdateLine(line.LineNumber, null, new, null) },
                    label = "Lot Number",
                    enabled = true
                )
                Spacer(Modifier.height(8.dp))
                LabeledText(
                    value = expText,
                    onChange = { new ->
                        val normalized = toIsoYmd(new)
                        expText = normalized
                        onUpdateLine(line.LineNumber, null, null, normalized)
                    },
                    label = "Expiry (YYYY-MM-DD)",
                    enabled = true
                )
            }

            if (confirmDelete) {
                AlertDialog(
                    onDismissRequest = { confirmDelete = false },
                    icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                    title = { Text("Remove line ${line.LineNumber}?") },
                    text = { Text("This will remove the line from this receipt. You can add it back using “Add Line”.") },
                    confirmButton = {
                        TextButton(onClick = { confirmDelete = false; onRemoveLine(line.LineNumber) }) {
                            Text("Remove", color = Color(0xFFB00020))
                        }
                    },
                    dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
                )
            }
        }
    }
}
