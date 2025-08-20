package com.lazymohan.zebraprinter.grn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/* -------------------- Reusable Banners -------------------- */

@Composable
fun InfoBanner(text: String) {
    Surface(color = Color(0xFFEFF4FF), shape = RoundedCornerShape(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Description, contentDescription = null, tint = Color(0xFF2E6BFF))
            Spacer(Modifier.width(8.dp))
            Text(text, color = Color(0xFF143A7B))
        }
    }
}

/* -------------------- Section Headers -------------------- */

@Composable
fun SectionHeader(
    title: String,
    badge: String? = null,
    badgeColor: Color = Color(0xFFEFF4FF)
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .padding(start = 20.dp, top = 12.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFEFF4FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Description, contentDescription = null, tint = Color(0xFF2E6BFF))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge.copy(
                color = Color(0xFF143A7B),
                fontWeight = FontWeight.ExtraBold
            )
        )
        if (badge != null) {
            Spacer(Modifier.width(8.dp))
            Surface(color = badgeColor, shape = RoundedCornerShape(20.dp)) {
                Text(
                    badge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = Color(0xFF495057),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

/* -------------------- Chips -------------------- */

@Composable
fun Chip(text: String) {
    Surface(color = Color(0xFFEFF4FF), shape = RoundedCornerShape(20.dp)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = Color(0xFF2E6BFF),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/* -------------------- Read-only fields -------------------- */

@Composable
fun ReadField(label: String, value: String) {
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(140.dp), color = Color(0xFF6B7280))
        Text(
            value,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F172A)
        )
    }
}

@Composable
fun ReadFieldInline(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "$label:",
            modifier = Modifier.width(120.dp),
            color = Color(0xFF6B7280),
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            value,
            color = Color(0xFF0F172A),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/* -------------------- Receipt Row -------------------- */

@Composable
fun ReceiptRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), color = Color(0xFF6B7280))
        Text(
            value,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
    Spacer(Modifier.height(6.dp))
}

/* -------------------- Input Fields -------------------- */

@Composable
fun LabeledText(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    enabled: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun LabeledNumber(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    errorText: String?,
    enabled: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
        ),
        label = { Text(label) },
        isError = errorText != null,
        singleLine = true,
        enabled = enabled,
        supportingText = {
            if (errorText != null) Text(errorText, color = Color(0xFFB00020))
        },
        modifier = Modifier.fillMaxWidth()
    )
}
