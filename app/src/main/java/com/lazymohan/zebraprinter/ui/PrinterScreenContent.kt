package com.lazymohan.zebraprinter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dokar.sheets.rememberBottomSheetState
import com.lazymohan.zebraprinter.effects.HandleErrorLaunchedEffect
import com.lazymohan.zebraprinter.product.data.Item
import com.lazymohan.zebraprinter.utils.EAMLoader
import com.lazymohan.zebraprinter.utils.PrinterSelectionCard
import com.tarkalabs.tarkaui.components.TUIAppTopBar
import com.tarkalabs.tarkaui.components.TUIDivider
import com.tarkalabs.tarkaui.components.TUIMobileButtonBlock
import com.tarkalabs.tarkaui.components.TUISnackBarHost
import com.tarkalabs.tarkaui.components.TUISnackBarState
import com.tarkalabs.tarkaui.components.TUISnackBarType
import com.tarkalabs.tarkaui.components.TUITextRow
import com.tarkalabs.tarkaui.components.VerticalSpacer
import com.tarkalabs.tarkaui.components.base.TUIInputField
import com.tarkalabs.tarkaui.components.base.TUIInputFieldStatus
import com.tarkalabs.tarkaui.components.base.TUIInputFieldType.InputField
import com.tarkalabs.tarkaui.icons.ChevronLeft24
import com.tarkalabs.tarkaui.icons.Info24
import com.tarkalabs.tarkaui.icons.Print24
import com.tarkalabs.tarkaui.icons.TarkaIcons
import com.tarkalabs.tarkaui.icons.TarkaIcons.Filled
import com.tarkalabs.tarkaui.theme.TUITheme
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterScreenContent(
    modifier: Modifier = Modifier,
    uiState: PrinterUiState,
    item: Item?,
    handleEvents: (PrinterEvents) -> Unit,
    onBackPressed: () -> Unit
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

    val bottomSheetState = rememberBottomSheetState()
    val scrollableState = rememberScrollState()
    val scope = rememberCoroutineScope()

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
            TUIAppTopBar(
                title = "Inventory Item ${item?.itemNumber}",
                navigationIcon = TarkaIcons.Regular.ChevronLeft24.copy(
                    contentDescription = "Back"
                ),
                onNavigationIconClick = {
                    onBackPressed()
                }
            )
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
            Column {
                VerticalSpacer(space = 16)
                PrinterSelectionCard(
                    selectedPrinter = uiState.selectedPrinter?.model,
                    onPrinterSelectionClick = {
                        handleEvents(PrinterEvents.UpdateBottomSheet(true))
                        scope.launch {
                            bottomSheetState.expand()
                        }
                    }
                )
                VerticalSpacer(space = 16)
                TUIDivider()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollableState)
                        .padding(16.dp)
                ) {
                    TUITextRow(title = "Item Number: ${item?.itemNumber ?: "N/A"}")
                    VerticalSpacer(space = 16)
                    TUITextRow(title = "Description: ${item?.itemDescription ?: "N/A"}")
                    VerticalSpacer(space = 16)
                    TUITextRow(title = "Lot Number: ${item?.lotNumber ?: "N/A"}")
                    VerticalSpacer(space = 16)
                    TUITextRow(title = "Organization: ${item?.organizationName ?: "N/A"}")
                    VerticalSpacer(space = 16)
                    TUITextRow(title = "Status: ${item?.statusCode ?: "N/A"}")
                    VerticalSpacer(space = 16)
                    TUITextRow(title = "Origination Date: ${item?.originationDate ?: "N/A"}")
                    VerticalSpacer(space = 16)
                    TUITextRow(title = "Expiration Date: ${item?.expirationDate ?: "N/A"}")
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
                            onNext = { focusManager.moveFocus(FocusDirection.Enter) }
                        )
                    )
                    VerticalSpacer(space = 90)
                }
            }
            TUIMobileButtonBlock(
                modifier = Modifier
                    .align(Alignment.BottomCenter),
                primaryButtonLabel = "Print",
                primaryButtonOnClick = {
                    handleEvents(PrinterEvents.Print(true))
                },
                primaryTrailingIcon = TarkaIcons.Regular.Print24,
                outlineButtonLabel = null,
                outlineButtonOnClick = {}
            )
        }
        if (uiState.showDialog) {
            AvailablePrintersBottomSheet(
                bottomSheetState = bottomSheetState,
                snackBarState = snackState,
                items = uiState.discoveredPrinters.toImmutableList(),
                onSelectItem = {
                    handleEvents(PrinterEvents.UpdateSelectedPrinter(it))
                    scope.launch {
                        bottomSheetState.collapse()
                    }
                },
                onDiscardClicked = {
                    scope.launch {
                        bottomSheetState.collapse()
                        handleEvents(PrinterEvents.UpdateBottomSheet(false))
                    }
                }
            )
        }
        if (uiState.isLoading) {
            EAMLoader()
        }
    }
}