package com.lazymohan.zebraprinter.grn.ui.to

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.lazymohan.zebraprinter.grn.data.TransferOrderHeader
import com.lazymohan.zebraprinter.grn.data.TransferOrderLine
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit


private fun stableLineId(headerId: Long?, line: TransferOrderLine): Long {
    val raw = line.transferOrderLineId
    if (raw != 0L) return raw
    val hid = headerId ?: 0L
    val ln = (line.lineNumber ?: 0)
    return (hid shl 20) + ln.toLong()
}

@Composable
fun ReviewCardTO(
    header: TransferOrderHeader,
    allLines: List<TransferOrderLine>,
    inputs: List<ToLineInput>,
    submitting: Boolean,
    submitError: String?,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    val lineMap = remember(allLines, header) {
        allLines.associateBy { stableLineId(header.headerId, it) }
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .offset(y = (-8).dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Receipt Preview (TO)",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF143A7B)
                )
            )
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(thickness = 1.dp, color = Color(0xFFE7EAF3))
            Spacer(Modifier.height(8.dp))

            KeyValueRow("TO Number", header.headerNumber)
            KeyValueRow("Business Unit", header.businessUnitName ?: "-")

            Spacer(Modifier.height(10.dp))

            Text(
                "Lines to Receive",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF143A7B)
                )
            )
            Spacer(Modifier.height(6.dp))

            if (inputs.isEmpty()) {
                Text("No lines selected.", color = Color(0xFF6B7280))
            } else {
                data class RowData(
                    val label: String,
                    val item: String,
                    val uom: String,
                    val qty: Int,
                    val lot: String,
                    val exp: String,
                    val rate: Double?,
                    val amount: Double?
                )

                val rows = remember(inputs, lineMap) {
                    inputs.flatMap { li ->
                        val base = lineMap[li.lineId] ?: return@flatMap emptyList()
                        li.sections.sortedBy { it.section }.map { s ->
                            val rate = base.unitPrice
                            val amount = rate?.let { it * s.qty }
                            RowData(
                                label = "Line ${base.lineNumber ?: "-"} • Sec ${s.section}",
                                item = base.itemNumber,
                                uom = base.unitOfMeasure ?: "-",
                                qty = s.qty,
                                lot = s.lot.ifBlank { "-" },
                                exp = s.expiry?.take(10) ?: "-",
                                rate = rate,
                                amount = amount
                            )
                        }
                    }
                }

                val totalLines = rows.size
                val totalQty = rows.sumOf { it.qty }
                val totalAmount = rows.sumOf { it.amount ?: 0.0 }
                val qtyWithPrice = rows.filter { it.rate != null }.sumOf { it.qty.toDouble() }
                val finalRate = if (qtyWithPrice > 0.0) totalAmount / qtyWithPrice else null

                rows.forEachIndexed { idx, r ->
                    CompactLineRowTO(
                        label = r.label,
                        itemNumber = r.item,
                        qty = r.qty,
                        uom = r.uom,
                        lot = r.lot,
                        expiry = r.exp,
                        unitRate = r.rate,
                        lineAmount = r.amount
                    )
                    if (idx != rows.lastIndex) {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE7EAF3))
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(thickness = 1.dp, color = Color(0xFFE7EAF3))
                Spacer(Modifier.height(8.dp))

                KeyValueRow("Total Lines", totalLines.toString())
                KeyValueRow("Total Quantity", totalQty.toString())
                KeyValueRow("Total Amount", fmtMoney(totalAmount))
                KeyValueRow("Final Rate", fmtMoney(finalRate))
            }

            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) { Text("Back to Receive") }

                Button(
                    onClick = onSubmit,
                    enabled = !submitting && inputs.any { it.sections.any { s -> s.qty > 0 } },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                ) {
                    if (submitting) CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White)
                    else Text("Submit Receipt", color = Color.White)
                }
            }

            submitError?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Color(0xFFB00020))
            }
        }
    }
}

/* ---------- small UI pieces / helpers ---------- */

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

@Composable
private fun CompactLineRowTO(
    label: String,
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
        // LEFT: Qty/UOM card
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
                Text(uom, style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569))
                Spacer(Modifier.height(2.dp))
                Text(
                    qty.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF143A7B),
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // RIGHT: details
        Column(Modifier.weight(1f)) {
            Text(
                text = "$label • $itemNumber",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("LOT", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                        Spacer(Modifier.height(2.dp))
                        Text(
                            lot,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }

                val (bg, fg) = run {
                    val d = daysUntil(expiry)
                    when {
                        expiry.isBlank() || expiry == "-" || d == null ->
                            Color(0xFFF1F5F9) to Color(0xFF0F172A)
                        d < 0   -> Color(0xFFFFE4E6) to Color(0xFFB00020)
                        d <= 30 -> Color(0xFFFFF1E6) to Color(0xFF9A3412)
                        d <= 90 -> Color(0xFFFFFBEB) to Color(0xFF92400E)
                        else    -> Color(0xFFE6FBE6) to Color(0xFF166534)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = bg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("EXP", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("RATE", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("AMOUNT", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
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
}

private fun fmtMoney(value: Double?): String = value?.let { "%,.2f".format(it) } ?: "-"

private fun daysUntil(dateStr: String): Int? {
    if (dateStr.isBlank() || dateStr == "-") return null
    val iso = dateStr.take(10)
    return try {
        val target = LocalDate.parse(iso)
        val today = LocalDate.now()
        ChronoUnit.DAYS.between(today, target).toInt()
    } catch (_: DateTimeParseException) {
        null
    }
}
