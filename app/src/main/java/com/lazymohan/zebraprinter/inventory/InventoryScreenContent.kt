package com.lazymohan.zebraprinter.inventory

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Domain
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lazymohan.zebraprinter.grn.ui.Header

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun InventoryScreenContent(
    viewModel: InventoryViewModel,
    onBackFromChoose: () -> Unit,
    onScanClick: () -> Unit
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val gradient = Brush.verticalGradient(listOf(Color(0xFF0E63FF), Color(0xFF5AA7FF)))

    Scaffold(
        containerColor = Color(0xFFF6F8FF),
        snackbarHost = { SnackbarHost(hostState = viewModel.snackHost) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF6F8FF))
        ) {
            Header(
                gradient = gradient,
                title = "Physical Inventory",
                subtitle = if (ui.stage == InventoryStage.Choose) "Choose organization" else "Scan items & export",
                onLogoClick = {
                    if (ui.stage == InventoryStage.Actions) viewModel.backToChoose()
                    else onBackFromChoose()
                }
            )

            Spacer(Modifier.height(16.dp))

            AnimatedContent(
                targetState = ui.stage,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                },
                modifier = Modifier.fillMaxSize()
            ) { stage ->
                when (stage) {
                    InventoryStage.Choose -> ChooseOrgCard(
                        org = ui.orgName,
                        subinventory = ui.subinventoryName,
                        onSelect = { viewModel.goToActions() }
                    )
                    InventoryStage.Actions -> ActionsContent(
                        onScanClick = onScanClick,
                        onExportClick = { viewModel.exportCsv() },
                        enabled = ui.onHandLoaded,
                        lines = ui.counts.size,
                        org = ui.orgName,
                        subinventory = ui.subinventoryName
                    )
                }
            }
        }
    }

    if (ui.matchDialog != null) {
        MatchDialog(
            data = ui.matchDialog!!,
            onDismiss = { viewModel.dismissDialog() },
            onSave = { txnId, qty -> viewModel.saveQty(txnId, qty) }
        )
    }
}

@Composable
private fun ChooseOrgCard(
    org: String,
    subinventory: String,
    onSelect: () -> Unit
) {
    Spacer(Modifier.height(16.dp))
    Card(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    color = Color(0xFFEFF4FF)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Outlined.Domain,
                            contentDescription = null,
                            tint = Color(0xFF2E6BFF)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = org,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color(0xFF133A7B),
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subinventory,
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

@Composable
private fun ActionsContent(
    onScanClick: () -> Unit,
    onExportClick: () -> Unit,
    enabled: Boolean,
    lines: Int,
    org: String,
    subinventory: String
) {
    Spacer(Modifier.height(14.dp))

    // Tiny pill showing current org/subinventory at top
    Row(
        Modifier
            .padding(horizontal = 20.dp)
            .clip(CircleShape)
            .background(Color(0xFFEFF4FF))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Domain,
            contentDescription = null,
            tint = Color(0xFF2E6BFF)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "$org • $subinventory",
            style = MaterialTheme.typography.labelLarge.copy(color = Color(0xFF2E3A59))
        )
    }

    Spacer(Modifier.height(16.dp))

    // Big action cards
    Column(Modifier.padding(horizontal = 20.dp)) {
        ActionCard(
            title = "Scan Item",
            subtitle = if (enabled) "Scan a GS1 QR to record a count" else "Loading master list…",
            icon = Icons.Outlined.QrCodeScanner,
            primary = true,
            enabled = enabled,
            onClick = onScanClick
        )
        Spacer(Modifier.height(12.dp))
        ActionCard(
            title = "Export Physical Count CSV",
            subtitle = "Transaction Id, Physical Onhand",
            icon = Icons.Outlined.FileDownload,
            primary = false,
            enabled = true,
            onClick = onExportClick
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    primary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val container = if (primary) Color(0xFF2E6BFF) else Color.White
    val content = if (primary) Color.White else Color(0xFF133A7B)
    val subtitleColor = if (primary) Color.White.copy(alpha = 0.85f) else Color(0xFF6E7791)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = enabled) { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = if (primary) 10.dp else 6.dp),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp)),
                color = if (primary) Color.White.copy(alpha = 0.15f) else Color(0xFFEFF4FF)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (primary) Color.White else Color(0xFF2E6BFF)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = content,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(color = subtitleColor)
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = if (primary) Color.White else Color(0xFF8A94AC)
            )
        }
    }
}

@Composable
private fun MatchDialog(
    data: MatchDialogData,
    onDismiss: () -> Unit,
    onSave: (Long, Int) -> Unit
) {
    var qtyText by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val q = qtyText.toIntOrNull() ?: 1
                    onSave(data.transactionId, q)
                },
                modifier = Modifier.height(44.dp)
            ) { Text("Save", color = Color.White) }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.height(44.dp)
            ) { Text("Cancel") }
        },
        title = { Text("Confirm Physical Count", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                InfoRow("Description", data.description ?: "-")
                InfoRow("GTIN", data.gtin ?: "-")
                InfoRow("Lot", data.lot)
                InfoRow("Expiry", data.expiryIso)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it.filter { c -> c.isDigit() } },
                    label = { Text("Quantity (EA)") },
                    singleLine = true
                )
            }
        }
    )
}


@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF133A7B)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF5B6B8C)
        )
    }
}
