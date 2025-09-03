package com.lazymohan.zebraprinter.grn.ui.to

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lazymohan.zebraprinter.grn.data.TransferOrderHeader
import com.lazymohan.zebraprinter.grn.data.TransferOrderLine

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
    val lineMap = remember(allLines) { allLines.associateBy { it.transferOrderLineId } }

    Card(
        modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-8).dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Receipt Preview (TO)",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF143A7B)
                )
            )

            Spacer(Modifier.height(10.dp))
            RowItem("TO Number", header.headerNumber)
            RowItem("Business Unit", header.businessUnitName ?: "-")

            Spacer(Modifier.height(12.dp))
            Divider()
            Spacer(Modifier.height(10.dp))

            // lines
            if (inputs.isEmpty()) {
                Text("No lines selected.", color = Color(0xFF6B7280))
            } else {
                inputs.forEach { li ->
                    val base = lineMap[li.lineId]
                    if (base != null) {
                        LineBlock(base, li)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(52.dp)) { Text("Back to Receive") }
                Button(
                    onClick = onSubmit,
                    enabled = !submitting && inputs.any { it.sections.any { s -> s.qty > 0 } },
                    modifier = Modifier.weight(1f).height(52.dp),
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

@Composable
private fun RowItem(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF6B7280), style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.width(12.dp))
        Text(value, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LineBlock(base: TransferOrderLine, li: ToLineInput) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF9FAFB)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Item ${base.itemNumber}", fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
            Spacer(Modifier.height(6.dp))
            RowItem("UOM", base.unitOfMeasure ?: "-")
            RowItem("Subinventory", base.subinventory ?: "-")
            Spacer(Modifier.height(6.dp))
            li.sections.sortedBy { it.section }.forEach { s ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF4FF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sec ${s.section}", color = Color(0xFF1E3A8A), style = MaterialTheme.typography.labelSmall)
                        Text(
                            "Qty ${s.qty} • Lot ${s.lot.ifBlank { "—" }}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}
