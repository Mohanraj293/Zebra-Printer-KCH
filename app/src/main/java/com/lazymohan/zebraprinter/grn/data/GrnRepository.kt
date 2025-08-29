package com.lazymohan.zebraprinter.grn.data

import android.util.Log

class GrnRepository(private val api: FusionApi) {

    suspend fun fetchPo(orderNumber: String): Result<PoItem> = runCatching {
        val resp = api.getPurchaseOrders(q = "OrderNumber=\"$orderNumber\"")
        resp.items.firstOrNull() ?: error("PO not found")
    }

    suspend fun fetchPoLines(poHeaderId: String): Result<List<PoLineItem>> = runCatching {
        val raw = api.getPoLines(poHeaderId).items

        // each PO line with GTIN using GTINRelationships API
        raw.map { line ->
            val itemNum = line.Item.trim().orEmpty()
            val gtin = fetchGtin(itemNum)
            if (gtin != null) {
                Log.d("GRN", "GTIN for $itemNum -> $gtin")
            } else {
                Log.d("GRN", "No GTIN found for $itemNum")
            }
            line.copy(GTIN = gtin)
        }
    }

    private suspend fun fetchGtin(itemNumber: String): String? {
        return try {
            val resp = api.getGtinForItem(q = "Item=$itemNumber")
            resp.items.firstOrNull()?.GTIN
        } catch (e: Exception) {
            Log.e("GRN", "GTIN lookup failed for $itemNumber: ${e.message}")
            null
        }
    }

    suspend fun createReceipt(request: ReceiptRequest): Result<ReceiptResponse> = runCatching {
        api.createReceipt(request)
    }

    suspend fun fetchProcessingErrors(
        headerInterfaceId: String,
        interfaceTransactionId: String
    ): Result<List<ProcessingError>> = runCatching {
        api.getProcessingErrors(headerInterfaceId, interfaceTransactionId).items
    }

    suspend fun uploadAttachment(
        receiptId: String,
        body: AttachmentRequest
    ): Result<AttachmentResponse> = runCatching {
        api.uploadReceiptAttachment(receiptId.toString(), body)
    }
}
