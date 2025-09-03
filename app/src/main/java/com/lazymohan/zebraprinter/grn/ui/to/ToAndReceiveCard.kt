@file:OptIn(ExperimentalLayoutApi::class)

package com.lazymohan.zebraprinter.grn.ui.to

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lazymohan.zebraprinter.grn.data.TransferOrderHeader
import com.lazymohan.zebraprinter.grn.data.TransferOrderLine
import com.lazymohan.zebraprinter.grn.data.ShipmentLine
import com.lazymohan.zebraprinter.grn.ui.ReadFieldInline

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
    val taken = inputs.map { it.lineId }.toSet()
    val candidates = allLines.filter { it.transferOrderLineId !in taken }

    val allowReview by remember(inputs) {
        derivedStateOf {
            inputs.isNotEmpty() && inputs.all { li -> li.sections.any { it.qty > 0 } }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.verticalScroll(rememberScrollState())) {

            // --- Header card (read-only) ---
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
                        ReadFieldInline("Header Number", header.headerNumber)
                        ReadFieldInline("Business Unit", header.businessUnitName ?: "-")
                        ReadFieldInline("Status", header.status ?: "-")
                        ReadFieldInline("Interface Status", header.interfaceStatus ?: "-")
                    }

                    Spacer(Modifier.height(8.dp))
                    val sn = shipment?.shipmentNumber?.toString() ?: "—"
                    val lot = shipment?.lotNumber ?: "—"
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Shipment (from Shipping)", fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                            Spacer(Modifier.height(6.dp))
                            ReadFieldInline("Shipment #", sn)
                            ReadFieldInline("Lot (default)", lot)
                        }
                    }
                }
            }

            // --- Lines card ---
            Card(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Lines",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color(0xFF143A7B),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = { /* open dialog */ },
                            enabled = candidates.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEFF4FF),
                                contentColor = Color(0xFF1E3A8A)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) { Text("➕ Add Item") }
                    }

                    var showAdd by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        // ensure dialog shows only when triggered
                    }
                    if (showAdd) {
                        AddToLineDialog(
                            candidates = candidates,
                            onPick = { id -> showAdd = false; onAddLine(id) },
                            onDismiss = { showAdd = false }
                        )
                    }
                    // link button triggers dialog
                    Row(Modifier.fillMaxWidth()) {
                        Spacer(Modifier.weight(1f))
                        TextButton(
                            enabled = candidates.isNotEmpty(),
                            onClick = { showAdd = true }
                        ) { Text("Select From TO") }
                    }

                    Spacer(Modifier.height(6.dp))

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
                            inputs.forEach { li ->
                                val base = allLines.firstOrNull { it.transferOrderLineId == li.lineId } ?: return@forEach
                                ToLineBox(
                                    line = base,
                                    input = li,
                                    onRemoveLine = { onRemoveLine(base.transferOrderLineId) },
                                    onAddSection = { onAddSection(base.transferOrderLineId) },
                                    onRemoveSection = { sec -> onRemoveSection(base.transferOrderLineId, sec) },
                                    onUpdateQty = { sec, qty -> onUpdateQty(base.transferOrderLineId, sec, qty) },
                                    onUpdateLot = { sec, lot -> onUpdateLot(base.transferOrderLineId, sec, lot) }
                                )
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) { Text("Back") }

                        Button(
                            onClick = onReview,
                            modifier = Modifier.weight(1f).height(52.dp).alpha(if (allowReview) 1f else 0.35f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                        ) { Text("Review & Submit", color = Color.White) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToLineBox(
    line: TransferOrderLine,
    input: ToLineInput,
    onRemoveLine: () -> Unit,
    onAddSection: () -> Unit,
    onRemoveSection: (Int) -> Unit,
    onUpdateQty: (Int, Int) -> Unit,
    onUpdateLot: (Int, String) -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFDFEFF),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = line.itemNumber,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF143A7B)
            )
            Spacer(Modifier.height(6.dp))
            ReadFieldInline("UOM", line.unitOfMeasure ?: "-")
            ReadFieldInline("Subinventory", line.subinventory ?: "-")

            Spacer(Modifier.height(10.dp))
            Divider()
            Spacer(Modifier.height(10.dp))

            input.sections.sortedBy { it.section }.forEach { sec ->
                SectionRow(
                    title = "Section #${sec.section}",
                    qty = sec.qty,
                    lot = sec.lot,
                    onQtyChange = { onUpdateQty(sec.section, it) },
                    onLotChange = { onUpdateLot(sec.section, it) },
                    onDelete = { onRemoveSection(sec.section) }
                )
                Spacer(Modifier.height(8.dp))
            }

            TextButton(
                onClick = onAddSection,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE8F0FF))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text("+ Add Section", color = Color(0xFF1D4ED8), fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onRemoveLine, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFB00020))) {
                    Text("Remove line")
                }
            }
        }
    }
}

@Composable
private fun SectionRow(
    title: String,
    qty: Int,
    lot: String,
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
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFB00020))) {
                    Text("Delete")
                }
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
                placeholder = { Text("Lot Number (optional — will fallback to shipment lot)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
