// app/src/main/java/com/lazymohan/zebraprinter/scan/ScanDeliverySlipActivity.kt
package com.lazymohan.zebraprinter.scan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.tarkalabs.tarkaui.theme.TUITheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ScanDeliverySlipActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TUITheme {
                val snack = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                ScanDeliverySlipScreen(
                    snackbarHostState = snack,
                    onBack = { finish() },
                    onCapture = {
                        // TODO: integrate real camera flow here
                        scope.launch { snack.showSnackbar("Capturing & processing…") }
                    }
                )
            }
        }
    }
}
