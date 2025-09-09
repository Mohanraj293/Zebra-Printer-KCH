// app/src/main/java/com/lazymohan/zebraprinter/scan/ScanResultScreen.kt
@file:OptIn(ExperimentalMaterial3Api::class)

package com.lazymohan.zebraprinter.scan

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.lazymohan.zebraprinter.grn.ui.GrnActivity
import com.lazymohan.zebraprinter.grn.ui.to.ToGrnActivity
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
        val collection =
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val name = "$fileBaseName.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= 29) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/KCH"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val itemUri = resolver.insert(collection, values) ?: return@withContext false
        resolver.openOutputStream(itemUri)?.use { out ->
            resolver.openInputStream(source)?.use { `in` -> `in`.copyTo(out) }
                ?: return@withContext false
        }
        if (Build.VERSION.SDK_INT >= 29) {
            values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)
        }
        true
    } catch (_: Exception) {
        false
    }
}

private suspend fun copyUrisToCache(
    context: Context,
    uris: List<Uri>,
    prefix: String = "delivery_slip_"
): ArrayList<String> = withContext(Dispatchers.IO) {
    val out = arrayListOf<String>()
    val cr = context.contentResolver
    var idx = 1
    for (u in uris) {
        try {
            val ext = when (cr.getType(u)) {
                "image/png" -> "png"
                else -> "jpg" // ML Kit result is usually JPEG
            }
            val file = kotlin.io.path.createTempFile(
                context.cacheDir.toPath(),
                "${prefix}${idx}_",
                ".$ext"
            ).toFile()
            cr.openInputStream(u)?.use { input ->
                file.outputStream().use { outStream -> input.copyTo(outStream) }
            }
            out += file.absolutePath
            idx++
        } catch (_: Exception) {
            // ignore individual copy failures
        }
    }
    out
}


private data class UiRow(
    val description: String,
    val qty: String,
    val batch: String,
    val expiry: String
)

private fun flattenUi(items: List<ExtractedItem>): List<UiRow> = buildList {
    items.forEach { item ->
        val desc = item.description ?: "—"
        val details = item.details
        if (!details.isNullOrEmpty()) {
            details.forEach { d ->
                add(
                    UiRow(
                        description = desc,
                        qty = d.qtyDelivered?.ifBlank { "—" } ?: "—",
                        batch = d.batchNo?.ifBlank { "—" } ?: "—",
                        expiry = d.expiryDate?.ifBlank { "—" } ?: "—"
                    )
                )
            }
        } else {
            add(
                UiRow(
                    description = desc,
                    qty = item.qtyDelivered?.ifBlank { "—" } ?: "—",
                    batch = item.batchNo?.ifBlank { "—" } ?: "—",
                    expiry = item.expiryDate?.ifBlank { "—" } ?: "—"
                )
            )
        }
    }
}

@Composable
private fun SavedBanner(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -40 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -40 }),
        modifier = modifier
    ) {
        Surface(
            color = Color(0xFF1DB954),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
    isFromPickSlip: Boolean = false,
    viewModel: ScanResultViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val state by viewModel.state.collectAsState()

    var selected by remember { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    var showSavedBanner by remember { mutableStateOf(false) }

    val gradient =
        Brush.verticalGradient(listOf(Color(0xFF0E63FF), Color(0xFF5AA7FF)))
    val scrollState = rememberScrollState()

    // Shared action for "Create GRN" button (bottom)
    val launchCreateGrn: () -> Unit = {
        val s = state
        if (s is ScanUiState.Completed) {
            val transfer = s.data.toTransfer()
            if (transfer == null) {
                scope.launch { snack.showSnackbar("No usable OCR data") }
            } else {
                scope.launch {
                    Log.d("ScanResultScreen", "Preparing to launch GrnActivity…")
                    val cachePaths = copyUrisToCache(ctx, pages)
                    Log.d("ScanResultScreen", "Cached ${cachePaths.size} files: $cachePaths")

                    val payload = scanGson.toJson(transfer)
                    Log.d("ScanResultScreen", "Transfer JSON size=${payload.length}")

                    val intentClass = if (isFromPickSlip)
                        Intent(ctx, ToGrnActivity::class.java)
                    else
                        Intent(ctx, GrnActivity::class.java)
                    val intent = intentClass.apply {
                        if (isFromPickSlip) {
                            putExtra("to_number", s.data.extractedText?.orderNumber)
                        } else {
                            putExtra("po_number", transfer.poNumber)
                        }
                        putExtra("scan_extract_json", payload)
                        putStringArrayListExtra("scan_image_cache_paths", cachePaths)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    Log.d("ScanResultScreen", "Launching GrnActivity with pages=${pages.size}")
                    ctx.startActivity(intent)
                    (ctx as? android.app.Activity)?.finish()
                }
            }
        } else {
            scope.launch { snack.showSnackbar("Complete scanning first") }
        }
    }

    val canCreateGrn = state is ScanUiState.Completed

    Scaffold(
        containerColor = Color(0xFFF6F8FF),
        snackbarHost = { SnackbarHost(hostState = snack) }
    ) { inner ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            Column(Modifier.fillMaxSize()) {
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
                            text = "Scan Result",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        val sub = when (state) {
                            is ScanUiState.Uploading -> "Uploading / Processing…"
                            is ScanUiState.Completed -> "Scanning completed"
                            is ScanUiState.Error -> "Scanning failed"
                            is ScanUiState.Idle -> "Ready"
                        }
                        Text(
                            sub,
                            color = Color.White.copy(alpha = 0.95f),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
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
                                    Modifier
                                        .fillMaxWidth()
                                        .height(280.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No image. Go back and scan again.")
                                }
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
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        itemsIndexed(pages) { idx, uri ->
                                            ElevatedCard(
                                                onClick = { selected = idx },
                                                modifier = Modifier.size(
                                                    width = 96.dp,
                                                    height = 120.dp
                                                )
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

                            // Under image: simple Retake + Download row
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onRetake,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) { Text("Retake") }

                                Button(
                                    onClick = {
                                        if (pages.isNotEmpty()) {
                                            val ts = SimpleDateFormat(
                                                "yyyyMMdd_HHmmss",
                                                Locale.US
                                            ).format(System.currentTimeMillis())
                                            val fileBase = "KCH_${ts}_p${selected + 1}"
                                            scope.launch {
                                                saving = true
                                                val ok = saveUriToGallery(
                                                    ctx,
                                                    pages[selected],
                                                    fileBase
                                                )
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
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2E6BFF)
                                    )
                                ) {
                                    Text(
                                        "Download",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Status / Result area
                    when (val s = state) {
                        is ScanUiState.Uploading -> {
                            Column(
                                Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Please wait… this may take a few seconds.",
                                    color = Color(0xFF5C6370)
                                )
                            }
                        }

                        is ScanUiState.Error -> {
                            Surface(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFFFF1F0)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        "Upload/Processing Error",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFFB00020),
                                        fontWeight = FontWeight.SemiBold
                                    )
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
                            val rawItems: List<ExtractedItem> = xt?.items ?: emptyList()
                            val rows: List<UiRow> = flattenUi(rawItems)

                            // Big, polished “invoice” card
                            Surface(
                                modifier = Modifier.padding(
                                    horizontal = 20.dp,
                                    vertical = 12.dp
                                ),
                                shape = RoundedCornerShape(22.dp),
                                color = Color.White,
                                tonalElevation = 1.dp,
                                shadowElevation = 10.dp
                            ) {
                                Column {

                                    // Fancy ribbon header
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        Color(0xFF0E63FF),
                                                        Color(0xFF5AA7FF)
                                                    )
                                                )
                                            )
                                            .padding(horizontal = 16.dp, vertical = 16.dp)
                                    ) {
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Surface(
                                                    color = Color.White.copy(alpha = 0.18f),
                                                    shape = RoundedCornerShape(14.dp)
                                                ) {
                                                    Text(
                                                        "OCR EXTRACT",
                                                        modifier = Modifier.padding(
                                                            horizontal = 10.dp,
                                                            vertical = 4.dp
                                                        ),
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                }
                                                Spacer(Modifier.weight(1f))
                                                Surface(
                                                    color = Color.White.copy(alpha = 0.18f),
                                                    shape = RoundedCornerShape(14.dp)
                                                ) {
                                                    Text(
                                                        text = "${rows.size} item${if (rows.size == 1) "" else "s"}",
                                                        modifier = Modifier.padding(
                                                            horizontal = 10.dp,
                                                            vertical = 4.dp
                                                        ),
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                "Extracted Invoice",
                                                color = Color.White,
                                                style = MaterialTheme.typography.headlineSmall.copy(
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            )
                                            Text(
                                                "Auto-parsed from your delivery slip",
                                                color = Color.White.copy(alpha = 0.9f),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }

                                    // Meta block — invoice details
                                    Column(
                                        Modifier.padding(
                                            horizontal = 16.dp,
                                            vertical = 14.dp
                                        )
                                    ) {
                                        if (isFromPickSlip) {
                                            InvoiceMetaGrid(
                                                left = listOf(
                                                    "TO Number" to (xt?.orderNumber?.ifBlank { "—" }
                                                        ?: "—"),
                                                    "Extracted On" to SimpleDateFormat(
                                                        "dd MMM yyyy, hh:mm a",
                                                        Locale.getDefault()
                                                    ).format(System.currentTimeMillis())
                                                ),
                                                right = emptyList()
                                            )
                                        } else {
                                            InvoiceMetaGrid(
                                                left = listOf(
                                                    "Invoice No." to (xt?.invoiceNo?.ifBlank { "—" }
                                                        ?: "—"),
                                                    "Invoice Date" to (xt?.invoiceDate?.ifBlank { "—" }
                                                        ?: "—")
                                                ),
                                                right = listOf(
                                                    "PO Number" to (xt?.poNo?.ifBlank { "—" }
                                                        ?: "—"),
                                                    "Extracted On" to SimpleDateFormat(
                                                        "dd MMM yyyy, hh:mm a",
                                                        Locale.getDefault()
                                                    ).format(System.currentTimeMillis())
                                                )
                                            )
                                        }
                                    }

                                    Divider(color = Color(0xFFE8ECF5))

                                    // Items table
                                    Column(Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Line Items",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = Color(0xFF143A7B),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                        Spacer(Modifier.height(8.dp))

                                        if (rows.isEmpty()) {
                                            Surface(
                                                color = Color(0xFFFFF4E5),
                                                contentColor = Color(0xFF92400E),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text(
                                                    "No line items were detected on this invoice.",
                                                    modifier = Modifier.padding(12.dp),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        } else {
                                            // Header row
                                            InvoiceTableHeader()

                                            // Rows
                                            rows.forEachIndexed { i, r ->
                                                val bg =
                                                    if (i % 2 == 0) Color(0xFFF9FBFF) else Color.White
                                                InvoiceTableRow(
                                                    description = r.description,
                                                    qty = r.qty,
                                                    batch = r.batch,
                                                    expiry = r.expiry,
                                                    background = bg
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(6.dp))
                                }
                            }
                        }

                        is ScanUiState.Idle -> {
                            // Exhaustive branch: gentle hint when nothing started yet
                            Surface(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        "Ready to Scan",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = Color(0xFF143A7B)
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "Open the scanner and capture your delivery slip. Once the OCR finishes, we’ll show the extracted invoice here.",
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom actions: Back + Create GRN
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
                        onClick = launchCreateGrn,
                        enabled = canCreateGrn,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                    ) { Text("Next", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }

            // Overlays
            SavedBanner(
                visible = showSavedBanner,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
            )

            // Centered loader overlay for upload/poll/saving
            val showLoader = saving || state is ScanUiState.Uploading
            if (showLoader) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Surface(tonalElevation = 6.dp, shape = RoundedCornerShape(16.dp)) {
                        Column(
                            Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
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

@Composable
private fun InvoiceMetaGrid(
    left: List<Pair<String, String>>,
    right: List<Pair<String, String>>
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            left.forEach { (k, v) -> MetaRow(k, v) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            right.forEach { (k, v) -> MetaRow(k, v) }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Column {
        Text(label, color = Color(0xFF6B7280), style = MaterialTheme.typography.labelMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF0F172A)
        )
    }
}

@Composable
private fun InvoiceTableHeader() {
    Surface(
        color = Color(0xFFEFF4FF),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderCell("Description", 0.46f)
            HeaderCell("Qty", 0.14f, TextAlign.Center)
            HeaderCell("Batch / Expiry", 0.40f) // merged column
        }
    }
}

@Composable
private fun RowScope.HeaderCell(
    text: String,
    cellWeight: Float,
    align: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        modifier = Modifier.weight(cellWeight),
        color = Color(0xFF1F2A44),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        textAlign = align,
        maxLines = 1
    )
}

@Composable
private fun RowScope.BodyCell(
    text: String,
    cellWeight: Float,
    align: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        modifier = Modifier.weight(cellWeight),
        color = Color(0xFF0F172A),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = align,
        maxLines = 2
    )
}

// dual-line value cell for the merged "Batch / Expiry" column
@Composable
private fun RowScope.DualLineCell(
    top: String,            // Batch number
    bottom: String,         // Expiry date
    cellWeight: Float,
    align: TextAlign = TextAlign.Start
) {
    Column(modifier = Modifier.weight(cellWeight)) {
        Text(
            text = top.ifBlank { "—" },
            color = Color(0xFF0F172A),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = align,
            maxLines = 1
        )
        Text(
            text = bottom.ifBlank { "—" },
            color = Color(0xFF475569),
            style = MaterialTheme.typography.labelSmall,
            textAlign = align,
            maxLines = 1
        )
    }
}

@Composable
private fun InvoiceTableRow(
    description: String,
    qty: String,
    batch: String,
    expiry: String,
    background: Color
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BodyCell(description, 0.46f)
        BodyCell(qty, 0.14f, TextAlign.Center)
        DualLineCell(batch, expiry, 0.40f) // Batch on top, Expiry below
    }
}
