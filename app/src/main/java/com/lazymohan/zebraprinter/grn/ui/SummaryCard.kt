package com.lazymohan.zebraprinter.grn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun SummaryCard(
    ui: GrnUiState,
    onStartOver: () -> Unit
) {
    val r = ui.receipt
    val parts = ui.progress

    val allSuccess = parts.isNotEmpty() && parts.all { it.returnStatus.equals("SUCCESS", true) }
    val anyFailed = parts.any { it.returnStatus.equals("ERROR", true) }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = if (allSuccess && !anyFailed) Color(0xFF22C55E) else Color(0xFF64748B),
                modifier = Modifier.size(96.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            if (allSuccess && !anyFailed) "All Parts Successfully Received"
            else if (anyFailed) "Receipt Failed"
            else "Receipt Submission",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall.copy(
                color = if (allSuccess && !anyFailed) Color(0xFF0E9F6E) else Color(0xFF143A7B),
                fontWeight = FontWeight.ExtraBold
            )
        )
        Spacer(Modifier.height(14.dp))

        Card(
            modifier = Modifier.padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(Modifier.padding(20.dp)) {

                // --- Timeline ---
                parts.forEachIndexed { idx, p ->
                    TimelineRow(
                        index = idx,
                        title = if (idx == 0) "GRN Created (Part 1)" else "Added Part ${p.sectionIndex}",
                        subtitle = p.message ?: "Processing…",
                        status = p.status,
                        returnStatus = p.returnStatus,
                        isLast = idx == parts.lastIndex,
                        errors = p.errors
                    )
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(Modifier, DividerDefaults.Thickness, color = Color(0xFFE7EAF3))
                Spacer(Modifier.height(12.dp))

                // Transaction summary
                SectionHeaderTitle("Transaction Summary")
                KeyValueRow("GRN Number:", r?.ReceiptNumber ?: "-")
                KeyValueRow("Status:", r?.ReturnStatus ?: if (anyFailed) "FAILED" else if (allSuccess) "SUCCESS" else "-")

                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onStartOver,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                ) {
                    Text(if (anyFailed) "Try New Receipt" else "Start New Receipt", color = Color.White)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

/* ---------- file-private UI atoms ---------- */

@Composable
private fun TimelineRow(
    index: Int,
    title: String,
    subtitle: String,
    status: PartStatus,
    returnStatus: String?,
    isLast: Boolean,
    errors: List<String> = emptyList()
) {
    val colorActive = Color(0xFF2E6BFF)
    val colorSuccess = Color(0xFF22C55E)
    val colorPending = Color(0xFFCBD5E1)
    val colorFailed = Color(0xFFEF4444)

    val dotColor = when {
        returnStatus.equals("SUCCESS", true) -> colorSuccess
        returnStatus.equals("ERROR", true) -> colorFailed
        status == PartStatus.SUBMITTING -> colorActive
        else -> colorPending
    }

    Row(Modifier.fillMaxWidth()) {
        // Timeline rail
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(14.dp).background(dotColor, CircleShape)
            )
            if (!isLast) {
                Spacer(
                    Modifier.width(2.dp).height(28.dp).background(colorPending)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        // Texts
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (!returnStatus.isNullOrBlank()) "Status: $returnStatus" else subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF475569))
            )
            // Inline errors
            errors.forEach {
                Spacer(Modifier.height(4.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFB00020),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun SectionHeaderTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium.copy(
            color = Color(0xFF143A7B),
            fontWeight = FontWeight.SemiBold
        )
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun KeyValueRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF6B7280))
        Text(value, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
    }
}
