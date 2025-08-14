package com.lazymohan.zebraprinter.product

import android.annotation.SuppressLint
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dokar.sheets.rememberBottomSheetState
import com.lazymohan.zebraprinter.effects.HandleErrorLaunchedEffect
import com.lazymohan.zebraprinter.ui.OrgIdBottomSheet
import com.lazymohan.zebraprinter.utils.EAMLoader
import com.lazymohan.zebraprinter.utils.EAMLoaderStyle
import com.tarkalabs.tarkaui.components.TUIAppTopBar
import com.tarkalabs.tarkaui.components.TUISnackBarHost
import com.tarkalabs.tarkaui.components.TUISnackBarState
import com.tarkalabs.tarkaui.components.TUISnackBarType
import com.tarkalabs.tarkaui.components.VerticalSpacer
import com.tarkalabs.tarkaui.components.base.TUIButton
import com.tarkalabs.tarkaui.components.base.TUIInputField
import com.tarkalabs.tarkaui.components.base.TUIInputFieldStatus
import com.tarkalabs.tarkaui.components.base.TUIInputFieldType.InputField
import com.tarkalabs.tarkaui.icons.ChevronLeft24
import com.tarkalabs.tarkaui.icons.Info24
import com.tarkalabs.tarkaui.icons.TarkaIcons.Filled
import com.tarkalabs.tarkaui.theme.TUITheme
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun ProductScreenContent(
    modifier: Modifier = Modifier,
    viewModel: ProductsViewModel,
    handleEvent: (ProductsEvent) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val snackState = remember {
        TUISnackBarState(
            SnackbarHostState(),
            TUISnackBarType.Error,
            Filled.Info24
        )
    }

    val bottomSheetState = rememberBottomSheetState()

    HandleErrorLaunchedEffect(
        snackBarMessage = uiState.snackBarMessage,
        snackBarType = uiState.snackBarType,
        snackState = snackState,
        removeErrorMessage = {
            handleEvent(ProductsEvent.RemoveError)
        }
    )

    Scaffold(
        topBar = {
            ProductTopBar(
                onNavigationIconClick = { handleEvent(ProductsEvent.Finish) }
            )
        },
        snackbarHost = {
            TUISnackBarHost(
                modifier = modifier.defaultMinSize(minHeight = 160.dp),
                state = snackState
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .padding(paddingValues)
                .background(TUITheme.colors.surface)
                .fillMaxSize()
        ) {
            OrgIdSelectionCard(
                selectedOrgId = uiState.selectedOrgId?.organizationCode
            ) {
                handleEvent(ProductsEvent.OnOrgIdClicked(true))
                coroutineScope.launch {
                    bottomSheetState.expand()
                }
            }


            Column(
                modifier = modifier
                    .padding(8.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProductMainContent(
                    focusManager = focusManager,
                    handleEvent = handleEvent,
                    uiState = uiState
                )
                VerticalSpacer(10)
                TUIButton("Search") {
                    handleEvent(ProductsEvent.OnSearchClicked)
                }
            }

            if (uiState.isLoading) {
                EAMLoader(loaderStyle = EAMLoaderStyle.S)
            }

            if (uiState.showProductScreen) {
                ProductListScreen(
                    viewModel = viewModel,
                    onBackPressed = {
                        handleEvent(ProductsEvent.OnBackPressed)
                    },
                    onClick = {
                        handleEvent(ProductsEvent.OnProductSelected(it))
                    }
                )
            }
        }
    }

    OrgIdBottomSheet(
        snackBarState = snackState,
        items = uiState.orgIdList.toImmutableList(),
        onSelectItem = {
            handleEvent(ProductsEvent.SelectedOrgId(it))
            coroutineScope.launch { bottomSheetState.collapse() }
        },
        bottomSheetState = bottomSheetState,
        onDiscardClicked = {
            coroutineScope.launch { bottomSheetState.collapse() }
        }
    )
}

@Composable
fun ProductMainContent(
    focusManager: FocusManager,
    uiState: ProductUiState,
    handleEvent: (ProductsEvent) -> Unit
) {
    TUIInputField(
        label = "Enter product name",
        onValueChange = { itemNum ->
            handleEvent(ProductsEvent.OnSearchQueryChanged(itemNum))
        },
        value = uiState.searchQuery,
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductTopBar(
    onNavigationIconClick: () -> Unit
) {
    TUIAppTopBar(
        title = "Products",
        navigationIcon = Filled.ChevronLeft24,
        onNavigationIconClick = onNavigationIconClick,
    )
}