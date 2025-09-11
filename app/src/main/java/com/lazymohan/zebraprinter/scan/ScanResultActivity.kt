package com.lazymohan.zebraprinter.scan

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import com.lazymohan.zebraprinter.scan.ui.ScanResultViewModel
import com.tarkalabs.tarkaui.theme.TUITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScanResultActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PAGES = "pages"
        private const val TAG = "ScanResultActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.d(TAG, "onCreate()")

        // Defensive: re-grant read permission for all Uris passed via ClipData
        intent?.clipData?.let { cd ->
            for (i in 0 until cd.itemCount) {
                cd.getItemAt(i).uri?.let { u ->
                    try {
                        grantUriPermission(
                            packageName,
                            u,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        Log.d(TAG, "Granted read permission for $u")
                    } catch (se: SecurityException) {
                        Log.e(TAG, "grantUriPermission failed for $u: ${se.message}")
                    }
                }
            }
        }

        val uris: List<Uri> =
            intent.getStringArrayListExtra(EXTRA_PAGES)?.map(Uri::parse) ?: emptyList()
        Log.d(TAG, "Received pages count = ${uris.size}")

        setContent {
            TUITheme {
                val vm: ScanResultViewModel = hiltViewModel()
                LaunchedEffect(uris) {
                    Log.d(TAG, "Starting upload/poll with ${uris.size} pages")
                    vm.start(uris)
                }
                ScanResultScreen(
                    pages = uris,
                    onBack = {
                        finish()
                    },
                    onRetake = {
                        finish()
                    },
                    isFromPickSlip = intent.getBooleanExtra("isFromPickSlip", false),
                )
            }
        }
    }
}
