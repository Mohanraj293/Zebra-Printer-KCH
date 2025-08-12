// app/src/main/java/com/lazymohan/zebraprinter/grn/ui/GrnScreens.kt
package com.lazymohan.zebraprinter.grn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lazymohan.zebraprinter.grn.data.*
import java.text.NumberFormat
import java.util.*

@Composable
fun GrnScreens(
    ui: GrnUiState,
    snackbarHostState: SnackbarHostState,
    onEnterPo: (String) -> Unit,
    onFetchPo: () -> Unit,
    onUpdateLine: (Int, Double?, String?, String?) -> Unit,
    onReview: () -> Unit,
    onSubmit: () -> Unit,
    onStartOver: () -> Unit,
    onBack: () -> Unit
) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF0E63FF), Color(0xFF5AA7FF)))
    Scaffold(
        containerColor = Color(0xFFF6F8FF),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            // header
            Header(gradient, when (ui.step) {
                GrnStep.ENTER_PO -> "Enter PO Number"
                GrnStep.SHOW_PO -> "Confirm Details"
                GrnStep.RECEIVE_LINES -> "Receive Items"
                GrnStep.REVIEW -> "Review & Submit"
                GrnStep.SUMMARY -> "Success!"
            }, when (ui.step) {
                GrnStep.ENTER_PO -> "Start a new GRN by entering the PO number"
                GrnStep.SHOW_PO -> "Review and edit extracted information"
                GrnStep.RECEIVE_LINES -> "Enter quantity, lot and expiry for each line"
                GrnStep.REVIEW -> "Send the receipt request to Oracle"
                GrnStep.SUMMARY -> "Goods Receipt Note Created"
            })

            when (ui.step) {
                GrnStep.ENTER_PO -> EnterPoCard(ui, onEnterPo, onFetchPo, onBack)
                GrnStep.SHOW_PO -> PoDetailsCard(ui, onProceed = { /* lines are auto-loaded -> */ })
                GrnStep.RECEIVE_LINES -> ReceiveLinesCard(ui, onUpdateLine, onReview)
                GrnStep.REVIEW -> ReviewCard(ui, onSubmit)
                GrnStep.SUMMARY -> SummaryCard(ui, onStartOver)
            }
        }
    }
}

@Composable
private fun Header(gradient: Brush, title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient)
            .padding(top = 28.dp, start = 24.dp, end = 24.dp, bottom = 18.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)),
                color = Color.White
            ) { Box(contentAlignment = Alignment.Center) { Text("KCH", color = Color(0xFF0E63FF), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) } }
            Spacer(Modifier.height(14.dp))
            Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold))
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.95f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EnterPoCard(
    ui: GrnUiState,
    onEnterPo: (String) -> Unit,
    onFetchPo: () -> Unit,
    onBack: () -> Unit
) {
    Card(
        modifier = Modifier.padding(20.dp).offset(y = (-18).dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            OutlinedTextField(
                value = ui.poNumber,
                onValueChange = onEnterPo,
                label = { Text("PO Number") },
                placeholder = { Text("PO-2024-3456") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(52.dp)) { Text("Back") }
                Button(
                    onClick = onFetchPo,
                    enabled = ui.poNumber.isNotBlank() && !ui.loading,
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                ) { if (ui.loading) CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White) else Text("Next", color = Color.White) }
            }
            if (ui.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(ui.error, color = Color(0xFFB00020))
            }
        }
    }
}

@Composable
private fun PoDetailsCard(ui: GrnUiState, onProceed: () -> Unit) {
    val po = ui.po ?: return
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Card(
            modifier = Modifier.padding(horizontal = 20.dp).offset(y = (-18).dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                SectionHeader("Extracted Information", badge = "EDITABLE", badgeColor = Color(0xFFFFE7A3))
                ReadField("PO Number:", po.OrderNumber)
                ReadField("Vendor:", po.Supplier)
                ReadField("Site:", po.SupplierSite)
                ReadField("Business Unit:", po.ProcurementBU)
                ReadField("Sold To:", po.SoldToLegalEntity)
            }
        }
        Spacer(Modifier.height(10.dp))
        // auto-proceeds to lines once loaded; keep the card for visual parity
    }
}

@Composable
private fun ReceiveLinesCard(
    ui: GrnUiState,
    onUpdateLine: (Int, Double?, String?, String?) -> Unit,
    onReview: () -> Unit
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Card(
            modifier = Modifier.padding(horizontal = 20.dp).offset(y = (-18).dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                SectionHeader("Receive Items", badge = "REQUIRED", badgeColor = Color(0xFFE3F2FD))
                ui.lineInputs.forEach { ln ->
                    Spacer(Modifier.height(10.dp))
                    Text("Line ${ln.lineNumber} • ${ln.itemNumber}", fontWeight = FontWeight.Bold, color = Color(0xFF133A7B))
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = if (ln.qty == 0.0) "" else ln.qty.toString(),
                        onValueChange = { v -> onUpdateLine(ln.lineNumber, v.toDoubleOrNull() ?: 0.0, null, null) },
                        label = { Text("Quantity (≤ ${fmt(ln.maxQty)})") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = ln.lot,
                        onValueChange = { onUpdateLine(ln.lineNumber, null, it, null) },
                        label = { Text("Lot Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = ln.expiry,
                        onValueChange = { onUpdateLine(ln.lineNumber, null, null, it) },
                        label = { Text("Expiry (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Divider(Modifier.padding(vertical = 10.dp))
                }
                Button(
                    onClick = onReview,
                    enabled = ui.lineInputs.any { it.qty > 0.0 && it.lot.isNotBlank() && it.expiry.isNotBlank() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                ) { Text("Review & Submit", color = Color.White) }
            }
        }
    }
}

@Composable
private fun ReviewCard(ui: GrnUiState, onSubmit: () -> Unit) {
    val p = ui.payload ?: return
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Card(
            modifier = Modifier.padding(horizontal = 20.dp).offset(y = (-18).dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                SectionHeader("Purchase Order Details", badge = "READ-ONLY", badgeColor = Color(0xFFDFF7E6))
                ReadField("PO Number:", p.lines.firstOrNull()?.DocumentNumber ?: "-")
                ReadField("Vendor:", p.VendorName)
                ReadField("Site:", p.VendorSiteCode)
                ReadField("Business Unit:", p.BusinessUnit)
                Spacer(Modifier.height(8.dp))
                Text("Lines", fontWeight = FontWeight.Bold, color = Color(0xFF133A7B))
                p.lines.forEach {
                    Text("• Line ${it.DocumentLineNumber} • ${it.ItemNumber} • Qty ${fmt(it.Quantity)} ${it.UnitOfMeasure}")
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onSubmit,
                    enabled = !ui.loading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                ) { if (ui.loading) CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White) else Text("Create GRN in Oracle", color = Color.White) }
                if (ui.error != null) { Spacer(Modifier.height(8.dp)); Text(ui.error, color = Color(0xFFB00020)) }
            }
        }
    }
}

@Composable
private fun SummaryCard(ui: GrnUiState, onStartOver: () -> Unit) {
    val r = ui.receipt
    Column(Modifier.verticalScroll(rememberScrollState())) {
        // success icon
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF2ECC71), modifier = Modifier.size(96.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "GRN Successfully Created",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall.copy(color = Color(0xFF0E9F6E), fontWeight = FontWeight.ExtraBold)
        )
        Spacer(Modifier.height(10.dp))
        Card(
            modifier = Modifier.padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                SectionHeader("Transaction Summary")
                ReadField("GRN Number:", r?.ReceiptNumber ?: "-")
                ReadField("Status:", r?.ReturnStatus ?: "-")
                if (ui.lineErrors.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Line-Level Errors", color = Color(0xFFB00020), fontWeight = FontWeight.Bold)
                    ui.lineErrors.forEach { Text("• ${it.ItemDescription ?: "-"}: ${it.ErrorMessage ?: ""}", color = Color(0xFFB00020)) }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onStartOver,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                ) { Text("Start New Receipt", color = Color.White) }
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable private fun SectionHeader(title: String, badge: String? = null, badgeColor: Color = Color(0xFFEFF4FF)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.padding(start = 20.dp, top = 12.dp).size(40.dp).clip(CircleShape).background(Color(0xFFEFF4FF)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.Description, contentDescription = null, tint = Color(0xFF2E6BFF)) }
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleLarge.copy(color = Color(0xFF143A7B), fontWeight = FontWeight.ExtraBold))
        if (badge != null) {
            Spacer(Modifier.width(8.dp))
            Surface(color = badgeColor, shape = RoundedCornerShape(20.dp)) {
                Text(badge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = Color(0xFF495057), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable private fun ReadField(label: String, value: String) {
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(140.dp), color = Color(0xFF6B7280))
        Text(value, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
    }
}

private fun fmt(d: Double): String = NumberFormat.getNumberInstance(Locale.US).format(d)
