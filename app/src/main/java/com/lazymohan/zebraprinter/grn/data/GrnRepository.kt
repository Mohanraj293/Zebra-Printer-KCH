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

    // Step 3a: shipment lines for a TO number (fallback to get ShipmentNumber)
    suspend fun getShipmentForToNumber(toNumber: String): Result<ShipmentLine?> = runCatching {
        api.getShipmentLines(q = "Order=$toNumber").firstOrNull()
    }

    // Step 3b: shipment lines for a TO number + Item (to build sections per lot)
    suspend fun getShipmentLinesForOrderAndItem(orderNumber: String, itemNumber: String): Result<List<ShipmentLine>> =
        runCatching {
            // Example q: Order=103006;Item="PH11233"
            val q = "Order=$orderNumber;Item=\"$itemNumber\""
            api.getShipmentLines(q = q).list()
        }

    // Step 2.2: lot expiry lookup
    suspend fun getLotExpiry(lotNumber: String): Result<String?> = runCatching {
        val resp = api.getInventoryItemLots(q = "LotNumber=\"$lotNumber\"")
        resp.items.firstOrNull()?.lotExpirationDate
    }
}
