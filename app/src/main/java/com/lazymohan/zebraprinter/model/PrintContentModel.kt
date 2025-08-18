package com.lazymohan.zebraprinter.model

data class PrintContentModel(
    val itemNum: String,
    val description: String,
    val gtinNum: String,
    val batchNo: String,
    val expiryDate: String
)