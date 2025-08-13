// app/src/main/java/com/lazymohan/zebraprinter/scan/ScanReviewScreen.kt
@file:OptIn(ExperimentalMaterial3Api::class)

package com.lazymohan.zebraprinter.scan

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.*

@Composable
fun ScanReviewScreen(
    initialPages: List<Uri>,
    onBack: () -> Unit,
    onDone: (List<Uri>) -> Unit
) {
    val ctx = LocalContext.current
    var pages by remember { mutableStateOf(initialPages.toMutableList()) }
    var editingIndex by remember { mutableIntStateOf(-1) }

    val cropLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK && res.data != null) {
            val resultUri = UCrop.getOutput(res.data!!)
            if (resultUri != null && editingIndex >= 0) pages[editingIndex] = resultUri
        }
        editingIndex = -1
    }

    fun launchCrop(index: Int) {
        val src = pages[index]
        val dstFile = File(ctx.cacheDir, "crop_${UUID.randomUUID()}.jpg")
        val dst = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", dstFile)
        val options = UCrop.Options().apply {
            setFreeStyleCropEnabled(true)
            setHideBottomControls(false)
        }
        val intent = UCrop.of(src, dst).withOptions(options).getIntent(ctx)
        editingIndex = index
        cropLauncher.launch(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review & Edit") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { pages.clear() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) { Text("Clear") }

                    Button(
                        onClick = { onDone(pages) },
                        enabled = pages.isNotEmpty(),
                        modifier = Modifier
                            .weight(2f)
                            .height(48.dp)
                    ) { Text("Continue") }
                }
            }
        }
    ) { inner ->
        if (pages.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentAlignment = Alignment.Center
            ) { Text("No pages. Go back and rescan.") }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(pages) { index, uri ->
                    Card {
                        Column {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp),
                                contentScale = ContentScale.Crop
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { launchCrop(index) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Crop / Rotate")
                                }
                                OutlinedButton(
                                    onClick = { pages.removeAt(index) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Remove")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
