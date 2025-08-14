package com.lazymohan.zebraprinter.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dokar.sheets.BottomSheetState
import com.lazymohan.zebraprinter.product.data.Organisations
import com.lazymohan.zebraprinter.utils.EAMBottomSheet
import com.tarkalabs.tarkaui.components.TUIDivider
import com.tarkalabs.tarkaui.components.TUIMobileOverlayFooter
import com.tarkalabs.tarkaui.components.TUIMobileOverlayHeaderStyle.HeaderWithTitle
import com.tarkalabs.tarkaui.components.TUISnackBarState
import com.tarkalabs.tarkaui.components.TUITextRow
import kotlinx.collections.immutable.ImmutableList

@Composable
fun OrgIdBottomSheet(
    bottomSheetState: BottomSheetState,
    snackBarState: TUISnackBarState,
    items: ImmutableList<Organisations>,
    onSelectItem: (selectedItem: Organisations) -> Unit,
    onDiscardClicked: () -> Unit
) {
    LaunchedEffect(bottomSheetState.visible) {
        if (!bottomSheetState.visible) {
            onDiscardClicked()
        }
    }

    EAMBottomSheet(
        modifier = Modifier.wrapContentHeight(),
        bottomSheetState = bottomSheetState,
        style = HeaderWithTitle("Select OrgId"),
        bottomSheetSnackBarState = snackBarState
    ) {
        LazyColumn(modifier = Modifier.padding(vertical = 16.dp)) {
            if (items.isNotEmpty()) {
                items(items) { item ->
                    TUITextRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 26.dp),
                        title = item.organizationCode,
                        onTextRowClick = {
                            onSelectItem(item)
                        }
                    )
                }
            } else {
                item {
                    TUITextRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 26.dp),
                        title = "No OrgId Found",
                        onTextRowClick = {}
                    )
                }
            }
        }
        TUIDivider()
        TUIMobileOverlayFooter(
            showMiddleDismiss = true,
            onMiddleDismissClick = onDiscardClicked,
            modifier = Modifier.fillMaxWidth()
        )
    }
}