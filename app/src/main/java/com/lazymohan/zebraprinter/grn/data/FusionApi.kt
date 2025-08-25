package com.lazymohan.zebraprinter.grn.data

import retrofit2.http.*

interface FusionApi {
    // Step 1: Purchase Orders by OrderNumber
    @GET("fscmRestApi/resources/11.13.18.05/purchaseOrders")
    suspend fun getPurchaseOrders(
        @Query("onlyData") onlyData: String = "true",
        @Query("q") q: String
    ): PoResponse

    // Step 2: Lines for a specific PO header
    @GET("fscmRestApi/resources/11.13.18.05/purchaseOrders/{poHeaderId}/child/lines")
    suspend fun getPoLines(
        @Path("poHeaderId") poHeaderId: String
    ): PoLinesResponse

    // GTIN lookup for an Item
    @GET("fscmRestApi/resources/11.13.18.05/GTINRelationships")
    suspend fun getGtinForItem(
        @Query("onlyData") onlyData: String = "true",
        @Query("q") q: String
    ): GtinResponse

    // Step 4: Create receipt (GRN) OR Add to the same GRN (when ReceiptHeaderId present)
    @POST("fscmRestApi/resources/11.13.18.05/receivingReceiptRequests")
    suspend fun createReceipt(
        @Body body: ReceiptRequest
    ): ReceiptResponse

    // Step 5: Processing errors for a line
    @GET("fscmRestApi/resources/11.13.18.05/receivingReceiptRequests/{headerInterfaceId}/child/lines/{interfaceTransactionId}/child/processingErrors")
    suspend fun getProcessingErrors(
        @Path("headerInterfaceId") headerInterfaceId: String,
        @Path("interfaceTransactionId") interfaceTransactionId: String
    ): ProcessingErrorsResponse

    // Step 6: Upload attachments against the receipt request (use ReceiptHeaderId as {receiptid})
    @POST("fscmRestApi/resources/11.13.18.05/receivingReceiptRequests/{receiptid}/child/attachments")
    suspend fun uploadReceiptAttachment(
        @Path("receiptid") receiptId: String,
        @Body body: AttachmentRequest
    ): AttachmentResponse
}
