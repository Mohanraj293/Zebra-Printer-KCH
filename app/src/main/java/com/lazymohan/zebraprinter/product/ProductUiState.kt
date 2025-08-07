package com.lazymohan.zebraprinter.product

import com.lazymohan.zebraprinter.product.data.Item

data class ProductUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val products: List<Item> = emptyList(),
)