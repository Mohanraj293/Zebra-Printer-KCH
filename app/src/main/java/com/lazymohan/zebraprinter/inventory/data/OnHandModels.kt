package com.lazymohan.zebraprinter.inventory.data

data class OnHandRow(
    val transactionId: Long,
    val lotNumber: String,
    val expiryIso: String,
    val description: String? = null,
    val gtin: String? = null
)
