package com.lazymohan.zebraprinter.landing

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import com.lazymohan.zebraprinter.app.AppPref
import com.lazymohan.zebraprinter.grn.ui.GrnActivity
import com.lazymohan.zebraprinter.login.LoginActivity
import com.lazymohan.zebraprinter.product.ProductsActivity
import com.lazymohan.zebraprinter.scan.ScanDeliverySlipActivity
import com.tarkalabs.tarkaui.theme.TUITheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LandingActivity : ComponentActivity() {

    @Inject lateinit var appPref: AppPref

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
                    onInProgress = { label ->
                        scope.launch { snack.showSnackbar("$label is in progress") }
                    },
                    userName = appPref.username.orEmpty(),
                    logoutHandler = {
                        appPref.clearUser()
                        scope.launch {
                            startActivity(Intent(this@LandingActivity, LoginActivity::class.java))
                            finish()
                        }
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
