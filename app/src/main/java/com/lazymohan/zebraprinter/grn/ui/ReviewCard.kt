package com.lazymohan.zebraprinter.grn.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

@Composable
fun ReviewCard(
    ui: GrnUiState,
    onSubmit: () -> Unit,
    onEditReceive: () -> Unit,
    onAddAttachments: (List<Uri>) -> Unit
) {
    val staged = ui.staged

    // quick lookup of unit price by PO line number
    val linePriceMap = remember(ui.allPoLines) {
        ui.allPoLines.associate { it.LineNumber to it.Price }
    }

    // Multi-file picker for any document type
    val docPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) onAddAttachments(uris)
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .offset(y = (-8).dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(Modifier.padding(16.dp)) {

                Text(
                    "Receipt Preview",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF143A7B)
                    )
                )
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(thickness = 1.dp, color = Color(0xFFE7EAF3))
                Spacer(Modifier.height(8.dp))

                val po = ui.po
                KeyValueRow("PO Number", po?.OrderNumber ?: "-")
                KeyValueRow("Vendor", po?.Supplier ?: "-")
                KeyValueRow("Site", po?.SupplierSite ?: "-")
                KeyValueRow("Business Unit", po?.ProcurementBU ?: "-")
                KeyValueRow("Invoice Number", ui.invoiceNumber)

                Spacer(Modifier.height(10.dp))

                // Allow attaching ANY file type(s)
                OutlinedButton(
                    onClick = { docPicker.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add Attachment")
                }

                if (ui.extraAttachments.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Attached (${ui.extraAttachments.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF475569)
                    )
                    Spacer(Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ui.extraAttachments.take(5).forEach {
                            Text(
                                "• ${it.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (ui.extraAttachments.size > 5) {
                            Text(
                                "+${ui.extraAttachments.size - 5} more…",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }


                if (staged.isEmpty()) {
                    Text(
                        "Lines to Receive",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF143A7B)
                        )
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("No lines selected.", color = Color(0xFF6B7280))
                } else {
                    // Totals
                    val totalLines = remember(staged) { staged.sumOf { it.request.lines.size } }
                    val totalQty =
                        remember(staged) { staged.sumOf { it.request.lines.sumOf { ln -> ln.Quantity } } }

                    // Amount totals & weighted final rate
                    val (totalAmount, pricedQty) = remember(staged, linePriceMap) {
                        var amt = 0.0
                        var qtyWithPrice = 0.0
                        staged.forEach { part ->
                            part.request.lines.forEach { ln ->
                                val price = linePriceMap[ln.DocumentLineNumber]
                                if (price != null) {
                                    amt += price * ln.Quantity
                                    qtyWithPrice += ln.Quantity
                                }
                            }
                        }
                        amt to qtyWithPrice
                    }
                    val finalRate = remember(totalAmount, pricedQty) {
                        if (pricedQty > 0.0) totalAmount / pricedQty else null
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(thickness = 1.dp, color = Color(0xFFE7EAF3))
                    Spacer(Modifier.height(8.dp))

                    // Parts + lines
                    staged.forEachIndexed { idx, part ->
                        val isFirst = idx == 0
                        val title =
                            if (isFirst) "Part 1 • Create GRN" else "Part ${part.sectionIndex} • Add Items"
                        Text(
                            title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        )
                        Spacer(Modifier.height(6.dp))

                        part.request.lines.forEach { ln ->
                            val lot = ln.lotItemLots.firstOrNull()?.LotNumber ?: "-"
                            val exp = ln.lotItemLots.firstOrNull()?.LotExpirationDate ?: "-"

                            val unitRate = linePriceMap[ln.DocumentLineNumber]
                            val lineAmount = unitRate?.let { it * ln.Quantity }

                            CompactLineRow(
                                lineNumber = ln.DocumentLineNumber,
                                itemNumber = ln.ItemNumber,
                                qty = ln.Quantity,
                                uom = ln.UnitOfMeasure,
                                lot = lot,
                                expiry = exp,
                                unitRate = unitRate,
                                lineAmount = lineAmount
                            )
                        }

                        if (idx != staged.lastIndex) {
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(thickness = 1.dp, color = Color(0xFFE7EAF3))
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(thickness = 1.dp, color = Color(0xFFE7EAF3))
                    Spacer(Modifier.height(8.dp))

                    KeyValueRow("Total Parts", staged.size.toString())
                    KeyValueRow("Total Lines", totalLines.toString())
                    KeyValueRow("Total Quantity", totalQty.toString())

                    // money totals
                    KeyValueRow("Total Amount", fmtMoney(totalAmount))
                    KeyValueRow("Final Rate", fmtMoney(finalRate))
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onEditReceive,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) { Text("Back to Receive") }

                    Button(
                        onClick = onSubmit,
                        enabled = !ui.loading && staged.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                    ) {
                        if (ui.loading) CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        else Text("Submit Parts", color = Color.White)
                    }
                }

                ui.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = Color(0xFFB00020))
                }
            }
        }
    }
}

/* --- condensed pieces --- */
@Composable
private fun CompactLineRow(
    lineNumber: Int,
    itemNumber: String,
    qty: Int,
    uom: String,
    lot: String,
    expiry: String,
    unitRate: Double?,
    lineAmount: Double?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT COLUMN: fixed width for Qty/UOM card
        Box(
            modifier = Modifier
                .width(70.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEFF4FF), RoundedCornerShape(8.dp))
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // UOM on top
                Text(
                    text = uom,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF475569)
                )
                Spacer(Modifier.height(2.dp))
                // Quantity bold below
                Text(
                    text = qty.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF143A7B),
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // RIGHT COLUMN: details
        Column(Modifier.weight(1f)) {
            Text(
                text = "Line $lineNumber • $itemNumber",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            // Row 1: LOT + EXP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LOT chip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "LOT",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            lot,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }

                // EXP chip
                val (bg, fg) = remember(expiry) {
                    val d = daysUntil(expiry)
                    when {
                        expiry.isBlank() || expiry == "-" || d == null -> Pair(
                            Color(0xFFF1F5F9),
                            Color(0xFF0F172A)
                        )

                        d < 0 -> Pair(Color(0xFFFFE4E6), Color(0xFFB00020))
                        d <= 30 -> Pair(Color(0xFFFFF1E6), Color(0xFF9A3412))
                        d <= 90 -> Pair(Color(0xFFFFFBEB), Color(0xFF92400E))
                        else -> Pair(Color(0xFFE6FBE6), Color(0xFF166534))
                    }
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = bg,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "EXP",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            expiry,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = fg
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Row 2: Rate & Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "RATE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            fmtMoney(unitRate),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFEEF2FF),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "AMOUNT",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            fmtMoney(lineAmount),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(4.dp))
}

/* --- helpers --- */

@Composable
private fun KeyValueRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF6B7280), style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F172A),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

//private fun fmt(d: Double): String =
//    if (d % 1.0 == 0.0) d.toInt().toString() else "%.2f".format(d)

private fun fmtMoney(value: Double?): String =
    value?.let { "%,.2f".format(it) } ?: "-"

//private fun fmtQtyBadge(q: Double): String {
//    val rounded = if (q % 1.0 == 0.0) q.toInt().toString() else (q * 100.0).roundToInt() / 100.0
//    return rounded.toString()
//}

private fun daysUntil(dateStr: String): Int? {
    if (dateStr.isBlank() || dateStr == "-") return null
    return try {
        val target = LocalDate.parse(dateStr) // expects YYYY-MM-DD
        val today = LocalDate.now()
        ChronoUnit.DAYS.between(today, target).toInt()
    } catch (_: DateTimeParseException) {
        null
    }
}
