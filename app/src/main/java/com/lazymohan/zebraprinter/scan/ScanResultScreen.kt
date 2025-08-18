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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.lazymohan.zebraprinter.scan.model.ExtractedHeader
import com.lazymohan.zebraprinter.scan.model.ExtractedItem as GrnItem
import com.lazymohan.zebraprinter.scan.data.ExtractedItem as ParsedItem
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

/* Map your parsed item -> GRN flow's Parcelable item */
private fun ParsedItem.toGrnItem(): GrnItem = GrnItem(
    description    = this.description ?: "",
    qtyDelivered   = this.qtyDelivered,
    batchNo        = this.batchNo,
    expiryDate     = this.expiryDate
)

/* --- Small labeled fields for the dummy editor ---
 * Put the function-type param LAST so trailing-lambda calls work.
 */
@Composable
private fun LabeledTextField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun LabeledNumberField(
    label: String,
    value: String,
    errorText: String? = null,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        isError = errorText != null,
        supportingText = { if (errorText != null) Text(errorText, color = Color(0xFFB00020)) },
        modifier = modifier.fillMaxWidth()
    )
}

// ---------- UI ----------
@Composable
fun ScanResultScreen(
    pages: List<Uri>,
    onBack: () -> Unit,
    onRetake: () -> Unit,
    onProceed: (ExtractedHeader, List<GrnItem>) -> Unit = { _, _ -> },
    viewModel: ScanResultViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val state by viewModel.state.collectAsState()

    var selected by remember { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    var showSavedBanner by remember { mutableStateOf(false) }

    // Fallback flag + editable dummy data
    var useDummy by remember { mutableStateOf(false) }
    var dummyInvoiceNo by remember { mutableStateOf("INV-DUMMY-001") }
    var dummyInvoiceDate by remember { mutableStateOf("2025-08-01") }
    var dummyPoNo by remember { mutableStateOf("KHQ/PO/99387") }
    val dummyItems = remember {
        mutableStateListOf(
            GrnItem(
                description = "TACHYBEN (URAPIDIL) 50MG/10ML AMP 5'S",
                uom = "BOX",
                itemNumber = "URP-50AMP",
                orderedQtyHint = 10.0,
                qtyDelivered = 5.0,
                batchNo = "B123",
                expiryDate = "2026-12-31"
            ),
            GrnItem(
                description = "NUTRYELT 10ML AMP (TRACE EL.) ADULT-10'S",
                uom = "BOX",
                itemNumber = "NUT-TRACE10",
                orderedQtyHint = 20.0,
                qtyDelivered = 10.0,
                batchNo = "N777",
                expiryDate = "2026-10-30"
            )
        )
    }

    val gradient = Brush.verticalGradient(listOf(Color(0xFF0E63FF), Color(0xFF5AA7FF)))

    Scaffold(
        containerColor = Color(0xFFF6F8FF),
        snackbarHost = { SnackbarHost(hostState = snack) }
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            Column(Modifier.fillMaxSize()) {

                // Header (non-scrollable)
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

                // SCROLLABLE CONTENT — unified LazyColumn
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Preview card
                    item {
                        Surface(
                            modifier = Modifier.offset(y = (-6).dp),
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
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                    if (pages.size > 1) {
                                        Spacer(Modifier.height(12.dp))
                                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    }

                    when (val s = state) {
                        is ScanUiState.Uploading -> {
                            item {
                                Column {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    Spacer(Modifier.height(8.dp))
                                    Text("Please wait… this may take a few seconds.", color = Color(0xFF5C6370))
                                }
                            }
                        }

                        is ScanUiState.Error -> {
                            // Error card
                            item {
                                Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFFFF1F0)) {
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
                            // Fallback button to use dummy data
                            item {
                                OutlinedButton(
                                    onClick = { useDummy = true },
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                ) { Text("Use Dummy Data Instead") }
                            }

                            // Editable Dummy Data UI
                            if (useDummy) {
                                // Dummy header editor
                                item {
                                    Surface(shape = RoundedCornerShape(20.dp), color = Color.White, tonalElevation = 1.dp) {
                                        Column(Modifier.padding(16.dp)) {
                                            Text("Dummy Delivery Slip (Editable)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                            Spacer(Modifier.height(10.dp))
                                            LabeledTextField("Invoice No", dummyInvoiceNo) { dummyInvoiceNo = it }
                                            Spacer(Modifier.height(8.dp))
                                            LabeledTextField("Invoice Date (YYYY-MM-DD)", dummyInvoiceDate) { dummyInvoiceDate = it }
                                            Spacer(Modifier.height(8.dp))
                                            LabeledTextField("PO No (Prefill for next step)", dummyPoNo) { dummyPoNo = it }
                                        }
                                    }
                                }

                                // Dummy items editor list
                                itemsIndexed(dummyItems) { idx, item ->
                                    Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp, color = Color.White) {
                                        Column(Modifier.padding(12.dp)) {
                                            Text(item.description, fontWeight = FontWeight.SemiBold)
                                            item.uom?.let { Text("UOM: $it") }
                                            item.orderedQtyHint?.let { Text("Ordered Qty (hint): $it") }
                                            Spacer(Modifier.height(8.dp))

                                            var qtyText by remember(idx) { mutableStateOf(item.qtyDelivered?.toString() ?: "") }
                                            var lotText by remember(idx) { mutableStateOf(item.batchNo.orEmpty()) }
                                            var expText by remember(idx) { mutableStateOf(item.expiryDate.orEmpty()) }

                                            val qtyErr = qtyText.toDoubleOrNull()?.let { q ->
                                                item.orderedQtyHint?.let { if (q > it) "Cannot exceed ordered qty." else null }
                                            }

                                            LabeledNumberField("Quantity", qtyText, errorText = qtyErr) {
                                                qtyText = it
                                                val q = it.toDoubleOrNull()
                                                dummyItems[idx] = dummyItems[idx].copy(qtyDelivered = q)
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            LabeledTextField("Lot Number", lotText) {
                                                lotText = it
                                                dummyItems[idx] = dummyItems[idx].copy(batchNo = it)
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            LabeledTextField("Expiry (YYYY-MM-DD)", expText) {
                                                expText = it
                                                dummyItems[idx] = dummyItems[idx].copy(expiryDate = it)
                                            }
                                        }
                                    }
                                }

                                // Proceed with dummy
                                item {
                                    val hasValid = dummyItems.any { (it.qtyDelivered ?: 0.0) > 0.0 && !it.batchNo.isNullOrBlank() && !it.expiryDate.isNullOrBlank() }
                                    Button(
                                        onClick = {
                                            val header = ExtractedHeader(
                                                supplierName = null,
                                                invoiceNo = dummyInvoiceNo,
                                                invoiceDate = dummyInvoiceDate,
                                                poGuess = dummyPoNo
                                            )
                                            onProceed(header, dummyItems.toList())
                                        },
                                        enabled = hasValid,
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                                    ) { Text("Proceed with Dummy Data", color = Color.White, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }

                        is ScanUiState.Completed -> {
                            val xt = s.data.extractedText
                            val parsed: List<ParsedItem> = xt?.items ?: emptyList()

                            // Summary card
                            item {
                                Surface(shape = RoundedCornerShape(20.dp), color = Color.White, tonalElevation = 1.dp) {
                                    Column(Modifier.padding(16.dp)) {
                                        Text("Invoice Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.height(6.dp))
                                        Text("Invoice No: ${xt?.invoiceNo ?: "-"}")
                                        Text("Invoice Date: ${xt?.invoiceDate ?: "-"}")
                                        Text("PO No: ${xt?.poNo ?: "-"}")
                                    }
                                }
                            }

                            // Items header
                            item {
                                Surface(shape = RoundedCornerShape(20.dp), color = Color.White, tonalElevation = 1.dp) {
                                    Column(Modifier.padding(16.dp)) {
                                        Text("Extracted Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        if (parsed.isEmpty()) {
                                            Spacer(Modifier.height(8.dp))
                                            Text("No items detected.")
                                        }
                                    }
                                }
                            }

                            // Items list (parsed)
                            items(parsed) { item: ParsedItem ->
                                Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp, color = Color.White) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(item.description ?: "-", fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.height(4.dp))
                                        Text("Qty: ${item.qtyDelivered ?: "-"}")
                                        Text("Batch: ${item.batchNo ?: "-"}")
                                        Text("Expiry: ${item.expiryDate ?: "-"}")
                                    }
                                }
                            }

                            // Proceed with parsed data
                            item {
                                val canProceed = parsed.isNotEmpty()
                                Button(
                                    onClick = {
                                        val header = ExtractedHeader(
                                            supplierName = null,
                                            invoiceNo    = xt?.invoiceNo,
                                            invoiceDate  = xt?.invoiceDate,
                                            poGuess      = xt?.poNo
                                        )
                                        val mapped: List<GrnItem> = parsed.map { it.toGrnItem() }
                                        onProceed(header, mapped)
                                    },
                                    enabled = canProceed,
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                                ) { Text("Proceed to GRN", color = Color.White, fontWeight = FontWeight.Bold) }
                            }

                            // Or use dummy data anyway
                            item {
                                OutlinedButton(
                                    onClick = { useDummy = true },
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                ) { Text("Use Dummy Data Instead") }
                            }

                            // If user toggled dummy after completion, show the same dummy editor
                            if (useDummy) {
                                item {
                                    Surface(shape = RoundedCornerShape(20.dp), color = Color.White, tonalElevation = 1.dp) {
                                        Column(Modifier.padding(16.dp)) {
                                            Text("Dummy Delivery Slip (Editable)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                            Spacer(Modifier.height(10.dp))
                                            LabeledTextField("Invoice No", dummyInvoiceNo) { dummyInvoiceNo = it }
                                            Spacer(Modifier.height(8.dp))
                                            LabeledTextField("Invoice Date (YYYY-MM-DD)", dummyInvoiceDate) { dummyInvoiceDate = it }
                                            Spacer(Modifier.height(8.dp))
                                            LabeledTextField("PO No (Prefill for next step)", dummyPoNo) { dummyPoNo = it }
                                        }
                                    }
                                }
                                itemsIndexed(dummyItems) { idx, item ->
                                    Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp, color = Color.White) {
                                        Column(Modifier.padding(12.dp)) {
                                            Text(item.description, fontWeight = FontWeight.SemiBold)
                                            item.uom?.let { Text("UOM: $it") }
                                            item.orderedQtyHint?.let { Text("Ordered Qty (hint): $it") }
                                            Spacer(Modifier.height(8.dp))

                                            var qtyText by remember(idx) { mutableStateOf(item.qtyDelivered?.toString() ?: "") }
                                            var lotText by remember(idx) { mutableStateOf(item.batchNo.orEmpty()) }
                                            var expText by remember(idx) { mutableStateOf(item.expiryDate.orEmpty()) }

                                            val qtyErr = qtyText.toDoubleOrNull()?.let { q ->
                                                item.orderedQtyHint?.let { if (q > it) "Cannot exceed ordered qty." else null }
                                            }

                                            LabeledNumberField("Quantity", qtyText, errorText = qtyErr) {
                                                qtyText = it
                                                val q = it.toDoubleOrNull()
                                                dummyItems[idx] = dummyItems[idx].copy(qtyDelivered = q)
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            LabeledTextField("Lot Number", lotText) {
                                                lotText = it
                                                dummyItems[idx] = dummyItems[idx].copy(batchNo = it)
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            LabeledTextField("Expiry (YYYY-MM-DD)", expText) {
                                                expText = it
                                                dummyItems[idx] = dummyItems[idx].copy(expiryDate = it)
                                            }
                                        }
                                    }
                                }
                                item {
                                    val hasValid = dummyItems.any { (it.qtyDelivered ?: 0.0) > 0.0 && !it.batchNo.isNullOrBlank() && !it.expiryDate.isNullOrBlank() }
                                    Button(
                                        onClick = {
                                            val header = ExtractedHeader(
                                                supplierName = null,
                                                invoiceNo = dummyInvoiceNo,
                                                invoiceDate = dummyInvoiceDate,
                                                poGuess = dummyPoNo
                                            )
                                            onProceed(header, dummyItems.toList())
                                        },
                                        enabled = hasValid,
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                                    ) { Text("Proceed with Dummy Data", color = Color.White, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }

                        else -> { /* Ready/Idle – no extra content */ }
                    }
                }

                // Bottom actions (non-scrollable)
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
