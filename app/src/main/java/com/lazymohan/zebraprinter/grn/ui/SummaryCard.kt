package com.lazymohan.zebraprinter.grn.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SummaryCard(
    ui: GrnUiState,
    onStartOver: () -> Unit
) {
    val r = ui.receipt
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF2ECC71),
                modifier = Modifier.size(96.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "GRN Successfully Created",
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall.copy(
                color = Color(0xFF0E9F6E),
                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
            )
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
                    Text(
                        "Line-Level Errors",
                        color = Color(0xFFB00020),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    ui.lineErrors.forEach {
                        Text(
                            "• ${it.ItemDescription ?: "-"}: ${it.ErrorMessage ?: ""}",
                            color = Color(0xFFB00020),
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onStartOver,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                ) {
                    Text("Start New Receipt", color = Color.White)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}
