package com.lazymohan.zebraprinter.login

import com.lazymohan.zebraprinter.snacbarmessage.SnackBarMessage
import com.tarkalabs.tarkaui.components.TUISnackBarType

data class LoginUiState(
    val loading: Boolean = false,
    val loadingMessage: String? = null,
    val success: Boolean = false,
    val snackBarMessage: SnackBarMessage? = null,
    val snackBarType: TUISnackBarType = TUISnackBarType.Error
)
