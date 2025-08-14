package com.lazymohan.zebraprinter.product

import com.lazymohan.zebraprinter.product.data.Item
import com.lazymohan.zebraprinter.product.data.Organisations

sealed class ProductsEvent {
    data object OnSearchClicked: ProductsEvent()
    data class OnSearchQueryChanged(val query: String): ProductsEvent()
    data object RemoveError : ProductsEvent()
    data class OnProductSelected(val product: Item) : ProductsEvent()
    data object OnBackPressed : ProductsEvent()
    data object Finish : ProductsEvent()
    data class OnOrgIdClicked(val show: Boolean) : ProductsEvent()
    data class SelectedOrgId(val orgId: Organisations) : ProductsEvent()
}