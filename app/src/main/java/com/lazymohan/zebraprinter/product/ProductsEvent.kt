package com.lazymohan.zebraprinter.product

sealed class ProductsEvent {
    data object OnSearchClicked: ProductsEvent()
    data class OnSearchQueryChanged(val query: String): ProductsEvent()
}