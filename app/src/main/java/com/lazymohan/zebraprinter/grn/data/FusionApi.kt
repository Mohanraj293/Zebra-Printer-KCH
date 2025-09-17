package com.lazymohan.zebraprinter.grn.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Oracle Fusion REST APIs for PO, lines, GTIN, GRN (receipt requests), TO, and attachments.
 */
interface FusionApi {

    // --- PO: Step 1: Purchase Orders by OrderNumber ---
    @Headers("Accept: application/json")
    @GET("fscmRestApi/resources/11.13.18.05/purchaseOrders")
    suspend fun getPurchaseOrders(
        @Query("onlyData") onlyData: String = "true",
        @Query("q") q: String
    ): PoResponse

    // --- PO: Step 2: Lines for a specific PO header ---
    @Headers("Accept: application/json")
    @GET("fscmRestApi/resources/11.13.18.05/purchaseOrders/{poHeaderId}/child/lines")
    suspend fun getPoLines(
        @Path("poHeaderId") poHeaderId: String,
        @Query("onlyData") onlyData: String = "true"
    ): PoLinesResponse

    // --- GTIN lookup for an Item ---
    @Headers("Accept: application/json")
    @GET("fscmRestApi/resources/11.13.18.05/GTINRelationships")
    suspend fun getGtinForItem(
        @Query("onlyData") onlyData: String = "true",
        @Query("q") q: String
    ): GtinResponse

    // --- GRN: Create receipt (PO or TO payloads) ---
    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("fscmRestApi/resources/11.13.18.05/receivingReceiptRequests")
    suspend fun createReceipt(@Body body: Any): Response<ReceiptResponse>

    // --- GRN: Processing errors for a line ---
    @Headers("Accept: application/json")
    @GET("fscmRestApi/resources/11.13.18.05/receivingReceiptRequests/{headerInterfaceId}/child/lines/{interfaceTransactionId}/child/processingErrors")
    suspend fun getProcessingErrors(
        @Path("headerInterfaceId") headerInterfaceId: String,
        @Path("interfaceTransactionId") interfaceTransactionId: String,
        @Query("onlyData") onlyData: String = "true"
    ): ProcessingErrorsResponse

    // --- GRN: Upload attachments (use ReceiptHeaderId for {receiptid}) ---
    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("fscmRestApi/resources/11.13.18.05/receivingReceiptRequests/{receiptid}/child/attachments")
    suspend fun uploadReceiptAttachment(
        @Path("receiptid") receiptId: String,
        @Body body: AttachmentRequest
    ): AttachmentResponse

    // =========================
    // Transfer Order (TO) APIs
    // =========================

    // STEP 1: Expected Shipment Lines (drives UI lines)
    @Headers("Accept: application/json")
    @GET("fscmRestApi/resources/11.13.18.05/receivingReceiptExpectedShipmentLines")
    suspend fun getExpectedShipmentLines(
        @Query("onlyData") onlyData: String = "true",
        @Query("q") q: String
    ): ExpectedShipmentLinesResponse

    // STEP 2: Shipment lines (to fetch Lot, Locator, ShippedQuantity, etc.)
    @Headers("Accept: application/json")
    @GET("fscmRestApi/resources/11.13.18.05/shipmentLines")
    suspend fun getShipmentLines(
        @Query("onlyData") onlyData: String = "true",
        @Query("q") q: String
    ): ShipmentLinesResponse

    // STEP 3: Inventory lots (expiry)
    @Headers("Accept: application/json")
    @GET("fscmRestApi/resources/11.13.18.05/inventoryItemLots")
    suspend fun getInventoryItemLots(
        @Query("onlyData") onlyData: String = "true",
        @Query("q") q: String
    ): InventoryLotsResponse

    // ====== Helpers ======

    // TO header details (business unit, status, etc.)
    @Headers("Accept: application/json")
    @GET("fscmRestApi/resources/11.13.18.05/transferOrders")
    suspend fun getToHeaders(
        @Query("onlyData") onlyData: String = "true",
        @Query("q") q: String
    ): ToHeaderSearchResponse

    // PO lifecycle → receipts
    @Headers("Accept: application/json")
    @GET("fscmRestApi/resources/11.13.18.05/purchaseOrderLifeCycleDetails/{poHeaderId}/child/receipts")
    suspend fun getPoLifecycleReceipts(
        @Path("poHeaderId") poHeaderId: String,
        @Query("onlyData") onlyData: String = "true"
    ): PoLifecycleReceiptsResponse

    @Headers("Accept: application/json")
    @GET("fscmRestApi/resources/11.13.18.05/receivingReceiptRequests")
    suspend fun searchReceiptRequests(
        @Query("onlyData") onlyData: String = "true",
        @Query("q") q: String
    ): ReceiptRequestsSearchResponse
}
