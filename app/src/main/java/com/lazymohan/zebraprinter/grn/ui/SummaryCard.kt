package com.lazymohan.zebraprinter.grn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
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

    val hasSuccess = parts.any { it.status == PartStatus.SUCCESS || it.returnStatus.equals("SUCCESS", true) }
    val hasFailed = parts.any { it.status == PartStatus.FAILED || it.returnStatus.equals("ERROR", true) }
    val allSuccess = parts.isNotEmpty() && hasSuccess && !hasFailed
    val partial = hasSuccess && hasFailed

    // Choose icon & color based on overall status
    val (icon, tint) = when {
        allSuccess -> Icons.Outlined.CheckCircle to Color(0xFF22C55E) // green
        partial    -> Icons.Outlined.Error to Color(0xFFF59E0B)       // amber
        hasFailed  -> Icons.Outlined.Error to Color(0xFFEF4444)       // red
        else       -> Icons.Outlined.CheckCircle to Color(0xFF64748B) // neutral/grey
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(96.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            when {
                allSuccess -> "All Parts Successfully Received"
                partial    -> "Partially Received"
                hasFailed  -> "Receipt Failed"
                else       -> "Receipt Submission"
            },
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall.copy(
                color = when {
                    allSuccess -> Color(0xFF0E9F6E)
                    partial    -> Color(0xFFB45309) // amber-700
                    hasFailed  -> Color(0xFF991B1B)
                    else       -> Color(0xFF143A7B)
                },
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
                        errors = p.errors,
                        httpCode = p.httpCode,
                        url = p.url,
                        errorBody = p.errorBody,
                        exception = p.exception
                    )
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(Modifier, DividerDefaults.Thickness, color = Color(0xFFE7EAF3))
                Spacer(Modifier.height(12.dp))

                // Transaction summary
                val overallText = when {
                    allSuccess -> "SUCCESS"
                    partial    -> "PARTIAL"
                    hasFailed  -> "FAILED"
                    else       -> r?.ReturnStatus ?: "-"
                }
                SectionHeaderTitle("Transaction Summary")
                KeyValueRow("GRN Number:", r?.ReceiptNumber ?: "-")
                KeyValueRow("Status:", overallText)

                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onStartOver,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                ) {
                    Text(if (hasFailed) "Try New Receipt" else "Start New Receipt", color = Color.White)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

/* ---------- file-private UI atoms ---------- */

@Composable
private fun StatusChip(status: PartStatus, returnStatus: String?) {
    val (bg, fg, label) = when {
        status == PartStatus.SUCCESS || returnStatus.equals("SUCCESS", true) ->
            Triple(Color(0xFFD1FAE5), Color(0xFF065F46), "SUCCESS")
        status == PartStatus.FAILED || returnStatus.equals("ERROR", true) ->
            Triple(Color(0xFFFEE2E2), Color(0xFF991B1B), "FAILED")
        status == PartStatus.SUBMITTING ->
            Triple(Color(0xFFE0EAFF), Color(0xFF1E40AF), "PROCESSING")
        else ->
            Triple(Color(0xFFE5E7EB), Color(0xFF374151), "PENDING")
    }
    Surface(color = bg, shape = RoundedCornerShape(999.dp)) {
        Text(
            label,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun ExpandableErrorBox(header: String, detail: String) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        color = Color(0xFFFFF1F2),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                header,
                color = Color(0xFF991B1B),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            )
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(
                    detail,
                    color = Color(0xFF7F1D1D),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(
    index: Int,
    title: String,
    subtitle: String,
    status: PartStatus,
    returnStatus: String?,
    isLast: Boolean,
    errors: List<String> = emptyList(),
    httpCode: Int? = null,
    url: String? = null,
    errorBody: String? = null,
    exception: String? = null
) {
    val colorActive = Color(0xFF2E6BFF)
    val colorSuccess = Color(0xFF22C55E)
    val colorPending = Color(0xFFCBD5E1)
    val colorFailed = Color(0xFFEF4444)

    val dotColor = when {
        returnStatus.equals("SUCCESS", true) || status == PartStatus.SUCCESS -> colorSuccess
        status == PartStatus.FAILED || returnStatus.equals("ERROR", true) -> colorFailed
        status == PartStatus.SUBMITTING -> colorActive
        else -> colorPending
    }

    Row(Modifier.fillMaxWidth()) {
        // Timeline rail
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(14.dp).background(dotColor, CircleShape))
            if (!isLast) {
                Spacer(Modifier.width(2.dp).height(28.dp).background(colorPending))
            }
        }
        Spacer(Modifier.width(12.dp))
        // Texts
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.weight(1f)
                )
                StatusChip(status = status, returnStatus = returnStatus)
            }

            Spacer(Modifier.height(2.dp))
            Text(
                when {
                    status == PartStatus.SUBMITTING -> "Processing…"
                    !returnStatus.isNullOrBlank()   -> "Status: $returnStatus"
                    else                            -> subtitle
                },
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF475569))
            )

            // Inline short errors
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

            // Full detail (HTTP + raw body) collapsible
            if (status == PartStatus.FAILED || returnStatus.equals("ERROR", true)) {
                Spacer(Modifier.height(6.dp))
                ExpandableErrorBox(
                    header = "View full error",
                    detail = buildString {
                        httpCode?.let { appendLine("HTTP Code: $it") }
                        url?.let { appendLine("URL: $it") }
                        exception?.let { appendLine("Exception: $it") }
                        if (!errorBody.isNullOrBlank()) {
                            appendLine("\n--- Error Body ---")
                            appendLine(errorBody)
                        }
                    }.trim().ifEmpty { "No additional error details provided." }
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
