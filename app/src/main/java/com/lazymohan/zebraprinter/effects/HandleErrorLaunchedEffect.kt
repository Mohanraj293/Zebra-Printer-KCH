package com.lazymohan.zebraprinter.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.lazymohan.zebraprinter.R
import com.lazymohan.zebraprinter.snacbarmessage.SnackBarMessage
import com.tarkalabs.tarkaui.components.TUISnackBarState
import com.tarkalabs.tarkaui.components.TUISnackBarType
import com.tarkalabs.tarkaui.components.TUISnackBarType.Error
import com.tarkalabs.tarkaui.components.TUISnackBarType.Information
import com.tarkalabs.tarkaui.components.TUISnackBarType.Success
import com.tarkalabs.tarkaui.components.TUISnackBarType.Warning
import com.tarkalabs.tarkaui.icons.CheckmarkCircle24
import com.tarkalabs.tarkaui.icons.ErrorCircle24
import com.tarkalabs.tarkaui.icons.Info24
import com.tarkalabs.tarkaui.icons.TarkaIcon
import com.tarkalabs.tarkaui.icons.TarkaIcons.Filled
import com.tarkalabs.tarkaui.icons.Warning24

@Composable
fun HandleErrorLaunchedEffect(
    snackBarMessage: SnackBarMessage?,
    snackBarType: TUISnackBarType = Error,
    snackBarIcon: TarkaIcon? = null,
    snackState: TUISnackBarState,
    removeErrorMessage: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(snackBarMessage) {
        if (snackBarMessage != null) {
            val message = when (snackBarMessage) {
                is SnackBarMessage.StringMessage ->
                    snackBarMessage.message
                        ?: context.getString(R.string.common_error_message)

                is SnackBarMessage.StringResMessage -> context.getString(
                    snackBarMessage.messageRes,
                    *snackBarMessage.formatArgs
                )
            }
            snackState.type = snackBarType
            snackState.leadingIcon = snackBarIcon ?: when (snackBarType) {
                Success -> Filled.CheckmarkCircle24
                Information -> Filled.Info24
                Warning -> Filled.Warning24
                Error -> Filled.ErrorCircle24
            }
            snackState.showSnackBar(message)
            removeErrorMessage()
        }
    }
}