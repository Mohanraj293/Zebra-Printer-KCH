package com.lazymohan.zebraprinter.grn.ui.to

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lazymohan.zebraprinter.grn.data.ReceiptResponse

@Composable
fun SummaryCardTO(
    response: ReceiptResponse?,
    onStartOver: () -> Unit
) {
    val status = response?.ReturnStatus ?: "-"
    val success = status.equals("SUCCESS", ignoreCase = true)
    val receiptNumber = response?.ReceiptNumber ?: "-"

    Column(Modifier.verticalScroll(rememberScrollState())) {

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (success) Icons.Outlined.CheckCircle else Icons.Outlined.Error,
                contentDescription = null,
                tint = if (success) Color(0xFF22C55E) else Color(0xFFEF4444),
                modifier = Modifier.size(96.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (success) "Receipt Successfully Created" else "Receipt Failed",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall.copy(
                color = if (success) Color(0xFF0E9F6E) else Color(0xFF991B1B),
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
                Text(
                    "Transaction Summary",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color(0xFF143A7B),
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(Modifier.height(8.dp))

                SummaryRow("Return Status:", status)
                SummaryRow("Receipt Number:", receiptNumber)

                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onStartOver,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                ) {
                    Text("Start New Receipt", color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF6B7280))
        Text(value, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
    }
}
