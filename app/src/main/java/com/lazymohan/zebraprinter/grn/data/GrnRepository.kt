package com.lazymohan.zebraprinter.grn.data

import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException

private const val TAG = "GRN"

/**
 * Repository for GRN flow (PO + TO).
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
        coroutineScope {
            lines.map { line ->
                async {
                    val itemNum = line.Item.trim()
                    val gtin = itemNum.takeIf { it.isNotEmpty() }?.let { safeItem ->
                        try {
                            api.getGtinForItem(q = "Item=\"$safeItem\"").items.firstOrNull()?.GTIN
                        } catch (e: Exception) {
                            Log.e(TAG, "GTIN lookup failed for $safeItem: ${e.message}", e)
                            null
                        }
                    }
                    line.copy(GTIN = gtin)
                }
            }.awaitAll()
        }
    }

    // --- Create receipt (PO or TO) ---
    suspend fun createReceipt(body: Any): Result<ReceiptResponse> = runCatching {
        val resp = api.createReceipt(body)
        if (resp.isSuccessful) {
            resp.body() ?: ReceiptResponse(ReturnStatus = "SUCCESS")
        } else {
            throw HttpException(resp)
        }
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

    // Step 3a – shipment for whole TO
    suspend fun getShipmentForToNumber(toNumber: String): Result<ShipmentLine?> = runCatching {
        api.getShipmentLines(q = "Order=$toNumber").firstOrNull()
    }

    // Step 3b – shipments per item
    suspend fun getShipmentLinesForOrderAndItem(
        toNumber: String,
        item: String
    ): Result<List<ShipmentLine>> = runCatching {
        api.getShipmentLines(q = "Order=$toNumber;Item=\"$item\"").list()
    }

    // Step 2.2 – fetch expiry for a lot
    suspend fun getLotExpiry(lot: String): Result<String?> = runCatching {
        val q = "LotNumber=\"$lot\""
        val item = api.getInventoryItemLots(q = q).items.firstOrNull()
        item?.lotExpirationDate?.take(10) // YYYY-MM-DD
    }

    suspend fun getLifecycleReceiptsByPo(poNumber: String): Result<List<PoLifecycleReceipt>> = runCatching {
        val po = fetchPo(poNumber).getOrThrow()
        api.getPoLifecycleReceipts(po.POHeaderId).items
    }

    suspend fun findHeaderByReceiptHeaderIdSuccess(receiptHeaderId: Long): Result<ReceiptResponse?> = runCatching {
        val q = "ReceiptHeaderId=$receiptHeaderId;ProcessingStatusCode=\"SUCCESS\""
        api.searchReceiptRequests(q = q).items.firstOrNull()
    }
}
