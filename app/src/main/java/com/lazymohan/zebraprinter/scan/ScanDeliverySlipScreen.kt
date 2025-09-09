package com.lazymohan.zebraprinter.scan

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.launch

@Composable
fun ScanDeliverySlipScreen(
    onBack: () -> Unit,
    isFromPickSlip: Boolean
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val gradient = Brush.verticalGradient(listOf(Color(0xFF0E63FF), Color(0xFF5AA7FF)))
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val result = GmsDocumentScanningResult.fromActivityResultIntent(res.data)
            val pages = result?.pages?.mapNotNull { it.imageUri } ?: emptyList()
            if (pages.isNotEmpty()) {
                ctx.startActivity(
                    Intent(ctx, ScanResultActivity::class.java).apply {
                        putStringArrayListExtra(
                            ScanResultActivity.EXTRA_PAGES,
                            ArrayList(pages.map { it.toString() })
                        )
                        putExtra("isFromPickSlip", isFromPickSlip)
                    }
                )
            } else scope.launch { snackbarHostState.showSnackbar("No pages captured") }
        } else {
            scope.launch { snackbarHostState.showSnackbar("Scan canceled") }
        }
    }

    fun startScanner() {
        val activity = (ctx as? Activity) ?: run {
            scope.launch { snackbarHostState.showSnackbar("Scanner error: no Activity context") }
            return
        }
        val opts = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .build()
        GmsDocumentScanning.getClient(opts)
            .getStartScanIntent(activity)
            .addOnSuccessListener { sender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(sender).build())
            }
            .addOnFailureListener { e ->
                scope.launch { snackbarHostState.showSnackbar("Scanner error: ${e.message}") }
            }
    }

    Scaffold(
        containerColor = Color(0xFFF6F8FF),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { inner ->
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(inner)
        ) {
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
                    ) { Box(contentAlignment = Alignment.Center) {
                        Text("KCH", style = MaterialTheme.typography.titleLarge.copy(color = Color(0xFF0E63FF), fontWeight = FontWeight.Bold))
                    } }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (isFromPickSlip) "Scan Pick SLip" else "Scan Delivery Slip",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Tap “Capture & Process” to open the camera or import from Gallery.",
                        color = Color.White.copy(alpha = 0.95f), textAlign = TextAlign.Center
                    )
                }
            }

            // Instructions (no camera frame)
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                InstructionItem(emoji = "🧻", title = "Clean & flatten", desc = "Place the slip on a flat, contrasting background.")
                InstructionItem(emoji = "💡", title = "Good lighting", desc = "Avoid glare/shadows; keep the whole slip visible.")
                InstructionItem(emoji = "📐", title = "Fill the screen", desc = "Capture all four corners; use auto-edge detection.")
                InstructionItem(emoji = "🖼️", title = "Multi-page", desc = "Add pages if the slip has a back side.")
            }

            Spacer(Modifier.weight(1f))

            // Actions
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(56.dp)) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Back")
                }
                Button(
                    onClick = { startScanner() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                ) { Text("Capture & Process", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun InstructionItem(emoji: String, title: String, desc: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFEFF3FF)), contentAlignment = Alignment.Center) {
                Text(emoji)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Text(desc, style = MaterialTheme.typography.bodySmall, color = Color(0xFF5C6370))
            }
        }
    }
}
