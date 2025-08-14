// app/src/main/java/com/lazymohan/zebraprinter/scan/ScanResultScreen.kt
@file:OptIn(ExperimentalMaterial3Api::class)

package com.lazymohan.zebraprinter.scan

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch

@Composable
fun ScanResultScreen(
    pages: List<Uri>,
    onBack: () -> Unit,
    onRetake: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    var selected by remember { mutableIntStateOf(0) }
    var processing by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf<String?>(null) }
    var apiJson by remember { mutableStateOf<String?>(null) } // set when you wire the API

    // NEW: dialog toggles + scroll state
    var showTextDialog by remember { mutableStateOf(false) }
    var showJsonDialog by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    fun extractWithMlKit() {
        if (pages.isEmpty()) return
        processing = true
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromFilePath(ctx, pages[selected])
        recognizer.process(image)
            .addOnSuccessListener { result ->
                recognizedText = result.text.ifBlank { "(No text detected)" }
                processing = false
                // If you want to auto-go home after extraction succeeds, uncomment:
                // val i = Intent(ctx, com.lazymohan.zebraprinter.landing.LandingActivity::class.java)
                //     .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                // ctx.startActivity(i)
            }
            .addOnFailureListener { e ->
                processing = false
                scope.launch { snack.showSnackbar("Extract failed: ${e.message}") }
            }
    }

    val gradient = Brush.verticalGradient(listOf(Color(0xFF0E63FF), Color(0xFF5AA7FF)))

    Scaffold(
        containerColor = Color(0xFFF6F8FF),
        snackbarHost = { SnackbarHost(hostState = snack) }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            // Header (same style as Scan screen)
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
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(20.dp),
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
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Scan Result",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Preview pages and extract text or JSON.",
                        color = Color.White.copy(alpha = 0.95f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // SCROLLABLE CONTENT (keeps bottom buttons visible)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scroll)
            ) {
                // Preview card
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .offset(y = (-18).dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    tonalElevation = 1.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(Modifier.padding(16.dp)) {
                        if (pages.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp),
                                contentAlignment = Alignment.Center
                            ) { Text("No image. Go back and scan again.") }
                        } else {
                            Image(
                                painter = rememberAsyncImagePainter(pages[selected]),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 280.dp, max = 520.dp),
                                contentScale = ContentScale.Fit
                            )
                            if (pages.size > 1) {
                                Spacer(Modifier.height(12.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    itemsIndexed(pages) { idx, uri ->
                                        ElevatedCard(
                                            onClick = { selected = idx },
                                            modifier = Modifier.size(width = 96.dp, height = 120.dp)
                                        ) {
                                            Image(
                                                painter = rememberAsyncImagePainter(uri),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Recognized text card (tap to open dialog)
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .clickable { showTextDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    tonalElevation = 1.dp,
                    shadowElevation = 2.dp
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Recognized Text",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        val body = recognizedText ?: "Press “Extract Text” to read the document on-device."
                        Text(body, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                        Spacer(Modifier.height(6.dp))
                        Text("Tap to expand", style = MaterialTheme.typography.bodySmall, color = Color(0xFF5C6370))
                    }
                }

                // API JSON card (tap to open dialog)
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 8.dp)
                        .clickable { showJsonDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    tonalElevation = 1.dp,
                    shadowElevation = 2.dp
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Extraction JSON",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        val json = apiJson ?: "API not integrated yet. Here the JSON will be coming."
                        Text(json, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                        Spacer(Modifier.height(6.dp))
                        Text("Tap to expand", style = MaterialTheme.typography.bodySmall, color = Color(0xFF5C6370))
                    }
                }
            }

            // Bottom actions (always visible)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) { Text("Back") }

                Button(
                    onClick = { extractWithMlKit() },
                    enabled = !processing && pages.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                ) { Text("Extract Text", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }

        if (processing) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Extracting…")
                    }
                }
            }
        }

        // POPUP dialogs
        if (showTextDialog) {
            AlertDialog(
                onDismissRequest = { showTextDialog = false },
                confirmButton = {
                    TextButton(onClick = { showTextDialog = false }) { Text("Close") }
                },
                title = { Text("Recognized Text") },
                text = {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 480.dp)
                            .verticalScroll(rememberScrollState())
                    ) { Text(recognizedText ?: "(No text yet)") }
                }
            )
        }

        if (showJsonDialog) {
            AlertDialog(
                onDismissRequest = { showJsonDialog = false },
                confirmButton = {
                    TextButton(onClick = { showJsonDialog = false }) { Text("Close") }
                },
                title = { Text("Extraction JSON") },
                text = {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 480.dp)
                            .verticalScroll(rememberScrollState())
                    ) { Text(apiJson ?: "API not integrated yet. Here the JSON will be coming.") }
                }
            )
        }
    }
}
