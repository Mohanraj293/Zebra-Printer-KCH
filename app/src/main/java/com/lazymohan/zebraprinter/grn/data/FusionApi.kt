package com.lazymohan.zebraprinter.grn.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Oracle Fusion REST APIs for PO, lines, GTIN, GRN (receipt requests) and attachments.
 *
 * NOTE:
 * - Do NOT pass Authorization headers here. Your Retrofit client should add
 *   "Authorization: Bearer <token>" via an OkHttp interceptor using AppPref.
 * - Most endpoints accept ?onlyData=true to reduce HAL payload. Keep it on where applicable.
 */
interface FusionApi {

    // --- Step 1: Purchase Orders by OrderNumber ---
    @Headers("Accept: application/json")
    @GET("fscmRestApi/resources/11.13.18.05/purchaseOrders")
    suspend fun getPurchaseOrders(
        @Query("onlyData") onlyData: String = "true",
        // Example: q = "OrderNumber=\"12345\""
        @Query("q") q: String
    ): PoResponse

    // --- Step 2: Lines for a specific PO header ---
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

    // --- Step 4: Create receipt (GRN) OR add lines to same GRN (when ReceiptHeaderId present) ---
    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @POST("fscmRestApi/resources/11.13.18.05/receivingReceiptRequests")
    suspend fun createReceipt(
        @Body body: ReceiptRequest
    ): ReceiptResponse

    // --- Step 5: Processing errors for a line ---
    @Headers("Accept: application/json")
    @GET(
        "fscmRestApi/resources/11.13.18.05/receivingReceiptRequests/{headerInterfaceId}/child/lines/{interfaceTransactionId}/child/processingErrors"
    )
    suspend fun getProcessingErrors(
        @Path("headerInterfaceId") headerInterfaceId: String,
        @Path("interfaceTransactionId") interfaceTransactionId: String,
        @Query("onlyData") onlyData: String = "true"
    ): ProcessingErrorsResponse

    // --- Step 6: Upload attachments against the receipt request (use ReceiptHeaderId as {receiptid}) ---
    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @POST("fscmRestApi/resources/11.13.18.05/receivingReceiptRequests/{receiptid}/child/attachments")
    suspend fun uploadReceiptAttachment(
        @Path("receiptid") receiptId: String,
        @Body body: AttachmentRequest
    ): AttachmentResponse
}
