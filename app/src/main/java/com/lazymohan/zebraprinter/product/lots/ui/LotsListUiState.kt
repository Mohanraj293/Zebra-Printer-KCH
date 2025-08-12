package com.lazymohan.zebraprinter.product.lots.ui

import com.lazymohan.zebraprinter.product.data.Lots
import com.lazymohan.zebraprinter.snacbarmessage.SnackBarMessage
import com.tarkalabs.tarkaui.components.TUISnackBarType

data class LotsListUiState(
    val isLoading: Boolean = false,
    val snackBarMessage: SnackBarMessage? = null,
    val snackBarType: TUISnackBarType = TUISnackBarType.Error,
    val searchQuery: String = "",
    val lotLists : List<Lots> = emptyList(),
    val gtinForItem: String? = null,
)