package com.lazymohan.zebraprinter.product

import com.lazymohan.zebraprinter.product.data.Item

sealed class ProductsEvent {
    data object OnSearchClicked: ProductsEvent()
    data class OnSearchQueryChanged(val query: String): ProductsEvent()
    data object RemoveError : ProductsEvent()
    data class OnProductSelected(val product: Item) : ProductsEvent()
    data object OnBackPressed : ProductsEvent()
}