// app/src/main/java/com/lazymohan/zebraprinter/grn/data/Dtos.kt
package com.lazymohan.zebraprinter.grn.data

// --- PO lookup ---
data class PoResponse(val items: List<PoItem> = emptyList())
data class PoItem(
    val OrderNumber: String,
    val Supplier: String,
    val SupplierSite: String,
    val ProcurementBU: String,
    val SoldToLegalEntity: String,
    val POHeaderId: String
)

// --- PO lines ---
data class PoLinesResponse(val items: List<PoLineItem> = emptyList())
data class PoLineItem(
    val LineNumber: Int,
    val Item: String,
    val Quantity: Double,
    val UOM: String,
    val Description: String? = null,
    val GTIN: String? = null
)

// --- GTIN lookup ---
data class GtinResponse(val items: List<GtinItem> = emptyList())
data class GtinItem(
    val Item: String? = null,
    val GTIN: String? = null
)

// --- Receipt request ---
data class ReceiptRequest(
    val ReceiptSourceCode: String = "VENDOR",
    val ReceiptHeaderId: Long? = null,
    val OrganizationCode: String,
    val VendorName: String,
    val VendorSiteCode: String,
    val BusinessUnit: String,
    val EmployeeId: String,
    val lines: List<ReceiptLine>
)

data class ReceiptLine(
    val ReceiptSourceCode: String = "VENDOR",
    val SourceDocumentCode: String = "PO",
    val TransactionType: String = "RECEIVE",
    val AutoTransactCode: String = "DELIVER",
    val OrganizationCode: String,
    val DocumentNumber: String,
    val DocumentLineNumber: Int,
    val ItemNumber: String,
    val Quantity: Double,
    val UnitOfMeasure: String,
    val SoldtoLegalEntity: String,
    val Subinventory: String,
    val Locator: String,
    val DestinationTypeCode: String = "INVENTORY",
    val lotItemLots: List<LotItem>
)

data class LotItem(
    val LotNumber: String,
    val TransactionQuantity: Double,
    val LotExpirationDate: String
)

// --- Receipt response ---
data class ReceiptResponse(
    val ReceiptNumber: String? = null,
    val ReturnStatus: String? = null,
    val HeaderInterfaceId: String? = null,
    val ReceiptHeaderId: Long? = null,
    val lines: List<ReceiptLineResponse>? = null
)

data class ReceiptLineResponse(
    val InterfaceTransactionId: String? = null,
    val ItemDescription: String? = null
)

// --- Errors ---
data class ProcessingErrorsResponse(val items: List<ProcessingError> = emptyList())
data class ProcessingError(
    val ItemDescription: String? = null,
    val ErrorMessage: String? = null
)

// --- Attachments ---
data class AttachmentRequest(
    val UploadedFileName: String,
    val CategoryName: String = "MISC",
    val FileContents: String,
    val Title: String? = null
)

data class AttachmentResponse(
    val AttachmentId: String? = null,
    val UploadedFileName: String? = null
)
