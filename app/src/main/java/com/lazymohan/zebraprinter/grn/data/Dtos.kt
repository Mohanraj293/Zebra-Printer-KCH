// app/src/main/java/com/lazymohan/zebraprinter/grn/data/FusionDtos.kt
package com.lazymohan.zebraprinter.grn.data

import com.google.gson.annotations.SerializedName

// --- PO lookup ---
data class PoResponse(
    @SerializedName("items")
    val items: List<PoItem> = emptyList()
)

data class PoItem(
    @SerializedName("OrderNumber")
    val OrderNumber: String = "",

    @SerializedName("Supplier")
    val Supplier: String = "",

    @SerializedName(value = "SupplierSite", alternate = ["VendorSiteCode"])
    val SupplierSite: String = "",

    @SerializedName(value = "ProcurementBU", alternate = ["BusinessUnit"])
    val ProcurementBU: String = "",

    @SerializedName("SoldToLegalEntity")
    val SoldToLegalEntity: String = "",

    @SerializedName(value = "POHeaderId", alternate = ["PoHeaderId"])
    val POHeaderId: String = ""
)

// --- PO lines ---
data class PoLinesResponse(
    @SerializedName("items")
    val items: List<PoLineItem> = emptyList()
)

data class PoLineItem(
    @SerializedName("LineNumber")
    val LineNumber: Int = 0,

    @SerializedName("Item")
    val Item: String = "",

    @SerializedName(value = "Quantity", alternate = ["OrderedQuantity"])
    val Quantity: Double = 0.0,

    @SerializedName(value = "UOM", alternate = ["UnitOfMeasure"])
    val UOM: String = "EA",

    @SerializedName(value = "Description", alternate = ["ItemDescription"])
    val Description: String? = null,
    val GTIN: String? = null,
    @SerializedName("Price")
    val Price: Double? = null
)

// --- GTIN lookup ---
data class GtinResponse(
    @SerializedName("items")
    val items: List<GtinItem> = emptyList()
)
data class GtinItem(
    @SerializedName("Item")
    val Item: String? = null,
    @SerializedName("GTIN")
    val GTIN: String? = null
)

// --- Receipt request ---
data class ReceiptRequest(
    @SerializedName("ReceiptSourceCode")
    val ReceiptSourceCode: String = "VENDOR",

    @SerializedName("ReceiptHeaderId")
    val ReceiptHeaderId: Long? = null,

    @SerializedName(value = "OrganizationCode", alternate = ["ShipToOrganizationCode"])
    val OrganizationCode: String,

    @SerializedName(value = "VendorName", alternate = ["Supplier"])
    val VendorName: String,

    @SerializedName(value = "VendorSiteCode", alternate = ["SupplierSite"])
    val VendorSiteCode: String,

    @SerializedName(value = "BusinessUnit", alternate = ["ProcurementBU"])
    val BusinessUnit: String,

    @SerializedName("EmployeeId")
    val EmployeeId: Long,

    @SerializedName(value = "lines", alternate = ["Lines"])
    val lines: List<ReceiptLine>
)

data class ReceiptLine(
    @SerializedName("ReceiptSourceCode")
    val ReceiptSourceCode: String = "VENDOR",

    @SerializedName("SourceDocumentCode")
    val SourceDocumentCode: String = "PO",

    @SerializedName("TransactionType")
    val TransactionType: String = "RECEIVE",

    @SerializedName("AutoTransactCode")
    val AutoTransactCode: String = "DELIVER",

    @SerializedName(value = "OrganizationCode", alternate = ["ShipToOrganizationCode"])
    val OrganizationCode: String,

    // PO number
    @SerializedName(value = "DocumentNumber", alternate = ["OrderNumber"])
    val DocumentNumber: String,

    // PO line number
    @SerializedName(value = "DocumentLineNumber", alternate = ["LineNumber"])
    val DocumentLineNumber: Int,

    // Item number
    @SerializedName(value = "ItemNumber", alternate = ["Item"])
    val ItemNumber: String,

    @SerializedName(value = "Quantity", alternate = ["OrderedQuantity"])
    val Quantity: Int,

    @SerializedName(value = "UnitOfMeasure", alternate = ["UOM"])
    val UnitOfMeasure: String = "EA",

    @SerializedName(value = "SoldtoLegalEntity", alternate = ["SoldToLegalEntity"])
    val SoldtoLegalEntity: String,

    @SerializedName(value = "Subinventory", alternate = ["SubInventory"])
    val Subinventory: String,

    @SerializedName("Locator")
    val Locator: String,

    @SerializedName("DestinationTypeCode")
    val DestinationTypeCode: String = "INVENTORY",

    @SerializedName("lotItemLots")
    val lotItemLots: List<LotItem>
)

data class LotItem(
    @SerializedName("LotNumber")
    val LotNumber: String,

    @SerializedName(value = "TransactionQuantity", alternate = ["Quantity"])
    val TransactionQuantity: Int,

    @SerializedName(value = "LotExpirationDate", alternate = ["ExpirationDate"])
    val LotExpirationDate: String
)

// --- Receipt response ---
data class ReceiptResponse(
    @SerializedName("ReceiptNumber")
    val ReceiptNumber: String? = null,

    @SerializedName("ReturnStatus")
    val ReturnStatus: String? = null,

    @SerializedName("HeaderInterfaceId")
    val HeaderInterfaceId: String? = null,

    @SerializedName("ReceiptHeaderId")
    val ReceiptHeaderId: Long? = null,

    // your code uses lowercase `lines`
    @SerializedName(value = "lines", alternate = ["Lines"])
    val lines: List<ReceiptLineResponse>? = null
)

data class ReceiptLineResponse(
    @SerializedName("InterfaceTransactionId")
    val InterfaceTransactionId: String? = null,

    @SerializedName(value = "ItemDescription", alternate = ["Description"])
    val ItemDescription: String? = null
)

// --- Errors ---

data class ProcessingErrorsResponse(
    @SerializedName("items")
    val items: List<ProcessingError> = emptyList()
)

data class ProcessingError(
    @SerializedName(value = "ItemDescription", alternate = ["Description"])
    val ItemDescription: String? = null,

    @SerializedName(value = "ErrorMessage", alternate = ["ProcessingErrorMessage", "Message"])
    val ErrorMessage: String? = null
)

// --- Attachments ---

data class AttachmentRequest(
    @SerializedName(value = "UploadedFileName", alternate = ["FileName"])
    val UploadedFileName: String,

    @SerializedName("CategoryName")
    val CategoryName: String = "MISC",

    @SerializedName(value = "FileContents", alternate = ["Content"])
    val FileContents: String,

    @SerializedName("Title")
    val Title: String? = null
)

data class AttachmentResponse(
    @SerializedName("AttachmentId")
    val AttachmentId: String? = null,

    @SerializedName(value = "UploadedFileName", alternate = ["FileName"])
    val UploadedFileName: String? = null
)
