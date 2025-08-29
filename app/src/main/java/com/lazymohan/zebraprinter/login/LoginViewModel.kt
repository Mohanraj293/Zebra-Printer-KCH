package com.lazymohan.zebraprinter.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazymohan.zebraprinter.app.AppPref
import com.lazymohan.zebraprinter.snacbarmessage.SnackBarMessage
import com.lazymohan.zebraprinter.utils.launchWithHandler
import com.tarkalabs.tarkaui.components.TUISnackBarType
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val appPref: AppPref
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state = _state.asStateFlow()

    private val _loginEvents = MutableSharedFlow<LoginEvent>()
    val loginEvents = _loginEvents.asSharedFlow()

    fun login(username: String, password: String) {
        viewModelScope.launchWithHandler(Dispatchers.IO, ::handleError) {
            updateLoader(true)
            val result = AuthRepo.login(username, password)
            result.fold(
                onSuccess = {
                    val user = it.items.first()
                    updateLoginStatus(isLoggedIn = true, user = user)
                    _loginEvents.emit(LoginEvent.Success)
                    appPref.saveUser(
                        personId = user.personId,
                        username = user.username.orEmpty()
                    )
                    updateLoader(false)
                },
                onFailure = {
                    updateLoginStatus(isLoggedIn = false, error = it.message)
                    _loginEvents.emit(LoginEvent.Failure(it.message))
                    updateLoader(false)
                }
            )
        }
    }

    private fun updateLoader(loading: Boolean) {
        _state.update { it.copy(loading = loading) }
    }

    private fun updateLoginStatus(
        isLoggedIn: Boolean,
        error: String? = null,
        user: UserAccount? = null
    ): LoginUiState {
        return _state.value.copy(
            success = isLoggedIn,
            snackBarMessage = SnackBarMessage.StringMessage(error),
            user = user
        )
    }


    private fun handleError(throwable: Throwable) {
        _state.update { it.copy(snackBarMessage = SnackBarMessage.StringMessage(throwable.message)) }
    }

    fun updateSnackBarMessage(
        message: SnackBarMessage?,
        type: TUISnackBarType = TUISnackBarType.Error
    ) {
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
    object Success : LoginEvent()
    data class Failure(val message: String?) : LoginEvent()
}