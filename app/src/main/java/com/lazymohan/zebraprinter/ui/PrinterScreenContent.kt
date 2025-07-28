package com.lazymohan.zebraprinter.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tarkalabs.tarkaui.components.VerticalSpacer
import com.tarkalabs.tarkaui.components.base.TUIButton
import com.tarkalabs.tarkaui.components.base.TUIInputField
import com.tarkalabs.tarkaui.components.base.TUIInputFieldStatus
import com.tarkalabs.tarkaui.components.base.TUIInputFieldType.InputField
import com.tarkalabs.tarkaui.theme.TUITheme

private var allPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN
    )
} else {
    arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
}

@Composable
fun PrinterScreenContent(
    modifier: Modifier = Modifier,
    uiState: PrinterUiState,
    handleEvents: (PrinterEvents) -> Unit
) {
    PermissionLaunchedEffect(
        onAllPermissionGranted = {

        },
        onSomePermissionDenied = {

        }
    )

    val focusManager = LocalFocusManager.current
    Scaffold { contentPadding ->
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
            TUIButton(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomEnd),
                label = "Print",
                onClick = {
                    handleEvents(PrinterEvents.Print)
                }
            )
        }
    }
}

@Composable
private fun PermissionLaunchedEffect(
    onAllPermissionGranted: () -> Unit,
    onSomePermissionDenied: () -> Unit
) {
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = RequestMultiplePermissions()
        ) { map ->
            val isAllPermissionsGranted = map.values.all { it }
            if (isAllPermissionsGranted) {
                onAllPermissionGranted()
            } else {
                onSomePermissionDenied()
            }
        }

    LaunchedEffect(true) {
        permissionLauncher.launch(allPermissions)
    }
}