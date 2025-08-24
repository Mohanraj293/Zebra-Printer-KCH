@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.lazymohan.zebraprinter.grn.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.lazymohan.zebraprinter.ZPLPrinterActivity
import com.lazymohan.zebraprinter.grn.data.PoLineItem
import com.lazymohan.zebraprinter.grn.util.ExtractedItem
import com.lazymohan.zebraprinter.grn.util.bestMatchIndex
import com.lazymohan.zebraprinter.grn.util.extractGtinFromRaw
import com.lazymohan.zebraprinter.grn.util.similarity
import com.lazymohan.zebraprinter.product.data.Lots
import com.lazymohan.zebraprinter.utils.DateTimeConverter
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Assumes LineInput has:
 *   data class LineSection(val section: Int, val qty: Double, val lot: String, val expiry: String)
 *   data class LineInput(..., val sections: List<LineSection>, ...)
 *
 * New behavior:
 * - Tapping the link icon on any section opens a picker listing ui.extractedFromScan.
 * - Selecting an item fills that section's qty, lot, expiry.
 */

@Composable
fun PoAndReceiveCard(
    ui: GrnUiState,
    onBack: () -> Unit,

    // legacy quick edit for Section #1 (kept)
    onUpdateLine: (Int, Double?, String?, String?) -> Unit,
    onRemoveLine: (Int) -> Unit,
    onAddLine: (Int) -> Unit,

    // NEW sectioned edit
    onAddSection: (Int) -> Unit,                            // lineNumber
    onRemoveSection: (Int, Int) -> Unit,                    // lineNumber, section#
    onUpdateSection: (Int, Int, Double?, String?, String?) -> Unit, // lineNumber, section#, qty, lot, expiry

    onReview: () -> Unit,
    dateTimeConverter: DateTimeConverter,
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

    // Section #1 must be valid & line verified to allow review
    fun section1Valid(li: LineInput, ln: PoLineItem): Boolean {
        val s1 = li.sections.firstOrNull { it.section == 1 } ?: return false
        return s1.qty > 0.0 && s1.qty <= ln.Quantity && s1.lot.isNotBlank() && s1.expiry.isNotBlank()
    }

    val allowReview by remember(ui.lineInputs, ui.lines, verifiedLines) {
        derivedStateOf {
            if (ui.lineInputs.isEmpty()) return@derivedStateOf false
            ui.lineInputs.all { li ->
                val ln = ui.lines.firstOrNull { it.LineNumber == li.lineNumber } ?: return@all false
                section1Valid(li, ln) && isLineVerified(li.lineNumber)
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
                                "No matches found between scanned items and PO lines. Use “Add Item” to include items manually."
                            else "No PO lines found."
                            Text(msg, color = Color(0xFF8B5CF6))
                        }
                        else -> {
                            ui.lines.forEach { ln ->
                                val li = ui.lineInputs.firstOrNull { it.lineNumber == ln.LineNumber } ?: return@forEach
                                val detailsRequired = !section1Valid(li, ln)
                                val verifiedOk = isLineVerified(ln.LineNumber)

                                SectionedLineCard(
                                    context = context,
                                    ln = ln,
                                    li = li,
                                    availableItems = ui.extractedFromScan,
                                    isVerified = verifiedOk,
                                    detailsRequired = detailsRequired,
                                    onUpdateLine = onUpdateLine,
                                    onUpdateSection = onUpdateSection,
                                    onAddSection = onAddSection,
                                    onRemoveSection = onRemoveSection,
                                    onRemoveLine = onRemoveLine,
                                    dateTimeConverter = dateTimeConverter,
                                    snackbarHostState = snackbarHostState
                                )

                                Spacer(Modifier.height(10.dp))
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
                                .alpha(if (allowReview) 1f else 0.35f),
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
                        val gt = extractGtinFromRaw(raw)
                        if (gt == null) {
                            scope.launch { snackbarHostState.showSnackbar("Couldn't find a valid GTIN in scan.") }
                            return@addOnSuccessListener
                        }
                        scannedText = raw
                        lastScannedGtin = gt

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
                .align(BottomEnd)
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

/* ===================== Sectioned Line Card ===================== */

@Composable
private fun SectionedLineCard(
    context: Context,
    ln: PoLineItem,
    li: LineInput,
    availableItems: List<ExtractedItem>,
    isVerified: Boolean,
    detailsRequired: Boolean,
    onUpdateLine: (Int, Double?, String?, String?) -> Unit, // quick Section #1 edit (kept)
    onUpdateSection: (Int, Int, Double?, String?, String?) -> Unit,
    onAddSection: (Int) -> Unit,
    onRemoveSection: (Int, Int) -> Unit,
    onRemoveLine: (Int) -> Unit,
    dateTimeConverter: DateTimeConverter,
    snackbarHostState: SnackbarHostState
) {
    var confirmDelete by rememberSaveable("${ln.LineNumber}-del") { mutableStateOf(false) }
    var expanded by rememberSaveable("${ln.LineNumber}-expanded") { mutableStateOf(true) }

    // which section is currently "linking" to a delivery item (null = closed)
    var linkingSection by rememberSaveable("${ln.LineNumber}-linkSection") { mutableStateOf<Int?>(null) }

    val s1 = li.sections.firstOrNull { it.section == 1 }
    val s1Qty = s1?.qty?.takeIf { it > 0 }?.toString().orEmpty()
    val s1Lot = s1?.lot.orEmpty()
    val s1Exp = s1?.expiry.orEmpty()

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

            // --- Title (Full width) ---
            val itemCode = ln.Item.takeIf { it.isNotBlank() } ?: "NA"
            val title = (ln.Description ?: "").ifBlank { "Item $itemCode" }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp),
                color = Color(0xFF143A7B),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // ======= Chips + Actions in ONE parent row =======
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: chips group → 70%
                FlowRow(
                    modifier = Modifier
                        .weight(0.7f)   // 70% width
                        .padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Status chips
                    if (isVerified) {
                        Chip("Verified", bg = Color(0xFFE8F5E9), textColor = Color(0xFF2E7D32))
                    } else {
                        Chip("Not Verified", bg = Color(0xFFFFEBEE), textColor = Color(0xFFD32F2F))
                    }
                    if (detailsRequired) {
                        Chip("Details Required", bg = Color(0xFFFFF7ED), textColor = Color(0xFF9A3412))
                    } else {
                        Chip("Details Filled", bg = Color(0xFFE8F5E9), textColor = Color(0xFF2E7D32))
                    }

                    // Meta chips
                    Chip("Item: $itemCode")
                    Chip("${fmt(ln.Quantity)} • ${ln.UOM}")

                }

                // Right: actions → 30%
                Row(
                    modifier = Modifier.weight(0.3f),   // 30% width
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = {
                            context.startActivity(
                                ZPLPrinterActivity.getCallingIntent(
                                    context = context,
                                    gtinNumber = ln.GTIN ?: "",
                                    product = Lots(
                                        lotNumber = s1Lot,
                                        itemDescription = ln.Description ?: "",
                                        itemNumber = ln.Item,
                                        statusCode = "Active",
                                        originationDate = null,
                                        expirationDate = dateTimeConverter.convertStringToDate(s1Exp),
                                        inventoryItemId = 0L
                                    ),
                                    qty = s1Qty
                                )
                            )
                        }
                    ) {
                        Icon(
                            Icons.Filled.Print,
                            contentDescription = "Print",
                            tint = Color(0xFF334155)
                        )
                    }

                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Remove line",
                            tint = Color(0xFFB00020)
                        )
                    }
                }
            }

            // ======= end combined row =======

            if (expanded) {
                // --- restored "item details" block (like old code) ---
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))

                if (!ln.Description.isNullOrBlank()) ReadFieldInline("Description", ln.Description!!)
                ReadFieldInline("Line #", ln.LineNumber.toString())
                ReadFieldInline("UOM", ln.UOM)
                ReadFieldInline("GTIN", ln.GTIN?.takeIf { it.isNotBlank() } ?: "—")

                Spacer(Modifier.height(12.dp))

                // ---------- Section #1 ----------
                SectionBox(
                    title = "Section #1",
                    onLink = { linkingSection = 1 },
                    onDelete = { onRemoveSection(ln.LineNumber, 1) }
                ) {
                    QtyField(
                        value = s1Qty,
                        onChange = { onUpdateLine(ln.LineNumber, it.toDoubleOrNull(), null, null) },
                        max = ln.Quantity
                    )
                    Spacer(Modifier.height(8.dp))
                    LinedTextField(
                        value = s1Lot,
                        onChange = { onUpdateLine(ln.LineNumber, null, it, null) },
                        placeholder = "Lot Number"
                    )
                    Spacer(Modifier.height(8.dp))
                    ExpiryDateField(
                        value = s1Exp,
                        onDatePicked = { iso -> onUpdateLine(ln.LineNumber, null, null, iso) }
                    )
                }

                // ---------- Sections > 1 ----------
                li.sections
                    .filter { it.section > 1 }
                    .sortedBy { it.section }
                    .forEach { sec ->
                        Spacer(Modifier.height(10.dp))
                        SectionBox(
                            title = "Section #${sec.section}",
                            onLink = { linkingSection = sec.section },
                            onDelete = { onRemoveSection(ln.LineNumber, sec.section) }
                        ) {
                            QtyField(
                                value = sec.qty.takeIf { it > 0 }?.toString().orEmpty(),
                                onChange = { onUpdateSection(ln.LineNumber, sec.section, it.toDoubleOrNull(), null, null) },
                                max = ln.Quantity
                            )
                            Spacer(Modifier.height(8.dp))
                            LinedTextField(
                                value = sec.lot,
                                onChange = { onUpdateSection(ln.LineNumber, sec.section, null, it, null) },
                                placeholder = "Lot Number"
                            )
                            Spacer(Modifier.height(8.dp))
                            ExpiryDateField(
                                value = sec.expiry,
                                onDatePicked = { iso -> onUpdateSection(ln.LineNumber, sec.section, null, null, iso) }
                            )
                        }
                    }

                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = { onAddSection(ln.LineNumber) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE8F0FF))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) { Text("+ Add Section", color = Color(0xFF1D4ED8), fontWeight = FontWeight.SemiBold) }
            }

            if (confirmDelete) {
                AlertDialog(
                    onDismissRequest = { confirmDelete = false },
                    icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                    title = { Text("Remove line ${ln.LineNumber}?") },
                    text = { Text("This will remove the line from this receipt. You can add it back using “Add Item”.") },
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

    /* ---- Link dialog (bottom sheet) ---- */
    val linking = linkingSection
    if (linking != null) {
        LinkDeliveryBottomSheet(
            line = ln,
            sectionNumber = linking,
            items = availableItems,
            onPick = { ex ->
                val qty = ex.qtyDelivered
                val lot = ex.batchNo
                val exp = ex.expiryDate
                if (linking == 1) {
                    onUpdateLine(ln.LineNumber, qty, lot, exp)
                } else {
                    onUpdateSection(ln.LineNumber, linking, qty, lot, exp)
                }
            },
            onDismiss = { linkingSection = null }
        )
    }
}

/* ===================== Link Picker ===================== */

@Composable
private fun LinkDeliveryBottomSheet(
    line: PoLineItem,
    sectionNumber: Int,
    items: List<ExtractedItem>,
    onPick: (ExtractedItem) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by rememberSaveable { mutableStateOf("") }

    // sort by similarity to PO description if any
    val poText = (line.Description ?: line.Item).lowercase()
    val filtered = remember(items, query) {
        items
            .filter {
                if (query.isBlank()) true
                else {
                    val q = query.lowercase()
                    it.description.lowercase().contains(q) ||
                            it.batchNo.lowercase().contains(q) ||
                            it.expiryDate.lowercase().contains(q)
                }
            }
            .sortedByDescending { similarity(it.description, poText) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                "Link delivery item → Section #$sectionNumber",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(8.dp))
            SearchField(query) { query = it }
            Spacer(Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                Text("No matching delivery items.", color = Color(0xFF64748B))
                Spacer(Modifier.height(16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered) { ex ->
                        DeliveryItemRow(ex) {
                            onPick(ex)
                            onDismiss()
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Close")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text("Search description / lot / expiry") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun DeliveryItemRow(ex: ExtractedItem, onSelect: () -> Unit) {
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, DividerDefaults.color.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(ex.description.ifBlank { "—" }, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Qty: ${fmt(ex.qtyDelivered)}   Lot: ${ex.batchNo.ifBlank { "—" }}   Exp: ${ex.expiryDate.ifBlank { "—" }}",
                color = Color(0xFF475569),
                fontSize = 13.sp
            )
        }
    }
}

/* ===================== UI bits reused ===================== */

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

@Composable
fun Chip(text: String, bg: Color = Color(0xFFEFF6FF), textColor: Color = Color(0xFF143A7B)) {
    Surface(color = bg, shape = RoundedCornerShape(50), tonalElevation = 0.dp) {
        Text(text, color = textColor, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
private fun SectionBox(
    title: String,
    onLink: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFF),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, DividerDefaults.color.copy(alpha = 0.35f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onLink, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Outlined.Link, contentDescription = "Link", tint = Color(0xFF64748B))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete section", tint = Color(0xFFB00020))
                }
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun QtyField(
    value: String,
    onChange: (String) -> Unit,
    max: Double
) {
    OutlinedTextField(
        value = value,
        onValueChange = { new -> if (new.isEmpty() || new.matches(Regex("""\d*\.?\d*"""))) onChange(new) },
        placeholder = { Text("Quantity") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .width(32.dp)
                    .fillMaxHeight()
            ) {
                IconButton(
                    onClick = {
                        val cur = value.toDoubleOrNull() ?: 0.0
                        val next = (cur + 1.0).coerceAtMost(max)
                        onChange(trimZero(next))
                    },
                    modifier = Modifier.size(20.dp)
                ) { Icon(Icons.Filled.ArrowDropUp, null) }
                IconButton(
                    onClick = {
                        val cur = value.toDoubleOrNull() ?: 0.0
                        val next = (cur - 1.0).coerceAtLeast(0.0)
                        onChange(trimZero(next))
                    },
                    modifier = Modifier.size(20.dp)
                ) { Icon(Icons.Filled.ArrowDropDown, null) }
            }
        }
    )
}

@Composable
private fun LinedTextField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ExpiryDateField(
    value: String,
    onDatePicked: (String) -> Unit
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        placeholder = { Text("Expiry (YYYY-MM-DD)") },
        singleLine = true,
        readOnly = true,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showPicker = true }
    )

    if (showPicker) {
        val today = LocalDate.now()
        val initialMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = state.selectedDateMillis
                        if (millis != null) onDatePicked(millisToIso(millis))
                        showPicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = state, showModeToggle = false)
        }
    }
}

/* ===================== small utils ===================== */

private fun fmt(d: Double): String = if (d % 1.0 == 0.0) d.toInt().toString() else "%.2f".format(d)
private fun trimZero(d: Double): String = if (d % 1.0 == 0.0) d.toInt().toString() else "%.2f".format(d)
private fun millisToIso(millis: Long): String {
    val dt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return dt.format(DateTimeFormatter.ISO_LOCAL_DATE)
}
