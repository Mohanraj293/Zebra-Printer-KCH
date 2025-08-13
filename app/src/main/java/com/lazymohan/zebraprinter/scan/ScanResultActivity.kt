// app/src/main/java/com/lazymohan/zebraprinter/scan/ScanResultActivity.kt
package com.lazymohan.zebraprinter.scan

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tarkalabs.tarkaui.theme.TUITheme

class ScanResultActivity : ComponentActivity() {
    companion object { const val EXTRA_PAGES = "pages" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val uris: List<Uri> =
            intent.getStringArrayListExtra(EXTRA_PAGES)?.map(Uri::parse) ?: emptyList()

        setContent {
            TUITheme {
                ScanResultScreen(
                    pages = uris,
                    onBack = { finish() },
                    onRetake = { finish() } // back to scan
                )
            }
        }
    }
}
