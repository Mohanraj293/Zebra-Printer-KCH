// app/src/main/java/com/lazymohan/zebraprinter/scan/ScanDeliverySlipScreen.kt
package com.lazymohan.zebraprinter.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.CornerRadius

@Composable
fun ScanDeliverySlipScreen(
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onCapture: () -> Unit
) {
    val gradient = Brush.verticalGradient(
        listOf(Color(0xFF0E63FF), Color(0xFF5AA7FF))
    )

    Scaffold(
        containerColor = Color(0xFFF6F8FF),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gradient)
                    .padding(top = 28.dp, start = 24.dp, end = 24.dp, bottom = 18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        color = Color.White
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "KCH",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = Color(0xFF0E63FF),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Scan Delivery Slip",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Position document within camera frame",
                        color = Color.White.copy(alpha = 0.95f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Camera frame card
            Surface(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .offset(y = (-18).dp),
                color = Color.White,
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 1.dp,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                        .heightIn(min = 320.dp)
                        .background(Color.Black, RoundedCornerShape(24.dp))
                        .padding(16.dp)
                        .dashedBorder(
                            color = Color(0xFFCCCCCC),
                            cornerRadius = 16.dp,
                            strokeWidth = 2.dp,
                            onBlack = true
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = Color.LightGray
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Align delivery slip within the frame\nTap to capture",
                            color = Color(0xFFE6E6E6),
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Tip card
            Surface(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFFFF3CD)
            ) {
                Row(
                    Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD27C)),
                        contentAlignment = Alignment.Center
                    ) { Text("📍") }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Ensure good lighting and document is clearly visible",
                        color = Color(0xFF8B3A1A),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Bottom actions
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    onClick = onBack,
                ) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Back")
                }

                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    onClick = onCapture,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                ) {
                    Text(
                        "Capture & Process",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}

/** dashed border helper */
private fun Modifier.dashedBorder(
    color: Color,
    cornerRadius: Dp,
    strokeWidth: Dp,
    onBlack: Boolean = false
): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
        val stroke = Stroke(
            width = strokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
        )
        drawRoundRect(
            color = color.copy(alpha = if (onBlack) 0.7f else 1f),
            cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
            style = stroke
        )
    }
)
