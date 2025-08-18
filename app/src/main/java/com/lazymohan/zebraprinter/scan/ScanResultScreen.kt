// app/src/main/java/com/lazymohan/zebraprinter/scan/ScanResultScreen.kt
@file:OptIn(ExperimentalMaterial3Api::class)

package com.lazymohan.zebraprinter.scan

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.lazymohan.zebraprinter.scan.data.ExtractedItem
import com.lazymohan.zebraprinter.scan.ui.ScanResultViewModel
import com.lazymohan.zebraprinter.scan.ui.ScanUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

// ---------- helpers ----------
private suspend fun saveUriToGallery(
    context: Context,
    source: Uri,
    fileBaseName: String
): Boolean = withContext(Dispatchers.IO) {
    try {
        val resolver = context.contentResolver
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val name = "$fileBaseName.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/KCH")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val itemUri = resolver.insert(collection, values) ?: return@withContext false
        resolver.openOutputStream(itemUri)?.use { out ->
            resolver.openInputStream(source)?.use { `in` -> `in`.copyTo(out) } ?: return@withContext false
        }
        if (Build.VERSION.SDK_INT >= 29) {
            values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)
        }
        true
    } catch (_: Exception) { false }
}

@Composable
private fun SavedBanner(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -40 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -40 }),
        modifier = modifier
    ) {
        Surface(color = Color(0xFF1DB954), shape = RoundedCornerShape(24.dp), shadowElevation = 6.dp) {
            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Saved to Gallery", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ---------- UI ----------
@Composable
fun ScanResultScreen(
    pages: List<Uri>,
    onBack: () -> Unit,
    onRetake: () -> Unit,
    viewModel: ScanResultViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val state by viewModel.state.collectAsState()

    var selected by remember { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    var showSavedBanner by remember { mutableStateOf(false) }

    val gradient = Brush.verticalGradient(listOf(Color(0xFF0E63FF), Color(0xFF5AA7FF)))

    Scaffold(containerColor = Color(0xFFF6F8FF), snackbarHost = { SnackbarHost(hostState = snack) }) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            Column(Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(gradient)
                        .padding(top = 28.dp, start = 24.dp, end = 24.dp, bottom = 18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(modifier = Modifier.size(72.dp), shape = RoundedCornerShape(20.dp), color = Color.White) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("KCH", style = MaterialTheme.typography.titleLarge.copy(color = Color(0xFF0E63FF), fontWeight = FontWeight.Bold))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Scan Result", color = Color.White, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(6.dp))
                        val sub = when (state) {
                            is ScanUiState.Uploading -> "Uploading / Processing…"
                            is ScanUiState.Completed -> "Scanning completed"
                            is ScanUiState.Error -> "Scanning failed"
                            else -> "Ready"
                        }
                        Text(sub, color = Color.White.copy(alpha = 0.95f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    }
                }

                // Content
                Column(modifier = Modifier.weight(1f)) {

                    // Preview card
                    Surface(
                        modifier = Modifier.padding(horizontal = 20.dp).offset(y = (-18).dp),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        tonalElevation = 1.dp,
                        shadowElevation = 8.dp
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            if (pages.isEmpty()) {
                                Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
                                    Text("No image. Go back and scan again.")
                                }
                            } else {
                                Image(
                                    painter = rememberAsyncImagePainter(pages[selected]),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp, max = 520.dp),
                                    contentScale = ContentScale.Fit
                                )
                                if (pages.size > 1) {
                                    Spacer(Modifier.height(12.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        itemsIndexed(pages) { idx, uri ->
                                            ElevatedCard(onClick = { selected = idx }, modifier = Modifier.size(width = 96.dp, height = 120.dp)) {
                                                Image(painter = rememberAsyncImagePainter(uri), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Status / Result area
                    when (val s = state) {
                        is ScanUiState.Uploading -> {
                            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Spacer(Modifier.height(8.dp))
                                Text("Please wait… this may take a few seconds.", color = Color(0xFF5C6370))
                            }
                        }

                        is ScanUiState.Error -> {
                            Surface(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFFFF1F0)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Upload/Processing Error", style = MaterialTheme.typography.titleMedium, color = Color(0xFFB00020), fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(8.dp))
                                    Text(s.message)
                                    s.raw?.let { raw ->
                                        Spacer(Modifier.height(8.dp))
                                        Text(raw, color = Color(0xFF6B7280))
                                    }
                                }
                            }
                        }

                        is ScanUiState.Completed -> {
                            val data = s.data
                            val xt = data.extractedText

                            // Summary card
                            Surface(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White,
                                tonalElevation = 1.dp
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Invoice Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(6.dp))
                                    Text("Invoice No: ${xt?.invoiceNo ?: "-"}")
                                    Text("Invoice Date: ${xt?.invoiceDate ?: "-"}")
                                    Text("PO No: ${xt?.poNo ?: "-"}")
                                }
                            }

                            // Items list
                            Surface(
                                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White,
                                tonalElevation = 1.dp
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Extracted Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(8.dp))
                                    val items: List<ExtractedItem> = xt?.items ?: emptyList()
                                    if (items.isEmpty()) {
                                        Text("No items detected.")
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(items) { item ->
                                                Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp) {
                                                    Column(Modifier.padding(12.dp)) {
                                                        Text(item.description ?: "-", fontWeight = FontWeight.SemiBold)
                                                        Spacer(Modifier.height(4.dp))
                                                        Text("Qty: ${item.qtyDelivered ?: "-"}")
                                                        Text("Batch: ${item.batchNo ?: "-"}")
                                                        Text("Expiry: ${item.expiryDate ?: "-"}")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> {}
                    }
                }

                // Bottom actions
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) { Text("Back") }

                    Button(
                        onClick = {
                            if (pages.isNotEmpty()) {
                                val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
                                val fileBase = "KCH_${ts}_p${selected + 1}"
                                scope.launch {
                                    saving = true
                                    val ok = saveUriToGallery(ctx, pages[selected], fileBase)
                                    saving = false
                                    if (ok) {
                                        showSavedBanner = true
                                        delay(1500)
                                        showSavedBanner = false
                                    } else {
                                        snack.showSnackbar("Save failed")
                                    }
                                }
                            }
                        },
                        enabled = pages.isNotEmpty(),
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                    ) { Text("Download", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }

            // Overlays
            SavedBanner(
                visible = showSavedBanner,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
            )

            // Centered loader overlay for upload/poll/saving
            val showLoader = saving || state is ScanUiState.Uploading
            if (showLoader) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Surface(tonalElevation = 6.dp, shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(10.dp))
                            Text("Please wait…")
                        }
                    }
                }
            }
        }
    }
}
