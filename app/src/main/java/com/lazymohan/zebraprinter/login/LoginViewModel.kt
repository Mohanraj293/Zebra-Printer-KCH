package com.lazymohan.zebraprinter.login

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
            _loginEvents.emit(LoginEvent.StartOAuth)
        }
    }

    /** Called by LoginActivity after token exchange succeeds */
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
            appPref.saveTokens(
                accessToken = accessToken,
                refreshToken = refreshToken ?: "",
                expiresAtMillis = expiresAtMillis,
                tokenType = "Bearer",
                scope = null,
                idToken = null
            )
            appPref.isLoggedIn = true
            Log.d(TAG, "Tokens saved. Emitting Success and updating state.")
            _state.update { it.copy(loading = false, success = true) }
            _loginEvents.emit(LoginEvent.Success)
        }
    }

    /** Called by LoginActivity when OAuth flow fails */
    fun onOAuthFailed(message: String) {
        viewModelScope.launch {
            Log.e(TAG, "OAuth failed: $message")
            _state.update { it.copy(loading = false) }
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
                loading = false
            )
        }
    }
}

sealed class LoginEvent {
    object StartOAuth : LoginEvent()
    object Success : LoginEvent()
    data class Failure(val message: String?) : LoginEvent()
}
