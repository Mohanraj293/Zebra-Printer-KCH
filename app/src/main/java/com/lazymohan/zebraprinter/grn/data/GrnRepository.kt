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
class GrnRepository(private val api: FusionApi) {

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
        if (resp.isSuccessful) resp.body() ?: ReceiptResponse(ReturnStatus = "SUCCESS")
        else throw HttpException(resp)
    }

    suspend fun fetchProcessingErrors(
        headerInterfaceId: String,
        interfaceTransactionId: String
    ): Result<List<ProcessingError>> = runCatching {
        api.getProcessingErrors(headerInterfaceId, interfaceTransactionId).items
    }

    suspend fun uploadAttachment(receiptId: String, body: AttachmentRequest): Result<AttachmentResponse> =
        runCatching { api.uploadReceiptAttachment(receiptId, body) }

    // ====== TO ======

    // Header details
    suspend fun findToHeaderByNumber(toNumber: String): Result<TransferOrderHeader?> = runCatching {
        val q = "HeaderNumber=\"$toNumber\""
        api.getToHeaders(q = q).items.firstOrNull()
    }

    /**
     * STEP 1: Expected shipment lines for this TO (default OrganizationCode=KDH).
     * We map them into TransferOrderLine used by the UI.
     */
    suspend fun fetchExpectedShipmentLines(
        toNumber: String,
        orgCode: String = "KDH"
    ): Result<List<TransferOrderLine>> = runCatching {
        val q = "OrganizationCode=$orgCode;TransferOrderNumber=$toNumber"
        api.getExpectedShipmentLines(q = q).items.map { e ->
            TransferOrderLine(
                transferOrderLineId = e.transferOrderLineId,
                transferOrderHeaderId = e.transferOrderHeaderId,
                itemDescription = e.itemDescription,
                itemNumber = e.itemNumber,
                subinventory = e.subinventory,
                unitOfMeasure = e.unitOfMeasure,
                lineNumber = e.transferOrderLineNumber?.toIntOrNull(),
                quantity = e.orderedQuantity,
                unitPrice = null,
                destinationOrganizationCode = e.organizationCode,
                sourceOrganizationCode = e.fromOrganizationCode,
                intransitShipmentNumber = e.intransitShipmentNumber,
                shipmentLineNumber = e.shipmentLineNumber,
                documentLineId = e.documentLineId
            )
        }
    }

    // STEP 2: Shipments per item (for lot, locator, shipped qty)
    suspend fun getShipmentLinesForOrderAndItem(
        toNumber: String,
        item: String
    ): Result<List<ShipmentLine>> = runCatching {
        api.getShipmentLines(q = "Order=$toNumber;Item=\"$item\"")
            .list()
            .filter { it.shipmentId != null } // only shipments that can be received
    }

    // STEP 3: Lot expiry — include org filter (default KDH)
    suspend fun getLotExpiry(lot: String, orgCode: String = "KDH"): Result<String?> = runCatching {
        val q = "LotNumber=\"$lot\";OrganizationCode=$orgCode"
        val item = api.getInventoryItemLots(q = q).items.firstOrNull()
        item?.lotExpirationDate?.take(10)
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
