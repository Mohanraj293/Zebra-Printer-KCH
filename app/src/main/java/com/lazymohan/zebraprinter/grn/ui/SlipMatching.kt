package com.lazymohan.zebraprinter.grn.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lazymohan.zebraprinter.grn.data.PoLineItem
import com.lazymohan.zebraprinter.grn.util.ExtractedItem
import kotlin.math.max
import kotlin.math.roundToInt

/* -------------------- Data Model -------------------- */

data class MatchDetail(
    val slip: ExtractedItem,
    val best: PoLineItem?,
    val similarity: Double
) {
    val matched: Boolean get() = best != null
}

/* -------------------- Matching Logic -------------------- */

fun computeSlipMatches(
    slipItems: List<ExtractedItem>,
    poLines: List<PoLineItem>,
    threshold: Double = 0.42
): List<MatchDetail> {
    fun tokens(s: String?): Set<String> =
        (s ?: "")
            .lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 }
            .toSet()

    fun jaccard(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val inter = a.intersect(b).size.toDouble()
        val union = a.union(b).size.toDouble()
        return if (union == 0.0) 0.0 else inter / union
    }

    return slipItems.map { slip ->
        val slipTok = tokens(slip.description)
        var best: PoLineItem? = null
        var bestScore = 0.0
        for (po in poLines) {
            val base = jaccard(slipTok, tokens(po.Description))
            val code = po.Item.trim()
            val boost = if (code.isNotBlank() && slip.description.contains(code, ignoreCase = true)) 0.95 else 0.0
            val score = max(base, boost)
            if (score > bestScore) {
                bestScore = score
                best = po
            }
        }
        MatchDetail(slip = slip, best = best.takeIf { bestScore >= threshold }, similarity = bestScore)
    }
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(matches) { m ->
                        Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 1.dp) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    m.slip.description,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                if (m.matched) {
                                    val line = m.best!!
                                    val pct = (m.similarity * 100).roundToInt()
                                    Text(
                                        "Matched → ${line.Item} • ${line.Description ?: ""}",
                                        color = Color(0xFF0F766E)
                                    )
                                    LinearProgressIndicator(
                                    progress = { (pct / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                                                                .fillMaxWidth()
                                                                                .padding(top = 6.dp),
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
                                    Text("No PO match above threshold.", color = Color(0xFF9A3412))
                                    Text(
                                        "Tip: Add the correct PO line manually and enter Qty/Lot/Expiry.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF9A3412)
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
