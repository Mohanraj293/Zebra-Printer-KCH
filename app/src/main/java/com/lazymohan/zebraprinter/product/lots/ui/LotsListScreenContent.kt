package com.lazymohan.zebraprinter.product.lots.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lazymohan.zebraprinter.effects.HandleErrorLaunchedEffect
import com.lazymohan.zebraprinter.product.lots.data.LotsListViewModel
import com.lazymohan.zebraprinter.utils.DateTimeConverter
import com.lazymohan.zebraprinter.utils.EAMLoader
import com.lazymohan.zebraprinter.utils.EAMLoaderStyle
import com.tarkalabs.tarkaui.components.TUIAppTopBar
import com.tarkalabs.tarkaui.components.TUISnackBarHost
import com.tarkalabs.tarkaui.components.TUISnackBarState
import com.tarkalabs.tarkaui.components.TUISnackBarType
import com.tarkalabs.tarkaui.components.VerticalSpacer
import com.tarkalabs.tarkaui.icons.ChevronLeft24
import com.tarkalabs.tarkaui.icons.Info24
import com.tarkalabs.tarkaui.icons.TarkaIcons.Filled
import com.tarkalabs.tarkaui.theme.TUITheme

@Composable
fun LotsListScreenContent(
    modifier: Modifier = Modifier,
    viewModel: LotsListViewModel,
    dateTimeConverter: DateTimeConverter,
    handleEvent: (LotsListsEvent) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackState by remember {
        mutableStateOf(
            TUISnackBarState(
                SnackbarHostState(),
                TUISnackBarType.Error,
                Filled.Info24
            )
        )
    }

    HandleErrorLaunchedEffect(
        snackBarMessage = uiState.snackBarMessage,
        snackBarType = uiState.snackBarType,
        snackState = snackState,
        removeErrorMessage = {
            handleEvent(LotsListsEvent.RemoveError)
        }
    )
    Scaffold(
        topBar = {
            LotsListTopBar(
                onNavigationIconClick = { handleEvent(LotsListsEvent.OnBackPressed) }
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
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = modifier
                    .padding(8.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LotsListMainContent(
                    listState = listState,
                    handleEvent = handleEvent,
                    uiState = uiState,
                    dateTimeConverter = dateTimeConverter
                )
            }

            if (uiState.isLoading) {
                EAMLoader(loaderStyle = EAMLoaderStyle.S)
            }
        }
    }
}

@Composable
fun LotsListMainContent(
    listState: LazyListState,
    uiState: LotsListUiState,
    dateTimeConverter: DateTimeConverter,
    handleEvent: (LotsListsEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        items(uiState.lotLists.size) { index ->
            val lot = uiState.lotLists[index]
            LotCard(
                lotNumber = lot.lotNumber.orEmpty(),
                description = lot.itemDescription.orEmpty(),
                statusCode = lot.statusCode.orEmpty(),
                organisationDate = dateTimeConverter.getDisplayDate(lot.originationDate),
                expirationDate = dateTimeConverter.getDisplayDate(lot.expirationDate),
                onClick = {
                    handleEvent(
                        LotsListsEvent.OnLotSelected(
                            lot = lot,
                            gtinNumber = uiState.gtinForItem ?: "N/A"
                        )
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotsListTopBar(
    onNavigationIconClick: () -> Unit
) {
    TUIAppTopBar(
        title = "Lots Lists",
        navigationIcon = Filled.ChevronLeft24,
        onNavigationIconClick = onNavigationIconClick
    )
}

@Composable
fun LotCard(
    lotNumber: String,
    description: String,
    statusCode: String,
    organisationDate: String,
    expirationDate: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .background(TUITheme.colors.surface)
                .padding(16.dp)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Lot Number : $lotNumber",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TUITheme.colors.onSurface
            )
            VerticalSpacer(8)
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Description: $description",
                style = MaterialTheme.typography.bodyMedium,
                color = TUITheme.colors.onSurface
            )
            VerticalSpacer(8)
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Status Code: $statusCode",
                style = MaterialTheme.typography.bodyMedium,
                color = TUITheme.colors.onSurface
            )
            VerticalSpacer(8)
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Organisation Date: $organisationDate",
                style = MaterialTheme.typography.bodyMedium,
                color = TUITheme.colors.onSurface
            )
            VerticalSpacer(8)
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Expiration Date: $expirationDate",
                style = MaterialTheme.typography.bodyMedium,
                color = TUITheme.colors.onSurface
            )
        }
    }
}