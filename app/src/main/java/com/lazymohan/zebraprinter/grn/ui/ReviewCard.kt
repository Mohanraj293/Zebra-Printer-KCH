// app/src/main/java/com/lazymohan/zebraprinter/grn/ui/ReviewCard.kt
package com.lazymohan.zebraprinter.grn.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ReviewCard(
    ui: GrnUiState,
    onSubmit: () -> Unit,
    onEditReceive: () -> Unit
) {
    val staged = ui.staged

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Card(
            modifier = Modifier.padding(horizontal = 20.dp).offset(y = (-12).dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(10.dp)
        ) {
            Column(Modifier.padding(20.dp)) {

                Text(
                    "Receipt Preview",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF143A7B)
                    )
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                Spacer(Modifier.height(12.dp))

                val po = ui.po
                KeyValueRow("PO Number", po?.OrderNumber ?: "-")
                KeyValueRow("Vendor", po?.Supplier ?: "-")
                KeyValueRow("Site", po?.SupplierSite ?: "-")
                KeyValueRow("Business Unit", po?.ProcurementBU ?: "-")

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(thickness = 1.dp, color = Color(0xFFE7EAF3))
                Spacer(Modifier.height(14.dp))

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
                    // === Parts view ===
                    val totalLines = staged.sumOf { it.request.lines.size }
                    val totalQty = staged.sumOf { it.request.lines.sumOf { ln -> ln.Quantity } }

                    staged.forEachIndexed { idx, part ->
                        val isFirst = idx == 0
                        val title = if (isFirst) "Part 1 (Create GRN)" else "Part ${part.sectionIndex}"
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF143A7B)
                            )
                        )
                        Spacer(Modifier.height(6.dp))

                        part.request.lines.forEach { ln ->
                            val lot = ln.lotItemLots.firstOrNull()?.LotNumber ?: "-"
                            val exp = ln.lotItemLots.firstOrNull()?.LotExpirationDate ?: "-"
                            Column {
                                Text(
                                    text = "Line ${ln.DocumentLineNumber} • ${ln.ItemNumber} • ${fmt(ln.Quantity)} ${ln.UnitOfMeasure}",
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

                        if (idx != staged.lastIndex) {
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(thickness = 1.dp, color = Color(0xFFE7EAF3))
                            Spacer(Modifier.height(10.dp))
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(thickness = 1.dp, color = Color(0xFFE7EAF3))
                    Spacer(Modifier.height(10.dp))

                    KeyValueRow("Total Parts", staged.size.toString())
                    KeyValueRow("Total Lines", totalLines.toString())
                    KeyValueRow("Total Quantity", fmt(totalQty))
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
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

/* --- helpers --- */

@Composable
private fun KeyValueRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF6B7280))
        Spacer(Modifier.width(20.dp))
        Text(value, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
    }
}

private fun fmt(d: Double): String =
    if (d % 1.0 == 0.0) d.toInt().toString() else "%.2f".format(d)
