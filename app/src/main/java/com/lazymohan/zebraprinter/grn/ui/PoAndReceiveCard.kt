package com.lazymohan.zebraprinter.grn.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.lazymohan.zebraprinter.grn.data.PoLineItem
import com.lazymohan.zebraprinter.grn.util.*
import kotlinx.coroutines.launch

@Composable
fun PoAndReceiveCard(
    ui: GrnUiState,
    onBack: () -> Unit,
    onUpdateLine: (Int, Double?, String?, String?) -> Unit,
    onRemoveLine: (Int) -> Unit,
    onAddLine: (Int) -> Unit,
    onReview: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val isScanMode = ui.extractedFromScan.isNotEmpty()
    val taken = ui.lineInputs.map { it.lineNumber }.toSet()
    val candidates = ui.allPoLines.filter { it.LineNumber !in taken }

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showMatchDialog by rememberSaveable { mutableStateOf(false) }
    var scannedText by rememberSaveable { mutableStateOf<String?>(null) }

    // Verified lines & last scanned GTIN
    var verifiedLines by rememberSaveable { mutableStateOf(setOf<Int>()) }
    var lastScannedGtin by rememberSaveable { mutableStateOf<String?>(null) }

    val matches by remember(ui.extractedFromScan, ui.allPoLines) {
        derivedStateOf {
            val extracted = ui.extractedFromScan
            val used = mutableSetOf<Int>()
            ui.allPoLines.map { line ->
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
                    MatchDetail(
                        slip = ExtractedItem("", 0.0, "", ""),
                        best = line,
                        similarity = 0.0
                    )
                }
            }
        }
    }

    val slipCount = matches.size
    val matchedCount = matches.count { it.similarity != 0.0 }
    val unmatchedCount = slipCount - matchedCount

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun isLineVerified(lnNumber: Int) = lnNumber in verifiedLines

    // All selected lines must be verified & details valid
    val allowReview by remember(ui.lineInputs, ui.lines, verifiedLines) {
        derivedStateOf {
            if (ui.lineInputs.isEmpty()) return@derivedStateOf false
            ui.lineInputs.all { li ->
                val ln = ui.lines.firstOrNull { it.LineNumber == li.lineNumber } ?: return@all false
                val qtyOk = li.qty > 0.0 && li.qty <= ln.Quantity
                val fieldsOk = li.lot.isNotBlank() && li.expiry.isNotBlank()
                val verifiedOk = isLineVerified(li.lineNumber)
                qtyOk && fieldsOk && verifiedOk
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.verticalScroll(rememberScrollState())) {

            // --- Purchase Order Details Card ---
            Card(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Purchase Order Details",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 19.sp,
                                color = Color(0xFF143A7B)
                            ),
                            modifier = Modifier.weight(0.7f)
                        )
                        Spacer(Modifier.weight(0.05f))
                        Tag(
                            "READ-ONLY",
                            bg = Color(0xFFDFF7E6),
                            textColor = Color(0xFF14532D)
                        )
                    }
                    Spacer(Modifier.height(10.dp))

                    val po = ui.po
                    if (po == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF2E6BFF)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Loading PO…", color = Color.Gray, fontSize = 13.sp)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row {
                                ReadFieldCompact("PO Number", po.OrderNumber, Modifier.weight(1f))
                                ReadFieldCompact("Vendor", po.Supplier, Modifier.weight(1f))
                            }
                            Row {
                                ReadFieldCompact("Site", po.SupplierSite, Modifier.weight(1f))
                                ReadFieldCompact("Business Unit", po.ProcurementBU, Modifier.weight(1f))
                            }
                            Row {
                                ReadFieldCompact("Sold To", po.SoldToLegalEntity, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // --- Lines Card ---
            Card(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                "Lines",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 19.sp,
                                    color = Color(0xFF143A7B)
                                )
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Tag("ENTER DETAILS", bg = Color(0xFFE3F2FD), textColor = Color(0xFF1E3A8A))
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = { showAddDialog = true },
                            enabled = candidates.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEFF4FF),
                                contentColor = Color(0xFF1E3A8A)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) { Text("➕ Add Item", fontSize = 13.sp) }
                    }

                    if (isScanMode && slipCount > 0) {
                        Spacer(Modifier.height(6.dp))
                        SlipMatchBar(
                            slipCount = slipCount,
                            matchedCount = matchedCount,
                            hasUnmatched = unmatchedCount > 0,
                            onClick = { showMatchDialog = true }
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    when {
                        !ui.linesLoaded -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF2E6BFF)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Loading lines…", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                        ui.lines.isEmpty() -> {
                            val msg = if (isScanMode)
                                "No matches found between scanned items and PO lines. Use “Add Line” to include items manually."
                            else "No PO lines found."
                            Text(msg, color = Color(0xFF8B5CF6))
                        }
                        else -> {
                            ui.lines.forEach { ln ->
                                val li = ui.lineInputs.firstOrNull { it.lineNumber == ln.LineNumber }
                                LineCard(
                                    ln = ln,
                                    li = li,
                                    isVerified = isLineVerified(ln.LineNumber),
                                    onUpdateLine = onUpdateLine,
                                    onRemoveLine = onRemoveLine,
                                    snackbarHostState = snackbarHostState
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // --- Back & Review inside scroll ---
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
                            onClick = onReview,
                            enabled = allowReview,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .alpha(if (allowReview) 1f else 0.35f), // dim until ready
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                        ) { Text("Review & Submit", color = Color.White) }
                    }
                }
            }
        }

        // --- Floating GS1/GTIN QR Scanner Button ---
        FloatingActionButton(
            onClick = {
                val options = GmsBarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .enableAutoZoom()
                    .build()
                val scanner = GmsBarcodeScanning.getClient(context, options)
                scanner.startScan()
                    .addOnSuccessListener { barcode ->
                        val raw = barcode.rawValue.orEmpty()

                        // 1) Extract GTIN from raw using util
                        val gt = extractGtinFromRaw(raw)
                        if (gt == null) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Couldn't find a valid GTIN in scan.")
                            }
                            return@addOnSuccessListener
                        }

                        scannedText = raw
                        lastScannedGtin = gt

                        // 2) Mark any matching PO lines as verified
                        val matched = ui.allPoLines
                            .filter { it.GTIN?.replace(Regex("\\s+"), "") == gt }
                            .map { it.LineNumber }
                            .toSet()

                        verifiedLines = verifiedLines + matched

                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (matched.isEmpty())
                                    "GTIN $gt scanned. No PO line GTIN matched."
                                else
                                    "GTIN $gt verified for lines: ${matched.sorted().joinToString(", ")}"
                            )
                        }
                    }
                    .addOnFailureListener {
                        scope.launch { snackbarHostState.showSnackbar("Scan cancelled.") }
                    }
            },
            containerColor = Color(0xFF2E6BFF),
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
        ) { Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan GS1/GTIN QR") }
    }

    if (showAddDialog) {
        AddLineDialog(
            candidates = candidates,
            onPick = { lnNumber -> showAddDialog = false; onAddLine(lnNumber) },
            onDismiss = { showAddDialog = false }
        )
    }
    if (showMatchDialog) {
        MatchBreakdownDialog(matches = matches, onDismiss = { showMatchDialog = false })
    }
}

// --- Small reusable Tag ---
@Composable
fun Tag(text: String, bg: Color, textColor: Color) {
    Surface(color = bg, shape = RoundedCornerShape(8.dp), tonalElevation = 0.dp) {
        Text(text, color = textColor, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
fun ReadFieldCompact(label: String, value: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, fontSize = 11.sp, color = Color(0xFF6B7280))
        Text(value.orEmpty(), fontSize = 13.sp, color = Color(0xFF111827))
    }
}

// --- LineCard with expand + print ---
@Composable
fun LineCard(
    ln: PoLineItem,
    li: LineInput?,
    isVerified: Boolean,
    onUpdateLine: (Int, Double?, String?, String?) -> Unit,
    onRemoveLine: (Int) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var confirmDelete by rememberSaveable("${ln.LineNumber}-del") { mutableStateOf(false) }
    var expanded by rememberSaveable("${ln.LineNumber}-expanded") { mutableStateOf(false) }

    var qtyText by rememberSaveable("${ln.LineNumber}-qty") {
        mutableStateOf(if ((li?.qty ?: 0.0) == 0.0) "" else (li?.qty?.toString() ?: ""))
    }
    var lotText by rememberSaveable("${ln.LineNumber}-lot") { mutableStateOf(li?.lot.orEmpty()) }
    var expText by rememberSaveable("${ln.LineNumber}-exp") { mutableStateOf(li?.expiry.orEmpty()) }

    val qtyNum = qtyText.toDoubleOrNull()
    val detailsOk = qtyNum != null && qtyNum > 0.0 && qtyNum <= ln.Quantity &&
            lotText.isNotBlank() && expText.isNotBlank()

    val scope = rememberCoroutineScope()

    Spacer(Modifier.height(10.dp))
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFDFEFF),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(Modifier.padding(14.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    val itemCode = ln.Item.takeIf { it.isNotBlank() } ?: "NA"
                    val title = (ln.Description ?: "").ifBlank { "Item $itemCode" }

                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp),
                        color = Color(0xFF143A7B),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Chip("Item: $itemCode")
                        Chip("${fmt(ln.Quantity)} . ${ln.UOM}")
                    }

                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isVerified) {
                            Chip("Verified", bg = Color(0xFFE8F5E9), textColor = Color(0xFF2E7D32))
                        } else {
                            Chip("Not Verified", bg = Color(0xFFFFEBEE), textColor = Color(0xFFD32F2F))
                        }

                        if (detailsOk) {
                            Chip("Details Filled", bg = Color(0xFFE8F5E9), textColor = Color(0xFF2E7D32))
                        } else {
                            Chip("Details Required", bg = Color(0xFFFFF7ED), textColor = Color(0xFF9A3412))
                        }

                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = {
                            scope.launch { snackbarHostState.showSnackbar("Will go to printing… page") }
                        }) { Icon(Icons.Filled.Print, contentDescription = "Print", tint = Color(0xFF334155)) }

                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Remove line", tint = Color(0xFFB00020))
                        }
                    }
                }
            }

            if (expanded) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))

                if (!ln.Description.isNullOrBlank()) ReadFieldInline("Description", ln.Description)
                ReadFieldInline("Line #", ln.LineNumber.toString())
                ReadFieldInline("UOM", ln.UOM)
                ReadFieldInline("GTIN", ln.GTIN?.takeIf { it.isNotBlank() } ?: "—")

                Spacer(Modifier.height(10.dp))

                LabeledNumber(
                    value = qtyText,
                    onChange = {
                        qtyText = it
                        onUpdateLine(ln.LineNumber, it.toDoubleOrNull(), null, null)
                    },
                    label = "Quantity (≤ ${fmt(ln.Quantity)})",
                    errorText = qtyText.toDoubleOrNull()?.let { q ->
                        when {
                            q <= 0.0 -> "Quantity must be > 0"
                            q > ln.Quantity -> "Cannot exceed ordered qty."
                            else -> null
                        }
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

// --- Chip helper ---
@Composable
fun Chip(text: String, bg: Color = Color(0xFFEFF6FF), textColor: Color = Color(0xFF143A7B)) {
    Surface(color = bg, shape = RoundedCornerShape(50), tonalElevation = 0.dp) {
        Text(
            text,
            color = textColor,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
