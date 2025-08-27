package com.lazymohan.zebraprinter.login

import com.lazymohan.zebraprinter.snacbarmessage.SnackBarMessage
import com.tarkalabs.tarkaui.components.TUISnackBarType

data class LoginUiState(
    val userName: String? = null,
    val password: String? = null,
    val loading: Boolean = false,
    val success: Boolean = false,
    val snackBarMessage: SnackBarMessage? = null,
    val snackBarType: TUISnackBarType = TUISnackBarType.Error,
    val user : UserAccount? = null
)