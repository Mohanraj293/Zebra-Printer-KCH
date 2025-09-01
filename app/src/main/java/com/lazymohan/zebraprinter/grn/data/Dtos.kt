package com.lazymohan.zebraprinter.grn.data

import com.google.gson.annotations.SerializedName

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
    val Quantity: Int,
    val UOM: String = "EA",
    val Description: String? = null,
    val GTIN: String? = null,
    val Price: Double? = null
)

// --- GTIN lookup ---
data class GtinResponse(val items: List<GtinItem> = emptyList())
data class GtinItem(
    val Item: String? = null,
    val GTIN: String? = null
)

// --- Receipt request (PO) ---
data class ReceiptRequest(
    val ReceiptSourceCode: String = "VENDOR",
    val ReceiptHeaderId: Long? = null,
    val OrganizationCode: String,
    val VendorName: String,
    val VendorSiteCode: String,
    val BusinessUnit: String,
    val EmployeeId: Long,
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
    val UnitOfMeasure: String = "EA",
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

// --- Receipt response (common) ---
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

// --- Errors (common) ---
data class ProcessingErrorsResponse(val items: List<ProcessingError> = emptyList())
data class ProcessingError(
    val ItemDescription: String? = null,
    val ErrorMessage: String? = null
)

// --- Attachments (common) ---
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

// Transfer Order DTOs

// --- Transfer Order header lookup ---
data class TransferOrderResponse(val items: List<TransferOrderItem> = emptyList())

data class TransferOrderItem(
    val HeaderId: Long,
    val HeaderNumber: String,
    val BusinessUnitName: String? = null,
    val Status: String? = null,
    val InterfaceStatus: String? = null
)

// --- Transfer Order lines ---
data class TransferOrderLinesResponse(val items: List<TransferOrderLineItem> = emptyList())

data class TransferOrderLineItem(
    @SerializedName("LineId") val TransferOrderLineId: Int,
    @SerializedName("HeaderId") val TransferOrderHeaderId: Long,
    @SerializedName("ItemNumber") val ItemNumber: String,
    @SerializedName("ItemDescription") val ItemDescription: String,
    @SerializedName("RequestedQuantity") val RequestedQuantity: Int,
    @SerializedName("SourceSubinventoryCode") val Subinventory: String? = null,
    @SerializedName("QuantityUOMName") val UnitOfMeasure: String? = "EA",
    @SerializedName("LineNumber") val LineNumber: Int? = null
)

// --- Shipment lines by TO number (to fetch ShipmentNumber & LotNumber) ---
data class ShipmentLinesResponse(val items: List<ShipmentLineItem> = emptyList())

data class ShipmentLineItem(
    val ShipmentNumber: String? = null,
    val LotNumber: String? = null
)

// --- Transfer Order receipt payload ---
data class TransferReceiptRequest(
    val FromOrganizationCode: String,
    val OrganizationCode: String,
    val EmployeeId: String,
    val ReceiptSourceCode: String = "TRANSFER ORDER",
    val ShipmentNumber: String,
    val ReceiptHeaderId: Long? = null, // used to add lines to same receipt
    val lines: List<TransferReceiptLine>
)

data class TransferReceiptLine(
    val SourceDocumentCode: String = "TRANSFER ORDER",
    val ReceiptSourceCode: String = "TRANSFER ORDER",
    val TransactionType: String = "RECEIVE",
    val AutoTransactCode: String = "DELIVER",
    val DocumentNumber: String,
    val DocumentLineNumber: Int,
    val ItemNumber: String,
    val OrganizationCode: String,   // receiving org (same as header OrganizationCode)
    val Quantity: Double,
    val UnitOfMeasure: String = "EA",
    val Subinventory: String? = null,

    val TransferOrderHeaderId: Long,
    val TransferOrderLineId: Int,

    val lotItemLots: List<TOLotItem>
)

data class TOLotItem(
    val LotNumber: String,
    val TransactionQuantity: Double
)