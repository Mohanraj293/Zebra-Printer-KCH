package com.lazymohan.zebraprinter.grn.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
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
    val p = ui.payload ?: return
    val totalQty = p.lines.sumOf { it.Quantity }
    val totalLines = p.lines.size
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

                ReceiptRow("PO Number", p.lines.firstOrNull()?.DocumentNumber ?: "-")
                ReceiptRow("Vendor", p.VendorName)
                ReceiptRow("Site", p.VendorSiteCode)
                ReceiptRow("Business Unit", p.BusinessUnit)

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(thickness = 1.dp, color = Color(0xFFE7EAF3))
                Spacer(Modifier.height(10.dp))

                Text(
                    "Lines to Receive",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF143A7B)
                    )
                )
                Spacer(Modifier.height(6.dp))

                if (p.lines.isEmpty()) {
                    Text("No lines selected.", color = Color(0xFF6B7280))
                } else {
                    p.lines.forEach { ln ->
                        val lot = ln.lotItemLots.firstOrNull()?.LotNumber ?: "-"
                        val exp = ln.lotItemLots.firstOrNull()?.LotExpirationDate ?: "-"
                        Column {
                            Text(
                                text = "Line ${ln.DocumentLineNumber}  •  ${ln.ItemNumber}  •  Qty ${fmt(ln.Quantity)} ${ln.UnitOfMeasure}",
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
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(thickness = 1.dp, color = Color(0xFFE7EAF3))
                Spacer(Modifier.height(10.dp))

                ReceiptRow("Total Lines", totalLines.toString())
                ReceiptRow("Total Quantity", fmt(totalQty))

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
                        enabled = !ui.loading && p.lines.isNotEmpty(),
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                    ) {
                        if (ui.loading) CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White)
                        else Text("Create GRN in Oracle", color = Color.White)
                    }
                }

                if (ui.error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(ui.error, color = Color(0xFFB00020))
                }
            }
        }
    }
}
