package com.lazymohan.zebraprinter.landing

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import com.lazymohan.zebraprinter.grn.ui.GrnActivity
import com.lazymohan.zebraprinter.product.ProductsActivity
import com.lazymohan.zebraprinter.scan.ScanDeliverySlipActivity
import com.tarkalabs.tarkaui.theme.TUITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LandingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TUITheme {
                val snack = remember { SnackbarHostState() }

                LandingScreenContent(
                    snackbarHostState = snack,
                    onScanDelivery = {
                        startActivity(
                            ScanDeliverySlipActivity.getCallingIntent(context = this)
                        )
                    },
                    onPrintQr = {
                        startActivity(Intent(this, ProductsActivity::class.java))
                    },
                    onManualGrn = {
                        startActivity(Intent(this, GrnActivity::class.java))
                    },
                    onPickSlipClicked = {
                        startActivity(
                            ScanDeliverySlipActivity.getCallingIntent(
                                context = this,
                                isFromPickSlip = true
                            )
                        )
                    }
                )
            }
        }
    }
}
