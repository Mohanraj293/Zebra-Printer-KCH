package com.lazymohan.zebraprinter.product

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dokar.sheets.rememberBottomSheetState
import com.lazymohan.zebraprinter.effects.HandleErrorLaunchedEffect
import com.lazymohan.zebraprinter.grn.ui.Header
import com.lazymohan.zebraprinter.ui.OrgIdBottomSheet
import com.lazymohan.zebraprinter.utils.EAMLoader
import com.lazymohan.zebraprinter.utils.EAMLoaderStyle
import com.tarkalabs.tarkaui.components.TUISnackBarHost
import com.tarkalabs.tarkaui.components.TUISnackBarState
import com.tarkalabs.tarkaui.components.TUISnackBarType
import com.tarkalabs.tarkaui.components.VerticalSpacer
import com.tarkalabs.tarkaui.components.base.TUIInputField
import com.tarkalabs.tarkaui.components.base.TUIInputFieldStatus
import com.tarkalabs.tarkaui.components.base.TUIInputFieldType
import com.tarkalabs.tarkaui.components.base.TUIInputFieldType.InputField
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

    val gradient = Brush.verticalGradient(listOf(Color(0xFF0E63FF), Color(0xFF5AA7FF)))

    Scaffold(
        containerColor = Color(0xFFF6F8FF),
        snackbarHost = {
            TUISnackBarHost(
                modifier = modifier.defaultMinSize(minHeight = 160.dp),
                state = snackState
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .padding(paddingValues)
                .background(TUITheme.colors.surface)
                .fillMaxSize()
        ) {
            Header(
                gradient = gradient,
                title = "Products",
                onLogoClick = {
                    handleEvent(ProductsEvent.Finish)
                },
                subtitle = "Search for products",
            )
            VerticalSpacer(20)
            Card(
                modifier = Modifier
                    .padding(20.dp)
                    .offset(y = (-18).dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    TUIInputField(
                        label = "Select Organization",
                        onValueChange = { itemNum ->
                            handleEvent(ProductsEvent.OnSearchQueryChanged(itemNum))
                        },
                        value = uiState.selectedOrgId?.organizationCode.orEmpty(),
                        status = TUIInputFieldStatus.Normal,
                        inputFieldTye = TUIInputFieldType.LookupInputField,
                        keyboardOption = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardAction = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = modifier.clickable {
                            handleEvent(ProductsEvent.OnOrgIdClicked(true))
                            coroutineScope.launch {
                                bottomSheetState.expand()
                            }
                        }
                    )
                    VerticalSpacer(10)
                    TUIInputField(
                        label = "Enter Item description to search",
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
                    Spacer(Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                handleEvent(ProductsEvent.OnSearchClicked)
                            },
                            enabled = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6BFF))
                        ) {
                            Text("Search", color = Color.White)
                        }
                    }
                }
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