// app/src/main/java/com/lazymohan/zebraprinter/login/LoginActivity.kt
package com.lazymohan.zebraprinter.login

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.lazymohan.zebraprinter.BuildConfig
import com.lazymohan.zebraprinter.landing.LandingActivity
import com.lazymohan.zebraprinter.snacbarmessage.SnackBarMessage
import com.tarkalabs.tarkaui.components.TUISnackBarType
import com.tarkalabs.tarkaui.theme.TUITheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ClientAuthentication
import net.openid.appauth.ClientSecretBasic
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest

@AndroidEntryPoint
class LoginActivity : ComponentActivity() {

    companion object {
        private const val TAG = "OAuth"
        private const val TAG_VM = "OAuthVM"
    }

    private val viewModel: LoginViewModel by viewModels()
    private lateinit var authService: AuthorizationService

    private val AUTHORIZATION_ENDPOINT =
        BuildConfig.IDCS_BASE_URL.trimEnd('/') + BuildConfig.AUTH_ENDPOINT
    private val TOKEN_ENDPOINT =
        BuildConfig.IDCS_BASE_URL.trimEnd('/') + BuildConfig.TOKEN_ENDPOINT

    private val CLIENT_ID = BuildConfig.OAUTH_CLIENT_ID
    private val CLIENT_SECRET: String? =
        BuildConfig.OAUTH_CLIENT_SECRET.takeIf { it.isNotBlank() }

    private val REDIRECT_URI = BuildConfig.REDIRECT_URI

    private val SCOPE = BuildConfig.OAUTH_SCOPE
    private val ACTION_AUTH_RESPONSE = "com.lazymohan.zebraprinter.login.AUTH_RESPONSE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate action=${intent?.action}")
        Log.d(TAG, "AUTHORIZATION_ENDPOINT=$AUTHORIZATION_ENDPOINT")
        Log.d(TAG, "TOKEN_ENDPOINT=$TOKEN_ENDPOINT")
        Log.d(TAG, "REDIRECT_URI=$REDIRECT_URI")
        Log.d(TAG, "CLIENT_ID=$CLIENT_ID")
        Log.d(TAG, "SCOPE=$SCOPE")

        authService = AuthorizationService(this)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginEvents.collect { event ->
                    when (event) {
                        is LoginEvent.StartOAuth -> {
                            Log.d(TAG, "LoginEvent.StartOAuth received → starting browser flow")
                            startOAuthFlow()
                        }
                        else -> Unit
                    }
                }
            }
        }

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

        if (intent.action == ACTION_AUTH_RESPONSE) {
            Log.d(TAG, "Handling AUTH_RESPONSE in onCreate")
            handleAuthResponse(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == ACTION_AUTH_RESPONSE) {
            Log.d(TAG, "Handling AUTH_RESPONSE in onNewIntent")
            handleAuthResponse(intent)
        }
    }

    private fun startOAuthFlow() {
        val serviceConfig = AuthorizationServiceConfiguration(
            Uri.parse(AUTHORIZATION_ENDPOINT),
            Uri.parse(TOKEN_ENDPOINT)
        )

        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            CLIENT_ID,
            ResponseTypeValues.CODE,
            Uri.parse(REDIRECT_URI)
        )
            .setScope(SCOPE)
            .build()

        Log.d(TAG, "AuthRequest built: ${authRequest.toUri()}")

        // ***** IMPORTANT *****
        // AppAuth needs a MUTABLE PendingIntent on API 31+ so it can attach the response/exception extras.
        val flags = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            }
            else -> PendingIntent.FLAG_UPDATE_CURRENT
        }

        val completeIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, LoginActivity::class.java).setAction(ACTION_AUTH_RESPONSE),
            flags
        )

        val cancelIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, LoginActivity::class.java).setAction(ACTION_AUTH_RESPONSE),
            flags
        )

        Log.d(TAG, "Launching browser for authorization…")
        authService.performAuthorizationRequest(authRequest, completeIntent, cancelIntent)
    }

    private fun handleAuthResponse(intent: Intent) {
        Log.d(TAG, "handleAuthResponse() called with intent=$intent")
        val extras = intent.extras
        Log.d(TAG, "extras keys=${extras?.keySet()} data=${intent.data}")

        val hasRespExtra = intent.hasExtra(AuthorizationResponse.EXTRA_RESPONSE)
        val hasExExtra = intent.hasExtra(AuthorizationException.EXTRA_EXCEPTION)
        Log.d(TAG, "hasRespExtra=$hasRespExtra hasExExtra=$hasExExtra")

        if (!hasRespExtra && !hasExExtra) {
            Log.w(TAG, "AUTH_RESPONSE received without AppAuth payload (likely test or stale). Ignoring.")
            return
        }

        val authResponse = AuthorizationResponse.fromIntent(intent)
        val authException = AuthorizationException.fromIntent(intent)

        if (authException != null) {
            Log.e(TAG, "Authorization failed: ${authException.errorDescription}")
            viewModel.onOAuthFailed(authException.errorDescription ?: "Authorization failed.")
            return
        }
        if (authResponse == null) {
            Log.e(TAG, "AuthorizationResponse is null")
            viewModel.onOAuthFailed("Empty authorization response.")
            return
        }

        Log.d(TAG, "Authorization code received, exchanging for tokens…")
        val tokenReq: TokenRequest = authResponse.createTokenExchangeRequest()
        val clientAuth: ClientAuthentication? = CLIENT_SECRET?.let { ClientSecretBasic(it) }

        if (clientAuth != null) {
            Log.d(TAG, "Using confidential client for token exchange")
            authService.performTokenRequest(tokenReq, clientAuth) { tokenResponse, ex ->
                handleTokenResponse(tokenResponse, ex)
            }
        } else {
            Log.d(TAG, "Using public client (PKCE) for token exchange")
            authService.performTokenRequest(tokenReq) { tokenResponse, ex ->
                handleTokenResponse(tokenResponse, ex)
            }
        }
    }

    private fun handleTokenResponse(
        tokenResponse: net.openid.appauth.TokenResponse?,
        ex: AuthorizationException?
    ) {
        if (ex != null) {
            Log.e(TAG, "Token exchange failed: ${ex.errorDescription}")
            viewModel.onOAuthFailed(ex.errorDescription ?: "Token exchange failed.")
            return
        }
        if (tokenResponse == null) {
            Log.e(TAG, "Token response was null")
            viewModel.onOAuthFailed("Empty token response.")
            return
        }

        val accessToken = tokenResponse.accessToken.orEmpty()
        val refreshToken = tokenResponse.refreshToken
        val expiresAt = tokenResponse.accessTokenExpirationTime ?: 0L

        Log.d(TAG, "Tokens received: access=${accessToken.take(6)}… refresh=${if (refreshToken != null) "yes" else "no"} exp=$expiresAt")
        Log.d(TAG_VM, "Saving tokens…")

        viewModel.onOAuthTokenReceived(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtMillis = expiresAt
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        authService.dispose()
    }
}
