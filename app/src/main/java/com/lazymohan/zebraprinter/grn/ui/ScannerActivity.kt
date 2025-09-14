package com.lazymohan.zebraprinter.grn.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tarkalabs.tarkaui.theme.TUITheme

class ScannerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TUITheme {
                QrScannerScreen(
                    onQrScanned = { scannedValue ->
                        // ✅ When a QR is scanned, return it to the caller
                        val resultIntent = Intent().apply {
                            putExtra("QR_RESULT", scannedValue)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                )
            }
        }
    }
}