package com.lazymohan.zebraprinter.scan

import com.google.gson.Gson
import com.lazymohan.zebraprinter.scan.data.OcrContentResponse

data class ScanExtractItem(
    val description: String,
    val qty: Double,
    val expiry: String,
    val batchNo: String
)

data class ScanExtractTransfer(
    val poNumber: String,
    val items: List<ScanExtractItem>
)

fun OcrContentResponse.toTransfer(): ScanExtractTransfer? {
    val xt = extractedText ?: return null
    val po = (xt.poNo ?: "").trim()
    val items = xt.items.mapNotNull { it ->
        val desc = (it.description ?: "").trim()
        if (desc.isEmpty()) return@mapNotNull null
        ScanExtractItem(
            description = desc,
            qty = (it.qtyDelivered ?: "0").replace(",", "").toDoubleOrNull() ?: 0.0,
            expiry = it.expiryDate ?: "",
            batchNo = it.batchNo ?: ""
        )
    }
    if (po.isEmpty() && items.isEmpty()) return null
    return ScanExtractTransfer(poNumber = po, items = items)
}

val scanGson: Gson by lazy { Gson() }
