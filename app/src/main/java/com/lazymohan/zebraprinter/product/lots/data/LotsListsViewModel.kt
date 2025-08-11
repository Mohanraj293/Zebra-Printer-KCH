package com.lazymohan.zebraprinter.product.lots.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lazymohan.zebraprinter.product.ProductUiState
import com.lazymohan.zebraprinter.product.data.Lots
import com.lazymohan.zebraprinter.product.data.ProductsRepo
import com.lazymohan.zebraprinter.product.lots.ui.LotsListUiState
import com.lazymohan.zebraprinter.snacbarmessage.SnackBarMessage
import com.lazymohan.zebraprinter.utils.launchWithHandler
import com.tarkalabs.tarkaui.components.TUISnackBarType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LotsListViewModel(
    private val productsRepo: ProductsRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(LotsListUiState())
    val uiState = _uiState.asStateFlow()

    private var currentOffset = 0
    private val limit = 25
    var hasMore = true
    val itemProducts = mutableListOf<Lots>()

    fun updateSearchQuery(query: String) {
        _uiState.update { currentState ->
            currentState.copy(searchQuery = query)
        }
    }

    fun getItemLots() {
        viewModelScope.launchWithHandler(Dispatchers.IO, ::handleErrors) {
            if (!hasMore) return@launchWithHandler
            if (_uiState.value.searchQuery.isEmpty()) {
                updateSnackBarMessage(
                    message = SnackBarMessage.StringMessage("Search query cannot be empty"),
                    type = TUISnackBarType.Error
                )
                return@launchWithHandler
            }
            showLoading()
            try {
                val products = productsRepo.getLots(
                    _uiState.value.searchQuery,
                    limit = limit,
                    offset = currentOffset
                )
                products?.let {
                    hasMore = it.hasMore
                    currentOffset += limit
                    if (it.items.isEmpty()) {
                        updateSnackBarMessage(
                            message = SnackBarMessage.StringMessage("No products found"),
                            type = TUISnackBarType.Error
                        )
                        hasMore = true
                        return@launchWithHandler
                    }
                    itemProducts.addAll(it.items)
                }
                _uiState.update { currentState ->
                    currentState.copy(
                        products = currentState.products + (products?.items ?: emptyList()),
                        showProductScreen = true
                    )
                }
                hideLoading()
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        snackBarMessage = SnackBarMessage.StringMessage(
                            message = e.message ?: "An error occurred"
                        )
                    )
                }
                hideLoading()
            }
        }
    }

    fun updateShowProductScreen(show: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(showProductScreen = show, products = emptyList())
        }
        hasMore = true
    }

    fun handleErrors(throwable: Throwable) {
        updateSnackBarMessage(message = SnackBarMessage.StringMessage(throwable.message))
    }

    fun removeError() {
        _uiState.update { it.copy(snackBarMessage = null, snackBarType = TUISnackBarType.Error) }
    }

    fun updateSnackBarMessage(
        message: SnackBarMessage?,
        type: TUISnackBarType = TUISnackBarType.Error
    ) {
        _uiState.update {
            it.copy(
                snackBarMessage = message,
                snackBarType = type,
                isLoading = false
            )
        }
    }

    fun showLoading() {
        _uiState.update { it.copy(isLoading = true) }
    }

    fun hideLoading() {
        _uiState.update { it.copy(isLoading = false) }
    }
}

class ProductsViewModelFactory(
    private val repository: ProductsRepo
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductsViewModel::class.java)) {
            return ProductsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}