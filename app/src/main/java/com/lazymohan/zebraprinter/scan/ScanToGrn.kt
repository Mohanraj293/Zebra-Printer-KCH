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

private fun parseQty(src: String?): Double {
    if (src.isNullOrBlank()) return 0.0
    val cleaned = src.replace(",", "").trim()
    val match = Regex("""[-+]?\d*\.?\d+""").find(cleaned) ?: return 0.0
    return match.value.toDoubleOrNull() ?: 0.0
}

fun OcrContentResponse.toTransfer(isFromPickSlip: Boolean): ScanExtractTransfer? {
    val xt = extractedText ?: return null

    val po = if (isFromPickSlip)
        listOf(xt.orderNumber)
            .firstOrNull { !it.isNullOrBlank() }?.trim().orEmpty()
    else
        listOf(xt.poNo)
            .firstOrNull { !it.isNullOrBlank() }?.trim().orEmpty()

    val items = buildList {
        xt.items.forEach { item ->
            val desc = item.description?.trim().orEmpty()
            if (desc.isBlank()) return@forEach

            // New schema path: details[]
            if (!item.details.isNullOrEmpty()) {
                item.details.forEach { d ->
                    val qty = parseQty(d.qtyDelivered)
                    val expiry = d.expiryDate?.trim().orEmpty()
                    val batch = d.batchNo?.trim().orEmpty()
                    if (qty > 0.0 || expiry.isNotBlank() || batch.isNotBlank()) {
                        add(ScanExtractItem(desc, qty, expiry, batch))
                    }
                }
            }
        }
    }

    if (po.isBlank() && items.isEmpty()) return null
    return ScanExtractTransfer(poNumber = po, items = items)
}

val scanGson: Gson by lazy { Gson() }
