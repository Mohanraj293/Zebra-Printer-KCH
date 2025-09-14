package com.lazymohan.zebraprinter.grn.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageFormat
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.YuvImage
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@OptIn(ExperimentalGetImage::class)
@Composable
fun QrScannerScreen(
    onQrScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // 🔎 Support all barcode formats
    val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_ALL_FORMATS
        )
        .build()
    val scanner = remember { BarcodeScanning.getClient(options) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    // Preview use case
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    var scanning = true

                    // Analyzer use case
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { useCase ->
                            useCase.setAnalyzer(
                                ContextCompat.getMainExecutor(ctx)
                            ) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val rotation = imageProxy.imageInfo.rotationDegrees
                                    val input = InputImage.fromMediaImage(mediaImage, rotation)
                                    scanner.process(input)
                                        .addOnSuccessListener { barcodes ->
                                            if (barcodes.isNotEmpty()) {
                                                val bitmap = imageProxyToBitmap(imageProxy)
                                                scanning = false // ✅ stop further processing
                                                onQrScanned(barcodes.first().rawValue ?: "")
                                            } else {
                                                // 🔄 only try inverted if normal failed
                                                val bitmap = imageProxyToBitmap(imageProxy)
                                                val inverted = invertBitmap(bitmap)
                                                val invertedInput = InputImage.fromBitmap(inverted, rotation)
                                                scanner.process(invertedInput)
                                                    .addOnSuccessListener { inv ->
                                                        if (inv.isNotEmpty()) {
                                                            scanning = false
                                                            onQrScanned(inv.first().rawValue ?: "")
                                                        }
                                                    }
                                                    .addOnCompleteListener { imageProxy.close() }
                                            }
                                        }
                                        .addOnFailureListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis
                        )
                    } catch (exc: Exception) {
                        exc.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        // White square overlay
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(240.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = Color.White,
                    size = size,
                    style = Stroke(width = 4.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                )
            }
        }
    }
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap?, fileName: String) {
    val fos: OutputStream?
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/ScannedQRCodes"
            )
        }
        val imageUri: Uri? =
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        fos = imageUri?.let { resolver.openOutputStream(it) }
    } else {
        val imagesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
        val image = File(imagesDir, "$fileName.jpeg")
        fos = FileOutputStream(image)
    }
    fos?.use {
        bitmap?.compress(
            /* format = */Bitmap.CompressFormat.JPEG,
            /* quality = */100,
            /* stream = */it
        )
    }
}

@OptIn(ExperimentalGetImage::class)
fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
    val mediaImage = imageProxy.image ?: throw IllegalArgumentException("No image in ImageProxy")
    val yBuffer = mediaImage.planes[0].buffer
    val uBuffer = mediaImage.planes[1].buffer
    val vBuffer = mediaImage.planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, mediaImage.width, mediaImage.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, mediaImage.width, mediaImage.height), 100, out)
    val imageBytes = out.toByteArray()
    val decoded = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    return decoded.copy(Bitmap.Config.ARGB_8888, true)
}


fun invertBitmap(source: Bitmap): Bitmap {
    val inverted = createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(inverted)
    val matrix = ColorMatrix()
    val paint = Paint()
    matrix.set(
        floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f,-1f, 0f, 0f, 255f,
            0f, 0f,-1f, 0f, 255f,
            0f, 0f, 0f, 1f,   0f
        )
    )
    paint.colorFilter = ColorMatrixColorFilter(matrix)
    canvas.drawBitmap(source, 0f, 0f, paint)
    return inverted
}