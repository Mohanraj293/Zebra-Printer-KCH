package com.lazymohan.zebraprinter.grn.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

@Composable
fun EnterPoCard(
    ui: GrnUiState,
    onEnterPo: (String) -> Unit,
    onFetchPo: () -> Unit,
    onBack: () -> Unit
) {
    val hasError = ui.error?.isNotBlank() == true

    Card(
        modifier = Modifier
            .padding(20.dp)
            .offset(y = (-12).dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(Modifier.padding(20.dp)) {

            OutlinedTextField(
                value = ui.poNumber,
                onValueChange = { onEnterPo(it.trim()) },
                label = { Text("PO Number") },
                placeholder = { Text("e.g., KHQ/PO/99387") },
                singleLine = true,
                isError = hasError,
                supportingText = {
                    if (hasError) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.width(6.dp))
                        }
                    } else {
                        Text(
                            "Enter the exact Oracle PO number.",
                            color = Color(0xFF64748B)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            AnimatedVisibility(visible = hasError) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ErrorOutline, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "PO not found in Oracle Fusion.",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                )
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SmallTipChip("Check typos")
                            SmallTipChip("Use exact slashes (/)")
                            SmallTipChip("Remove spaces")
                            SmallTipChip("Example: KHQ/PO/98840")
                        }
                    }
                }
            }


            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) { Text("Back") }

                Button(
                    onClick = onFetchPo,
                    enabled = ui.poNumber.isNotBlank() && !ui.loading,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                ) {
                    if (ui.loading) {
                        CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Next", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallTipChip(text: String) {
    Surface(
        color = Color.White.copy(alpha = 0.35f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
