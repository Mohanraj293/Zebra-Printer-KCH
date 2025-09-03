package com.lazymohan.zebraprinter.product.lots.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lazymohan.zebraprinter.product.data.Item
import com.lazymohan.zebraprinter.product.data.Lots
import com.lazymohan.zebraprinter.product.data.ProductsRepo
import com.lazymohan.zebraprinter.product.lots.ui.LotsListUiState
import com.lazymohan.zebraprinter.snacbarmessage.SnackBarMessage
import com.lazymohan.zebraprinter.utils.launchWithHandler
import com.tarkalabs.tarkaui.components.TUISnackBarType
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LotsListViewModel @AssistedInject constructor(
    private val productsRepo: ProductsRepo,
    @Assisted("item") private val item: Item,
) : ViewModel() {

    @AssistedFactory
    interface LotsListViewModelFactory {
        fun create(
            @Assisted("item") item: Item
        ): LotsListViewModel
    }

    @Suppress("UNCHECKED_CAST")
    companion object {
        fun providesFactory(
            assistedFactory: LotsListViewModelFactory,
            item: Item
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>) = assistedFactory.create(
                item = item,
            ) as T
        }
    }

    init {
        getItemLots()
    }

    private val _uiState = MutableStateFlow(LotsListUiState())
    val uiState = _uiState.asStateFlow()

    private var currentOffset = 0
    private val limit = 25
    var hasMore = true
    var inventoryItemId: Long = 0L
    val itemProducts = mutableListOf<Lots>()

    fun getItemLots() {
        viewModelScope.launchWithHandler(Dispatchers.IO, ::handleErrors) {
            if (!hasMore) return@launchWithHandler
            showLoading()
            try {
                val lots = productsRepo.getLots(
                    itemNum = item.itemNumber.orEmpty(),
                    limit = limit,
                    offset = currentOffset
                )
                lots?.let {
                    hasMore = it.hasMore
                    currentOffset += limit
                    if (it.items.isEmpty()) {
                        updateSnackBarMessage(
                            message = SnackBarMessage.StringMessage("No lots found"),
                            type = TUISnackBarType.Error
                        )
                        hasMore = true
                        return@launchWithHandler
                    }
                    itemProducts.addAll(it.items)
                }
                val gtinForItem = productsRepo.getGTINForItem(itemProducts.first().inventoryItemId)
                _uiState.update { currentState ->
                    currentState.copy(
                        lotLists = currentState.lotLists + (lots?.items ?: emptyList()),
                        gtinForItem = gtinForItem?.items?.firstOrNull()?.gtin,
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