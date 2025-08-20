package com.lazymohan.zebraprinter.landing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LandingScreenContent(
    snackbarHostState: SnackbarHostState,
    onScanDelivery: () -> Unit,
    onPrintQr: () -> Unit,
    onManualGrn: () -> Unit,
    onInProgress: (String) -> Unit,
    userName: String = "Ahmed Al-Rashid",
    userRole: String = "Warehouse Worker - Shift A"
) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF0E63FF), Color(0xFF5AA7FF)))
    val overlap = 28.dp

    Scaffold(
        containerColor = Color(0xFFF6F8FF),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(gradient)
                        .padding(
                            top = 32.dp,
                            start = 24.dp,
                            end = 24.dp,
                            bottom = 24.dp + overlap
                        )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(20.dp)),
                            color = Color.White.copy(alpha = 0.9f)
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
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "King's College Hospital",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Dubai - Warehouse & Pharmacy Management",
                            color = Color.White.copy(alpha = 0.95f),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Welcome card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = -overlap),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .wrapContentHeight(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Welcome, $userName",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color(0xFF143A7B),
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                userRole,
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF687089)),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Warehouse Operations
            item { SectionHeader(title = "Warehouse Operations", emojiBg = Color(0xFF2E6BFF)) }
            item {
                FeatureCard(
                    icon = Icons.Outlined.Smartphone,
                    title = "Scan Delivery Slip",
                    subtitle = "Position document within camera frame",
                    onClick = onScanDelivery
                )
            }
            item {
                FeatureCard(
                    icon = Icons.Outlined.QrCode,
                    title = "Print QR Codes",
                    subtitle = "Generate labels for unlabeled products",
                    onClick = onPrintQr
                )
            }

            // GRN Operations
            item { Spacer(Modifier.height(8.dp)) }
            item { SectionHeader(title = "GRN Operations", emojiBg = Color(0xFF2E6BFF)) }
            item {
                FeatureCard(
                    icon = Icons.Outlined.Description,
                    title = "Manual GRN by PO",
                    subtitle = "Create receipt by entering PO number",
                    onClick = onManualGrn
                )
            }

            // Pharmacy Operations
            item { Spacer(Modifier.height(8.dp)) }
            item { SectionHeader(title = "Pharmacy Operations", emojiBg = Color(0xFF2E6BFF)) }
            item {
                FeatureCard(
                    icon = Icons.Outlined.ReceiptLong,
                    title = "Scan Pick Up Slip",
                    subtitle = "Receive transfers from pharmacy",
                    onClick = { onInProgress("Scan Pick Up Slip") }
                )
            }

            // bottom spacer so last card isn't hidden behind bottom bar
            item { Spacer(Modifier.height(96.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String, emojiBg: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(emojiBg.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(14.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(emojiBg)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color(0xFF153B7C),
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun FeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp)),
                color = Color(0xFFEFF4FF)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF2E6BFF)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color(0xFF133A7B),
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6E7791))
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF8A94AC)
            )
        }
    }
}