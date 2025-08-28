package com.lazymohan.zebraprinter.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.lazymohan.zebraprinter.landing.LandingActivity
import com.lazymohan.zebraprinter.snacbarmessage.SnackBarMessage
import com.tarkalabs.tarkaui.components.TUISnackBarType
import com.tarkalabs.tarkaui.theme.TUITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : ComponentActivity() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TUITheme {
                LoginScreenContent(
                    viewModel = viewModel,
                    moveToLandingPage = {
                        startActivity(Intent(this, LandingActivity::class.java))
                        finish()
                    },
                    updateError = {
                        viewModel.updateSnackBarMessage(
                            message = SnackBarMessage.StringMessage(it),
                            type = TUISnackBarType.Error
                        )
                    }
                )
            }
        }
    }
}