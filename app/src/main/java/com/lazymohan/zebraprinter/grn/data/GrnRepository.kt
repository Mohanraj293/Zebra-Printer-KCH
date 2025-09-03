// app/src/main/java/com/lazymohan/zebraprinter/grn/data/GrnRepository.kt
package com.lazymohan.zebraprinter.grn.data

import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private const val TAG = "GRN"

/**
 * Repository for GRN flow (PO + TO).
 * Retrofit client should inject OAuth Bearer automatically.
 */
class GrnRepository(
    private val api: FusionApi
) {

    // ====== PO ======

    suspend fun fetchPo(orderNumber: String): Result<PoItem> = runCatching {
        val q = "OrderNumber=\"$orderNumber\""
        val resp = api.getPurchaseOrders(q = q)
        resp.items.firstOrNull() ?: error("PO not found for OrderNumber=$orderNumber")
    }

    suspend fun fetchPoLines(poHeaderId: String): Result<List<PoLineItem>> = runCatching {
        val lines = api.getPoLines(poHeaderId).items

        // Enrich GTIN in parallel to cut down latency
        coroutineScope {
            lines.map { line ->
                async {
                    val itemNum = line.Item.trim()
                    val gtin = itemNum.takeIf { it.isNotEmpty() }?.let { safeItem ->
                        fetchGtin(safeItem).also { g ->
                            if (g.isNullOrEmpty()) {
                                Log.d(TAG, "No GTIN found for $safeItem")
                            } else {
                                Log.d(TAG, "GTIN for $safeItem -> $g")
                            }
                        }
                    }
                    line.copy(GTIN = gtin)
                }
            }.awaitAll()
        }
    }

    private suspend fun fetchGtin(itemNumber: String): String? = try {
        val resp = api.getGtinForItem(q = "Item=\"$itemNumber\"")
        resp.items.firstOrNull()?.GTIN
    } catch (e: Exception) {
        Log.e(TAG, "GTIN lookup failed for $itemNumber: ${e.message}", e)
        null
    }

    // Unified: works for PO ReceiptRequest or TO payload
    suspend fun createReceipt(body: Any): Result<ReceiptResponse> = runCatching {
        api.createReceipt(body)
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
        api.uploadReceiptAttachment(receiptId, body)
    }

    // ====== TO ======

    // Step 1
    suspend fun findToHeaderByNumber(toNumber: String): Result<TransferOrderHeader?> = runCatching {
        val q = "HeaderNumber=\"$toNumber\""
        api.getToHeaders(q = q).items.firstOrNull()
    }

    // Step 2
    suspend fun getToLines(headerId: Long): Result<List<TransferOrderLine>> = runCatching {
        api.getToLines(headerId).items
    }

    // Step 3
    suspend fun getShipmentForToNumber(toNumber: String): Result<ShipmentLine?> = runCatching {
        api.getShipmentLinesByOrder(q = "Order=$toNumber").firstOrNull()
    }
}
