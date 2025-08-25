// app/src/main/java/com/lazymohan/zebraprinter/grn/ui/ReviewCard.kt
package com.lazymohan.zebraprinter.grn.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
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
    onEditReceive: () -> Unit
) {
    val staged = ui.staged

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

                // Header (unchanged copy, slightly tighter spacing)
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

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(thickness = 1.dp, color = Color(0xFFE7EAF3))
                Spacer(Modifier.height(10.dp))

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
                    // Totals (condensed)
                    val totalLines = remember(staged) { staged.sumOf { it.request.lines.size } }
                    val totalQty = remember(staged) { staged.sumOf { it.request.lines.sumOf { ln -> ln.Quantity } } }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(thickness = 1.dp, color = Color(0xFFE7EAF3))
                    Spacer(Modifier.height(8.dp))

                    // Compact parts + lines
                    staged.forEachIndexed { idx, part ->
                        val isFirst = idx == 0
                        val title = if (isFirst) "Part 1 • Create GRN" else "Part ${part.sectionIndex} • Add Items"
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

                            CompactLineRow(
                                lineNumber = ln.DocumentLineNumber,
                                itemNumber = ln.ItemNumber,
                                qty = ln.Quantity,
                                uom = ln.UnitOfMeasure,
                                lot = lot,
                                expiry = exp
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
                    KeyValueRow("Total Quantity", fmt(totalQty))
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Keep same labels & sizing as your original
                    OutlinedButton(
                        onClick = onEditReceive,
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) { Text("Back to Receive") }

                    Button(
                        onClick = onSubmit,
                        enabled = !ui.loading && staged.isNotEmpty(),
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                    ) {
                        if (ui.loading) CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White)
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
    qty: Double,
    uom: String,
    lot: String,
    expiry: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Qty block
        Column(
            modifier = Modifier
                .widthIn(min = 60.dp)
                .background(Color(0xFFEFF4FF), RoundedCornerShape(8.dp))
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = fmtQtyBadge(qty),
                style = MaterialTheme.typography.titleSmall, // slightly bigger again
                fontWeight = FontWeight.Bold,
                color = Color(0xFF143A7B),
                maxLines = 1
            )
            Text(
                text = uom,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF475569)
            )
        }

        Spacer(Modifier.width(6.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = "Line $lineNumber • $itemNumber",
                style = MaterialTheme.typography.bodyMedium, // back to medium for readability
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(2.dp))

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


                // EXP chip — strong colors depending on date
                val (bg, fg) = remember(expiry) {
                    val d = daysUntil(expiry)
                    when {
                        expiry.isBlank() || expiry == "-" || d == null -> Pair(Color(0xFFF1F5F9), Color(0xFF0F172A))
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



        }
    }

    Spacer(Modifier.height(4.dp))
}


/* --- helpers (unchanged behavior) --- */

@Composable
private fun KeyValueRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF6B7280), style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.width(12.dp))
        Text(value, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A), style = MaterialTheme.typography.bodySmall)
    }
}

private fun fmt(d: Double): String =
    if (d % 1.0 == 0.0) d.toInt().toString() else "%.2f".format(d)

private fun fmtQtyBadge(q: Double): String {
    val rounded = if (q % 1.0 == 0.0) q.toInt().toString() else (q * 100.0).roundToInt() / 100.0
    return rounded.toString()
}

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
