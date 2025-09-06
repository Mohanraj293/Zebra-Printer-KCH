package com.lazymohan.zebraprinter.grn.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lazymohan.zebraprinter.grn.data.PoLineItem
import com.lazymohan.zebraprinter.grn.util.ExtractedItem
import kotlin.math.roundToInt

/* -------------------- Data Model -------------------- */
data class MatchDetail(
    val slip: ExtractedItem,
    val best: PoLineItem?,
    val similarity: Double
) {
    val matched: Boolean get() = best != null
}

/* -------------------- UI: Match summary bar -------------------- */
@Composable
fun SlipMatchBar(
    slipCount: Int,
    matchedCount: Int,
    hasUnmatched: Boolean,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val bg = if (hasUnmatched) Color(0xFFFFF4E5) else Color(0xFFE9F9EF)
    val fg = if (hasUnmatched) Color(0xFF92400E) else Color(0xFF166534)

    Surface(
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title (smaller but clear)
            Text(
                text = "Purchase Order Summary",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            )

            // Total PO Items
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Inventory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Total Items: ",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                )
                Text(
                    "$slipCount",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            // Matched Items
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Matched: ",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                )
                Text(
                    "$matchedCount",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                )
            }

            // Tail note
            val tail = if (hasUnmatched) {
                "⚠️ Tap to review unmatched items"
            } else {
                "✅ All items mapped"
            }
            Text(
                text = tail,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (hasUnmatched) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                )
            )
        }
    }

}

/* -------------------- UI: Match details dialog -------------------- */
@Composable
fun MatchBreakdownDialog(
    matches: List<MatchDetail>,
    onDismiss: () -> Unit
) {
    val matched = matches.count { it.matched }
    val total = matches.size

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delivery Slip Match Details") },
        text = {
            Column {
                Text(
                    "$matched of $total items matched to PO lines.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF334155)
                )
                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(matches) { m ->
                        val isMatched = m.similarity > 0.0 && m.best != null
                        val line = m.best
                        val pct = (m.similarity * 100).roundToInt()
                        val border = if (!isMatched) BorderStroke(1.dp, Color(0xFFB00020)) else null

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            tonalElevation = 2.dp,
                            shadowElevation = 1.dp,
                            color = Color.White,
                            border = border,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(14.dp)) {

                                // Header row: status + % pill
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isMatched) "MATCHED" else "UNMATCHED",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMatched) Color(0xFF166534) else Color(0xFFB00020)
                                        )
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (isMatched) Color(0xFFE6F4EA) else Color(0xFFFDE7E9),
                                        contentColor = if (isMatched) Color(0xFF166534) else Color(0xFFB00020)
                                    ) {
                                        Text(
                                            "$pct%",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                // DELIVERY SLIP (always shown)
                                Text(
                                    "DELIVERY SLIP",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309) // amber accent
                                    )
                                )
                                Text(
                                    text = m.slip.description.ifBlank { "Unrecognized item" },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF374151),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    m.slip.qtyDelivered.let { Chip("Qty: ${fmt(it)}") }
                                    m.slip.batchNo.takeIf { it.isNotBlank() }?.let { Chip("Lot: $it") }
                                    m.slip.expiryDate.takeIf { it.isNotBlank() }?.let { Chip("Exp: $it") }
                                }

                                Spacer(Modifier.height(10.dp))

                                // PO LINE (ALWAYS show this section; if null, show placeholder)
                                val poHeaderColor = if (line != null) {
                                    if (isMatched) Color(0xFF166534) else Color(0xFF334155)
                                } else {
                                    Color(0xFF64748B)
                                }
                                Text(
                                    "PO LINE",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = poHeaderColor
                                    )
                                )
                                if (line != null) {
                                    if (!line.Description.isNullOrBlank()) {
                                        Text(
                                            line.Description,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = Color(0xFF374151),
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Chip("Line: ${line.LineNumber}")
                                        Chip("Item: ${line.Item}")
                                        Chip("${fmt(line.Quantity)} ${line.UOM}")
                                    }
                                } else {
                                    Text(
                                        "No PO line selected.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B)
                                    )
                                }

                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}





