package com.lazymohan.zebraprinter.grn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun Header(
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
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                title,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                color = Color.White.copy(alpha = 0.95f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun Stepper(step: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .offset(y = (-10).dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("PO", "Receive", "Review", "Done").forEachIndexed { idx, label ->
            val active = (idx + 1) <= step
            val bg = if (active) Color(0xFFE8F0FF) else Color(0xFFF2F4F7)
            val fg = if (active) Color(0xFF0E63FF) else Color(0xFF64748B)
            Surface(color = bg, shape = RoundedCornerShape(18.dp)) {
                Text(
                    label,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = fg,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
