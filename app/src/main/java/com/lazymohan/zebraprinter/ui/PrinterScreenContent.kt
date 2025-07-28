package com.lazymohan.zebraprinter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lazymohan.zebraprinter.effects.HandleErrorLaunchedEffect
import com.lazymohan.zebraprinter.effects.PermissionLaunchedEffect
import com.tarkalabs.tarkaui.components.TUIAppTopBar
import com.tarkalabs.tarkaui.components.TUIMobileButtonBlock
import com.tarkalabs.tarkaui.components.TUISnackBarHost
import com.tarkalabs.tarkaui.components.TUISnackBarState
import com.tarkalabs.tarkaui.components.TUISnackBarType
import com.tarkalabs.tarkaui.components.VerticalSpacer
import com.tarkalabs.tarkaui.components.base.TUIInputField
import com.tarkalabs.tarkaui.components.base.TUIInputFieldStatus
import com.tarkalabs.tarkaui.components.base.TUIInputFieldType.InputField
import com.tarkalabs.tarkaui.icons.Info24
import com.tarkalabs.tarkaui.icons.Print24
import com.tarkalabs.tarkaui.icons.TarkaIcons
import com.tarkalabs.tarkaui.icons.TarkaIcons.Filled
import com.tarkalabs.tarkaui.theme.TUITheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterScreenContent(
    modifier: Modifier = Modifier,
    uiState: PrinterUiState,
    handleEvents: (PrinterEvents) -> Unit
) {
    val snackState by remember {
        mutableStateOf(
            TUISnackBarState(
                SnackbarHostState(),
                TUISnackBarType.Error,
                Filled.Info24
            )
        )
    }

    PermissionLaunchedEffect(
        onAllPermissionGranted = {
            handleEvents(PrinterEvents.UpdatePrintButton(showPrintButton = true))
        },
        onSomePermissionDenied = {}
    )

    HandleErrorLaunchedEffect(
        snackBarMessage = uiState.snackBarMessage,
        snackBarType = uiState.snackBarType,
        snackState = snackState,
        removeErrorMessage = {
            handleEvents(PrinterEvents.RemoveError)
        }
    )


    val focusManager = LocalFocusManager.current
    Scaffold(
        topBar = {
            TUIAppTopBar(title = "ZPL Printer test")
        },
        snackbarHost = {
            TUISnackBarHost(
                modifier = modifier.defaultMinSize(minHeight = 160.dp),
                state = snackState
            )
        }
    ) { contentPadding ->
        Box(
            modifier = modifier
                .padding(contentPadding)
                .background(TUITheme.colors.surface)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                TUIInputField(
                    label = "Enter Item Number",
                    onValueChange = { itemNum ->
                        handleEvents(PrinterEvents.UpdateItemNum(itemNum = itemNum))
                    },
                    value = uiState.itemNum.orEmpty(),
                    status = TUIInputFieldStatus.Normal,
                    inputFieldTye = InputField,
                    keyboardOption = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardAction = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
                VerticalSpacer(space = 16)
                TUIInputField(
                    label = "Enter Description",
                    onValueChange = { description ->
                        handleEvents(PrinterEvents.UpdateDescription(description = description))
                    },
                    value = uiState.description.orEmpty(),
                    status = TUIInputFieldStatus.Normal,
                    inputFieldTye = InputField,
                    keyboardOption = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardAction = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
                VerticalSpacer(space = 16)
                TUIInputField(
                    label = "Enter No of Copies",
                    onValueChange = { noOfCopies ->
                        handleEvents(PrinterEvents.UpdateNoOfCopies(noOfCopies = noOfCopies))
                    },
                    value = uiState.noOfCopies.orEmpty(),
                    status = TUIInputFieldStatus.Normal,
                    inputFieldTye = InputField,
                    keyboardOption = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardAction = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
                VerticalSpacer(space = 16)
            }
            if (uiState.showPrintButton) {
                TUIMobileButtonBlock(
                    modifier = Modifier
                        .align(Alignment.BottomCenter),
                    primaryButtonLabel = "Print",
                    primaryButtonOnClick = {
                        handleEvents(PrinterEvents.Print)
                    },
                    primaryTrailingIcon = TarkaIcons.Regular.Print24,
                    outlineButtonLabel = null,
                    outlineButtonOnClick = {}
                )
            }
        }
    }
}