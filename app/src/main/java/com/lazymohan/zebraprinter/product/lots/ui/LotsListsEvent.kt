package com.lazymohan.zebraprinter.product.lots.ui

import com.lazymohan.zebraprinter.product.data.Lots

sealed class LotsListsEvent {
    data object RemoveError : LotsListsEvent()
    data class OnLotSelected(val lot: Lots, val gtinNumber: String) : LotsListsEvent()
    data object OnBackPressed : LotsListsEvent()
}