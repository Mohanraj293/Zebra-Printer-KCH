package com.lazymohan.zebraprinter.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dokar.sheets.BottomSheetState
import com.dokar.sheets.m3.BottomSheet
import com.tarkalabs.tarkaui.components.TUIMobileOverlayHeader
import com.tarkalabs.tarkaui.components.TUIMobileOverlayHeaderStyle
import com.tarkalabs.tarkaui.components.TUISnackBarHost
import com.tarkalabs.tarkaui.components.TUISnackBarState
import com.tarkalabs.tarkaui.theme.TUITheme

@Composable
fun EAMBottomSheet(
    modifier: Modifier = Modifier,
    bottomSheetState: BottomSheetState,
    style: TUIMobileOverlayHeaderStyle,
    skipPeeked: Boolean = true,
    bottomSheetSnackBarState: TUISnackBarState? = null,
    onDismiss: (() -> Unit)? = null,
    isFromAudioBottomSheet: Boolean = false,
    content: @Composable () -> Unit
) {
    LaunchedEffect(bottomSheetState.visible) {
        if (!bottomSheetState.visible && onDismiss != null) onDismiss()
    }
    BottomSheet(
        modifier = Modifier
            .wrapContentHeight()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        state = bottomSheetState,
        skipPeeked = skipPeeked,
        showAboveKeyboard = true,
        dragHandle = {}
    ) {
        Box(modifier = Modifier) {
            Column(modifier = modifier.background(TUITheme.colors.surface)) {
                TUIMobileOverlayHeader(style = style)
                content()
            }
            if (bottomSheetSnackBarState != null) {
                TUISnackBarHost(
                    modifier = Modifier
                        .padding(bottom = if (isFromAudioBottomSheet) 100.dp else 16.dp)
                        .align(Alignment.BottomCenter),
                    state = bottomSheetSnackBarState
                )
            }
        }
    }
}