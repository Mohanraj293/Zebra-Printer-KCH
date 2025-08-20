// app/src/main/java/com/lazymohan/zebraprinter/grn/data/FusionApi.kt
package com.lazymohan.zebraprinter.grn.data

import retrofit2.http.*

interface FusionApi {
    // Step 1: Purchase Orders by OrderNumber
    @GET("fscmRestApi/resources/11.13.18.05/purchaseOrders")
    suspend fun getPurchaseOrders(
        @Query("onlyData") onlyData: String = "true",
        @Query("q") q: String // q='OrderNumber="PO-2024-3456"'
    ): PoResponse

    // Step 2: Lines for a specific PO header
    @GET("fscmRestApi/resources/11.13.18.05/purchaseOrders/{poHeaderId}/child/lines")
    suspend fun getPoLines(
        @Path("poHeaderId") poHeaderId: String
    ): PoLinesResponse

    // Step 4: Create receipt (GRN)
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
}
