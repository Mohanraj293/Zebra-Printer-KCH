// app/src/main/java/com/lazymohan/zebraprinter/grn/data/GrnRepository.kt
package com.lazymohan.zebraprinter.grn.data

class GrnRepository(private val api: FusionApi) {

    suspend fun fetchPo(orderNumber: String): Result<PoItem> = runCatching {
        val resp = api.getPurchaseOrders(q = "OrderNumber=\"$orderNumber\"")
        resp.items.firstOrNull() ?: error("PO not found")
    }

    suspend fun fetchPoLines(poHeaderId: String): Result<List<PoLineItem>> = runCatching {
        api.getPoLines(poHeaderId).items
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
        api.uploadReceiptAttachment(receiptId, body)
    }

    suspend fun uploadAttachments(
        receiptId: String,
        files: List<AttachmentRequest>
    ): Result<List<AttachmentResponse>> = runCatching {
        val out = mutableListOf<AttachmentResponse>()
        for (f in files) {
            runCatching { api.uploadReceiptAttachment(receiptId, f) }
                .onSuccess { out += it }
                .onFailure { }
        }
        out
    }
}
