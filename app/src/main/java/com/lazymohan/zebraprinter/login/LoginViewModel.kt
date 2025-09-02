// app/src/main/java/com/lazymohan/zebraprinter/login/LoginViewModel.kt
package com.lazymohan.zebraprinter.login

import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazymohan.zebraprinter.app.AppPref
import com.lazymohan.zebraprinter.snacbarmessage.SnackBarMessage
import com.tarkalabs.tarkaui.components.TUISnackBarType
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val appPref: AppPref,
    private val authRepo: AuthRepo
) : ViewModel() {

    private val TAG = "OAuthVM"

    private val _state = MutableStateFlow(LoginUiState())
    val state = _state.asStateFlow()

    private val _loginEvents = MutableSharedFlow<LoginEvent>()
    val loginEvents = _loginEvents.asSharedFlow()

    /** Called by the Login button to start OAuth */
    fun startOAuth() {
        viewModelScope.launch {
            Log.d(TAG, "Emitting StartOAuth")
            _state.update { it.copy(loading = true, loadingMessage = "Redirecting…") }
            _loginEvents.emit(LoginEvent.StartOAuth)
        }
    }

    /**
     * Called by LoginActivity after token exchange succeeds.
     * - Saves tokens
     * - Extracts "sub" from access token and stores as username
     * - Calls Fusion userAccounts to get PersonId and saves it
     */
    fun onOAuthTokenReceived(
        accessToken: String,
        refreshToken: String?,
        expiresAtMillis: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(
                TAG,
                "Saving tokens… access=${accessToken.take(12)}… refresh=${refreshToken?.take(10)}… exp=$expiresAtMillis"
            )

            _state.update { it.copy(loading = true, loadingMessage = "Completing sign-in…") }

            // 1) Save tokens (no id_token)
            appPref.saveTokens(
                accessToken = accessToken,
                refreshToken = refreshToken ?: "",
                expiresAtMillis = expiresAtMillis,
                tokenType = "Bearer",
                scope = null,
                idToken = null
            )
            appPref.isLoggedIn = true
            Log.d(TAG, "Tokens saved.")

            // 2) Extract username from access token's "sub"
            val usernameFromToken = extractSubFromAccessToken(accessToken)
            if (usernameFromToken.isNullOrBlank()) {
                Log.w(TAG, "Could not extract 'sub' (username) from access token")
            } else {
                Log.d(TAG, "Extracted username from token sub=$usernameFromToken")

                appPref.username = usernameFromToken

                // 3) Hit Fusion API to resolve PersonId for this username
                _state.update { it.copy(loading = true, loadingMessage = "Fetching user profile…") }
                Log.d(TAG, "Calling AuthRepo.fetchUserByUsername($usernameFromToken)")
                val result = authRepo.fetchUserByUsername(usernameFromToken)
                result.onSuccess { resp ->
                    val match = resp.items.firstOrNull { it.Username.equals(usernameFromToken, ignoreCase = true) }
                        ?: resp.items.firstOrNull()

                    if (match != null) {
                        Log.d(TAG, "User lookup success. Username=${match.Username}, PersonId=${match.PersonId}")
                        appPref.saveUser(
                            personId = match.PersonId,
                            username = usernameFromToken
                        )
                    } else {
                        Log.w(TAG, "User lookup returned 0 items for $usernameFromToken")
                    }
                }.onFailure { e ->
                    Log.e(TAG, "User lookup failed for $usernameFromToken: ${e.message}", e)
                }
            }

            Log.d(TAG, "Emitting Success and updating state.")
            _state.update { it.copy(loading = false, loadingMessage = null, success = true) }
            _loginEvents.emit(LoginEvent.Success)
        }
    }

    /** Called by LoginActivity when OAuth flow fails */
    fun onOAuthFailed(message: String) {
        viewModelScope.launch {
            Log.e(TAG, "OAuth failed: $message")
            _state.update { it.copy(loading = false, loadingMessage = null) }
            _loginEvents.emit(LoginEvent.Failure(message))
        }
    }

    fun updateSnackBarMessage(
        message: SnackBarMessage?,
        type: TUISnackBarType = TUISnackBarType.Error
    ) {
        Log.d(TAG, "updateSnackBarMessage: $message type=$type")
        _state.update {
            it.copy(
                snackBarMessage = message,
                snackBarType = type,
                loading = it.loading
            )
        }
    }

    /**
     * Extracts the "sub" claim from a JWT access token (without verification).
     * Works if the access token is a JWT. If it's opaque, returns null.
     */
    private fun extractSubFromAccessToken(jwt: String): String? {
        return try {
            val parts = jwt.split('.')
            if (parts.size < 2) return null
            val payloadB64 = parts[1]
            val payloadJson = String(
                Base64.decode(
                    payloadB64,
                    Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
                )
            )
            val obj = JSONObject(payloadJson)
            obj.optString("sub").takeIf { it.isNotBlank() }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to parse 'sub' from access token: ${t.message}")
            null
        }
    }
}

sealed class LoginEvent {
    object StartOAuth : LoginEvent()
    object Success : LoginEvent()
    data class Failure(val message: String?) : LoginEvent()
}
