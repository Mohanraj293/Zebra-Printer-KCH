// app/src/main/java/com/lazymohan/zebraprinter/grn/ui/GrnScreens.kt
package com.lazymohan.zebraprinter.grn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
        Column(Modifier.fillMaxSize().padding(inner)) {
            Header(
                gradient,
                when (ui.step) {
                    GrnStep.ENTER_PO -> "Enter PO Number"
                    GrnStep.SHOW_PO  -> "Confirm & Receive"
                    GrnStep.REVIEW   -> "Review & Submit"
                    GrnStep.SUMMARY  -> "Success!"
                    else             -> "Confirm & Receive"
                },
                when (ui.step) {
                    GrnStep.ENTER_PO -> "Start a new GRN by entering the PO number"
                    GrnStep.SHOW_PO  -> "Expand a line, enter Qty/Lot/Expiry"
                    GrnStep.REVIEW   -> "Send the receipt request to Oracle"
                    GrnStep.SUMMARY  -> "Goods Receipt Note Created"
                    else             -> "Expand a line, enter Qty/Lot/Expiry"
                },
                onLogoClick = onBack
            )

            when (ui.step) {
                GrnStep.ENTER_PO -> EnterPoCard(ui, onEnterPo, onFetchPo, onBack)
                GrnStep.SHOW_PO  -> PoAndReceiveCard(ui, onBack, onUpdateLine, onReview)
                GrnStep.REVIEW   -> ReviewCard(ui, onSubmit)
                GrnStep.SUMMARY  -> SummaryCard(ui, onStartOver)
                else             -> Unit
            }
        }
    }
}

@Composable
private fun Header(
    gradient: Brush,
    title: String,
    subtitle: String,
    onLogoClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient)
            .padding(top = 28.dp, start = 24.dp, end = 24.dp, bottom = 18.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onLogoClick() },
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "KCH",
                        color = Color(0xFF0E63FF),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                title,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                color = Color.White.copy(alpha = 0.95f),
                style = MaterialTheme.typography.bodyMedium
            )
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
            if (ui.error != null) { Spacer(Modifier.height(8.dp)); Text(ui.error, color = Color(0xFFB00020)) }
        }
    }
}

/** Single page: PO header + expandable lines (with inputs) */
@Composable
private fun PoAndReceiveCard(
    ui: GrnUiState,
    onBack: () -> Unit,
    onUpdateLine: (Int, Double?, String?, String?) -> Unit,
    onReview: () -> Unit
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {

        // PO header
        Card(
            modifier = Modifier.padding(horizontal = 20.dp).offset(y = (-18).dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                SectionHeader("Purchase Order Details", badge = "EDITABLE", badgeColor = Color(0xFFFFE7A3))
                val po = ui.po
                if (po == null) {
                    Spacer(Modifier.height(8.dp))
                    Text("Loading PO…", color = Color(0xFF6B7280))
                } else {
                    ReadField("PO Number:", po.OrderNumber)
                    ReadField("Vendor:", po.Supplier)
                    ReadField("Site:", po.SupplierSite)
                    ReadField("Business Unit:", po.ProcurementBU)
                    ReadField("Sold To:", po.SoldToLegalEntity)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Lines + inputs inside each expandable card
        Card(
            modifier = Modifier.padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                SectionHeader("Lines", badge = "ENTER DETAILS", badgeColor = Color(0xFFE3F2FD))

                if (ui.lines.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Loading lines…", color = Color(0xFF6B7280))
                } else {
                    ui.lines.forEach { ln ->
                        var expanded by rememberSaveable(ln.LineNumber) { mutableStateOf(false) }

                        val li = ui.lineInputs.firstOrNull { it.lineNumber == ln.LineNumber }
                        var qtyText by rememberSaveable("${ln.LineNumber}-qty") {
                            mutableStateOf(if ((li?.qty ?: 0.0) == 0.0) "" else (li?.qty?.toString() ?: ""))
                        }
                        var lotText by rememberSaveable("${ln.LineNumber}-lot") { mutableStateOf(li?.lot.orEmpty()) }
                        var expText by rememberSaveable("${ln.LineNumber}-exp") { mutableStateOf(li?.expiry.orEmpty()) }

                        Spacer(Modifier.height(10.dp))
                        Surface(
                            tonalElevation = 2.dp,
                            shadowElevation = 2.dp,
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFFDFEFF),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(14.dp)) {

                                // Header: Description -> Item code -> Ordered
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expanded = !expanded }
                                ) {
                                    val itemCode = ln.Item?.takeIf { it.isNotBlank() } ?: "NA"
                                    val title = (ln.Description ?: "").ifBlank { "Item ${itemCode}" }
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFF143A7B),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Item Code: ${itemCode}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF334155)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Ordered: ${fmt(ln.Quantity)} ${ln.UOM}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF6B7280)
                                    )
                                }

                                if (expanded) {
                                    Spacer(Modifier.height(12.dp))
                                    Divider()
                                    Spacer(Modifier.height(12.dp))

                                    // Details block (kept concise)
                                    if (!ln.Description.isNullOrBlank())
                                        ReadFieldInline("Description", ln.Description!!)

                                    ReadFieldInline("Line #", ln.LineNumber.toString())
                                    ReadFieldInline("UOM", ln.UOM)

                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Enter Received Details",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color(0xFF143A7B)
                                    )
                                    Spacer(Modifier.height(6.dp))

                                    OutlinedTextField(
                                        value = qtyText,
                                        onValueChange = {
                                            qtyText = it
                                            onUpdateLine(ln.LineNumber, it.toDoubleOrNull(), null, null)
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        label = { Text("Quantity (≤ ${fmt(ln.Quantity)})") },
                                        singleLine = true,
                                        supportingText = {
                                            val q = qtyText.toDoubleOrNull() ?: 0.0
                                            if (q > ln.Quantity) Text("Cannot exceed ordered qty.", color = Color(0xFFB00020))
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = lotText,
                                        onValueChange = {
                                            lotText = it
                                            onUpdateLine(ln.LineNumber, null, it, null)
                                        },
                                        label = { Text("Lot Number") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = expText,
                                        onValueChange = {
                                            expText = it
                                            onUpdateLine(ln.LineNumber, null, null, it)
                                        },
                                        label = { Text("Expiry (YYYY-MM-DD)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(52.dp)) { Text("Back") }
                    Button(
                        onClick = onReview,
                        enabled = ui.lineInputs.any { it.qty > 0.0 && it.lot.isNotBlank() && it.expiry.isNotBlank() },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                    ) { Text("Review & Submit", color = Color.White) }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
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

@Composable private fun ReadFieldInline(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("$label:", modifier = Modifier.width(120.dp), color = Color(0xFF6B7280), style = MaterialTheme.typography.labelMedium)
        Text(value, color = Color(0xFF0F172A), style = MaterialTheme.typography.bodyMedium)
    }
}

private fun fmt(d: Double): String = NumberFormat.getNumberInstance(Locale.US).format(d)
