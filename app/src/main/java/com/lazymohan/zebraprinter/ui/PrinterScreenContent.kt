package com.lazymohan.zebraprinter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dokar.sheets.rememberBottomSheetState
import com.lazymohan.zebraprinter.effects.HandleErrorLaunchedEffect
import com.lazymohan.zebraprinter.grn.ui.Header
import com.lazymohan.zebraprinter.model.DiscoveredPrinterInfo
import com.lazymohan.zebraprinter.product.data.Lots
import com.lazymohan.zebraprinter.utils.DateTimeConverter
import com.lazymohan.zebraprinter.utils.EAMLoader
import com.lazymohan.zebraprinter.utils.EAMLoaderStyle
import com.lazymohan.zebraprinter.utils.PrinterSelectionCard
import com.tarkalabs.tarkaui.components.TUIAppTopBar
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
import java.sql.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterScreenContent(
    modifier: Modifier = Modifier,
    uiState: PrinterUiState,
    lots: Lots?,
    gtinNumber: String?,
    dateTimeConverter: DateTimeConverter,
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

    val gradient = Brush.verticalGradient(listOf(Color(0xFF0E63FF), Color(0xFF5AA7FF)))
    val bottomSheetState = rememberBottomSheetState()
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
                title = "Inventory Item ${lots?.itemNumber}",
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
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Header(
                    gradient = gradient,
                    title = "Printer",
                    onLogoClick = {

                    },
                    subtitle = "ZPL Printer",
                )
                VerticalSpacer(20)
                PrinterSelectionCard(
                    selectedPrinter = uiState.selectedPrinter?.model,
                    onPrinterSelectionClick = {
                        handleEvents(
                            if (uiState.canDiscoverPrinter) {
                                PrinterEvents.UpdateBottomSheet(show = true)
                            } else {
                                PrinterEvents.CanDiscover(
                                    canDiscoverPrinter = true
                                )
                            }
                        )
                        scope.launch {
                            bottomSheetState.expand()
                        }
                    }
                )
                Card(
                    modifier = Modifier.padding(20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        TUITextRow(title = "Item Number: ${lots?.itemNumber ?: "N/A"}")
                        VerticalSpacer(space = 4)
                        TUITextRow(title = "Description: ${lots?.itemDescription ?: "N/A"}")
                        VerticalSpacer(space = 4)
                        TUITextRow(title = "Lot Number: ${lots?.lotNumber ?: "N/A"}")
                        VerticalSpacer(space = 4)
                        TUITextRow(title = "Status: ${lots?.statusCode ?: "N/A"}")
                        VerticalSpacer(space = 4)
                        TUITextRow(title = "Expiration Date: ${dateTimeConverter.getDisplayDate(lots?.expirationDate)}")
                        VerticalSpacer(space = 4)
                        TUITextRow(title = "GTIN Number: ${gtinNumber ?: "N/A"}")
                        VerticalSpacer(space = 4)
                        TUIInputField(
                            label = "Enter no of Labels",
                            onValueChange = { noOfCopies ->
                                handleEvents(PrinterEvents.UpdateNoOfCopies(noOfCopies = noOfCopies))
                            },
                            value = uiState.noOfCopies,
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
                    }
                }
                TUIMobileButtonBlock(
                    primaryButtonLabel = "Print",
                    primaryButtonOnClick = {
                        if (uiState.selectedPrinter != null)
                            handleEvents(PrinterEvents.Print)
                    },
                    primaryTrailingIcon = TarkaIcons.Regular.Print24,
                    outlineButtonLabel = null,
                    outlineButtonOnClick = {}
                )
            }
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
            EAMLoader(loaderStyle = EAMLoaderStyle.S)
        }
    }
}

@Preview
@Composable
private fun PrevPrint() {
    PrinterScreenContent(
        modifier = Modifier,
        uiState = PrinterUiState(
            isLoading = false,
            showDialog = false,
            canDiscoverPrinter = false,
            discoveredPrinters = listOf(
                DiscoveredPrinterInfo(
                    macAddress = "00:11:22:33:44:55",
                    model = "Zebra ZD620"
                )
            ),
            selectedPrinter = DiscoveredPrinterInfo(
                macAddress = "00:11:22:33:44:55",
                model = "Zebra ZD620"
            ),
            noOfCopies = "1",
            snackBarMessage = null,
            snackBarType = TUISnackBarType.Error
        ),
        lots = Lots(
            lotNumber = "123",
            itemDescription = "Test Desc",
            itemNumber = "Test Item",
            statusCode = "Available",
            originationDate = Date(1234567890),
            expirationDate = Date(1234567890),
            inventoryItemId = 12344L
        ),
        gtinNumber = "TODO()",
        dateTimeConverter = DateTimeConverter(),
        handleEvents = { },
        onBackPressed = { }
    )
}