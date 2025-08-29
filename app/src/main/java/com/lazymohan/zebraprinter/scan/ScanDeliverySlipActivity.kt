package com.lazymohan.zebraprinter.scan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tarkalabs.tarkaui.theme.TUITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScanDeliverySlipActivity : ComponentActivity() {

    companion object {
        fun getCallingIntent(
            context: Context,
            isFromPickSlip: Boolean = false
        ): Intent {
            return Intent(context, ScanDeliverySlipActivity::class.java).apply {
                putExtra("isFromPickSlip", isFromPickSlip)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TUITheme {
                ScanDeliverySlipScreen(
                    isFromPickSlip = intent.getBooleanExtra("isFromPickSlip", false),
                    onBack = { finish() }
                )
            }
        }
    }
}
