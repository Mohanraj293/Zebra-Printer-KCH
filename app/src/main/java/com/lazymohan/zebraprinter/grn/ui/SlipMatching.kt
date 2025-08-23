package com.lazymohan.zebraprinter.grn.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        val base = "Delivery slip items: $slipCount • Matched to PO: $matchedCount"
        val tail = if (hasUnmatched) " • Tap to see what didn’t match" else " • All mapped ✓"
        Text(
            text = base + if (compact) "" else tail,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
        )
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
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            tonalElevation = 2.dp,
                            shadowElevation = 1.dp,
                            color = Color.White,
                            border = if (m.similarity == 0.0) BorderStroke(1.dp, Color(0xFFB00020)) else null // 🔴 border if no match
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                // Slip description (always show)
                                Text(
                                    text = m.slip.description.ifBlank { "Unrecognized item" },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (m.similarity > 0.0) Color(0xFF143A7B) else Color(0xFFB00020),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(6.dp))

                                if (m.similarity > 0.0 && m.best != null) {
                                    val line = m.best
                                    val pct = (m.similarity * 100).roundToInt()

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Chip("Line: ${line.LineNumber}")
                                        Chip("Item: ${line.Item}")
                                        Chip("${fmt(line.Quantity)} ${line.UOM}")
                                    }
                                    if (!line.Description.isNullOrBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            line.Description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF475569)
                                        )
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    LinearProgressIndicator(
                                        progress = { (pct / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color(0xFF0EA5E9),
                                        trackColor = Color(0xFFE0F2FE),
                                        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                                    )
                                    Text(
                                        "$pct% similarity",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF155E75)
                                    )
                                } else {
                                    // ❌ Unmatched but still show details in red
                                    Text(
                                        "❌ No PO match (0% similarity).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFB00020)
                                    )

                                    m.best?.let { line ->
                                        Spacer(Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Chip("Line: ${line.LineNumber}")
                                            Chip("Item: ${line.Item}")
                                            Chip("${fmt(line.Quantity)} ${line.UOM}")
                                        }
                                        if (!line.Description.isNullOrBlank()) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                line.Description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFB00020) // 🔴 red text for unmatched desc
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Tip: Add the correct PO line manually and enter Qty/Lot/Expiry.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFB00020)
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


