// grn/ui/to/EnterToCard.kt
package com.lazymohan.zebraprinter.grn.ui.to

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun EnterToCard(
    ui: ToUiState,
    onEnter: (String) -> Unit,
    onFetch: () -> Unit,
    onBack: () -> Unit
) {
    val hasError = ui.error?.isNotBlank() == true

    Card(
        modifier = Modifier.padding(20.dp).offset(y = (-12).dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(Modifier.padding(20.dp)) {

            OutlinedTextField(
                value = ui.toNumber,
                onValueChange = { onEnter(it.trim()) },
                label = { Text("TO Number") },
                placeholder = { Text("e.g., 107054") },
                singleLine = true,
                isError = hasError,
                supportingText = {
                    if (hasError) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ErrorOutline, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(ui.error.orEmpty())
                        }
                    } else {
                        Text("Enter the exact Transfer Order number.", color = Color(0xFF64748B))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            AnimatedVisibility(visible = hasError) {
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(52.dp)
                ) { Text("Back") }

                Button(
                    onClick = onFetch,
                    enabled = ui.toNumber.isNotBlank() && !ui.loading,
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                ) {
                    if (ui.loading) {
                        CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Next", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
