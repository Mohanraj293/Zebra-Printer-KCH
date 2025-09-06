package com.lazymohan.zebraprinter.grn.ui.to

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lazymohan.zebraprinter.grn.data.TransferOrderHeader
import com.lazymohan.zebraprinter.grn.data.TransferOrderLine
import com.lazymohan.zebraprinter.grn.data.ShipmentLine
import com.lazymohan.zebraprinter.grn.ui.ReadFieldInline

/* ---- local helper: stable id for lines ---- */
private fun stableLineId(headerId: Long?, line: TransferOrderLine): Long {
    val raw = line.transferOrderLineId
    if (raw != 0L) return raw
    val hid = headerId ?: 0L
    val ln = (line.lineNumber ?: 0)
    return (hid shl 20) + ln.toLong()
}

@Composable
fun ToAndReceiveCard(
    header: TransferOrderHeader?,
    linesLoaded: Boolean,
    allLines: List<TransferOrderLine>,
    inputs: List<ToLineInput>,
    shipment: ShipmentLine?,
    onBack: () -> Unit,
    onAddLine: (Long) -> Unit,
    onRemoveLine: (Long) -> Unit,
    onAddSection: (Long) -> Unit,
    onRemoveSection: (Long, Int) -> Unit,
    onUpdateQty: (Long, Int, Int) -> Unit,
    onUpdateLot: (Long, Int, String) -> Unit,
    onReview: () -> Unit
) {
    val headerId = header?.headerId

    val candidates by remember(allLines, inputs, headerId) {
        derivedStateOf {
            val chosen = inputs.map { it.lineId }.toSet()
            allLines.filter { line -> stableLineId(headerId, line) !in chosen }
        }
    }

    val allowReview by remember(inputs, header) {
        derivedStateOf {
            val hasQty = inputs.isNotEmpty() && inputs.any { it.sections.any { s -> s.qty > 0 } }
            val hasToNumber = header?.headerNumber?.toLongOrNull() != null
            hasQty && hasToNumber
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.verticalScroll(rememberScrollState())) {

            // --- Transfer Order Header ---
            Card(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Transfer Order Details",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color(0xFF143A7B),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    if (header == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Loading TO…", color = Color.Gray)
                        }
                    } else {
                        ReadFieldInline("TO Number", header.headerNumber)
                        ReadFieldInline("Business Unit", header.businessUnitName ?: "-")
                        ReadFieldInline("Status", header.status ?: "-")
                        ReadFieldInline("Interface Status", header.interfaceStatus ?: "-")
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
                        Text(
                            "Lines",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color(0xFF143A7B),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = { showAddDialog = true },
                            enabled = candidates.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEFF4FF),
                                contentColor = Color(0xFF1E3A8A)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) { Text("➕ Add Item") }
                    }

                    Spacer(Modifier.height(10.dp))

                    when {
                        !linesLoaded -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Loading lines…", color = Color.Gray)
                            }
                        }
                        allLines.isEmpty() -> {
                            Text("No TO lines found.", color = Color(0xFF64748B))
                        }
                        else -> {
                            Column {
                                inputs.forEach { li ->
                                    val base = allLines.firstOrNull { l ->
                                        l.transferOrderLineId == li.lineId ||
                                                stableLineId(headerId, l) == li.lineId
                                    } ?: return@forEach
                                    key(li.lineId) {
                                        ToLineCard(
                                            line = base,
                                            input = li,
                                            onRemoveLine = { onRemoveLine(li.lineId) },
                                            onAddSection = { onAddSection(li.lineId) },
                                            onRemoveSection = { sec -> onRemoveSection(li.lineId, sec) },
                                            onUpdateQty = { sec, qty -> onUpdateQty(li.lineId, sec, qty) },
                                            onUpdateLot = { sec, lot -> onUpdateLot(li.lineId, sec, lot) }
                                        )
                                        Spacer(Modifier.height(10.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
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
    }

    // --- Add Item Dialog ---
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Select Transfer Order Line") },
            text = {
                if (candidates.isEmpty()) {
                    Text("All TO lines are already added.", color = Color(0xFF64748B))
                } else {
                    LazyColumn {
                        items(items = candidates) { c ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable {
                                        onAddLine(stableLineId(headerId, c))
                                        showAddDialog = false
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(c.itemDescription ?: c.itemNumber, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Item: ${c.itemNumber}", color = Color(0xFF64748B))
                                    if (!c.subinventory.isNullOrBlank()) {
                                        Text("Subinv (Dest): ${c.subinventory}", color = Color(0xFF64748B))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun ToLineCard(
    line: TransferOrderLine,
    input: ToLineInput,
    onRemoveLine: () -> Unit,
    onAddSection: () -> Unit,
    onRemoveSection: (Int) -> Unit,
    onUpdateQty: (Int, Int) -> Unit,
    onUpdateLot: (Int, String) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    var confirmDelete by remember { mutableStateOf(false) }

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
            // Title
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = line.itemDescription ?: line.itemNumber,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF143A7B),
                        maxLines = 3
                    )
                    if (!line.itemDescription.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Item: ${line.itemNumber}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF334155))
                        )
                    }
                }

                // right-top delete icon
                IconButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete line",
                        tint = Color(0xFFB00020)
                    )
                }
            }

            if (expanded) {
                Spacer(Modifier.height(10.dp))
                ReadFieldInline("UOM", line.unitOfMeasure ?: "-")
                ReadFieldInline("Subinventory (Dest)", line.subinventory ?: "-")
                ReadFieldInline(
                    "Requested Qty",
                    (line.quantity ?: 0.0).let { if (it % 1.0 == 0.0) it.toInt().toString() else "%.2f".format(it) }
                )
                ReadFieldInline("Unit Price", line.unitPrice?.let { "%,.2f".format(it) } ?: "-")

                Spacer(Modifier.height(12.dp))
                input.sections.sortedBy { it.section }.forEach { sec ->
                    SectionRow(
                        title = "Section #${sec.section}",
                        qty = sec.qty,
                        lot = sec.lot,
                        expiry = sec.expiry,
                        onQtyChange = { onUpdateQty(sec.section, it) },
                        onLotChange = { onUpdateLot(sec.section, it) },
                        onDelete = { onRemoveSection(sec.section) }
                    )
                    Spacer(Modifier.height(8.dp))
                }

                TextButton(
                    onClick = onAddSection,
                    modifier = Modifier
                        .background(Color(0xFFE8F0FF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text("+ Add Section", color = Color(0xFF1D4ED8), fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(6.dp))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text("Remove line ${line.itemNumber}?") },
            text = { Text("This will remove the line from this receipt.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onRemoveLine() }) {
                    Text("Remove", color = Color(0xFFB00020))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionRow(
    title: String,
    qty: Int,
    lot: String,
    expiry: String?,
    onQtyChange: (Int) -> Unit,
    onLotChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFF),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(title, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFB00020))
                ) { Text("Delete") }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = qty.takeIf { it > 0 }?.toString().orEmpty(),
                onValueChange = { v -> onQtyChange(v.filter { it.isDigit() }.toIntOrNull() ?: 0) },
                placeholder = { Text("Quantity") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = lot,
                onValueChange = onLotChange,
                placeholder = { Text("Lot Number (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (!expiry.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                ReadFieldInline("Expiry", expiry)
            }
        }
    }
}
