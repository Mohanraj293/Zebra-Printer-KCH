package com.lazymohan.zebraprinter.product

import com.lazymohan.zebraprinter.product.data.Item
import com.lazymohan.zebraprinter.product.data.Organisations
import com.lazymohan.zebraprinter.snacbarmessage.SnackBarMessage
import com.tarkalabs.tarkaui.components.TUISnackBarType

data class ProductUiState(
    val isLoading: Boolean = false,
    val snackBarMessage: SnackBarMessage? = null,
    val snackBarType: TUISnackBarType = TUISnackBarType.Error,
    val searchQuery: String = "",
    val products: List<Item> = emptyList(),
    val showProductScreen: Boolean = false,
    val selectedOrgId: Organisations? = null,
    val orgIdList: List<Organisations> = emptyList(),
    val showOrgIdBottomSheet: Boolean = false
)