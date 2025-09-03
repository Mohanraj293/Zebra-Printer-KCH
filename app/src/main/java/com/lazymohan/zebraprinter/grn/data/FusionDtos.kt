package com.lazymohan.zebraprinter.grn.data

import com.google.gson.annotations.SerializedName

/* =========================================================
   PO FLOW DTOS  (UNCHANGED – keep old flow working)
   ========================================================= */

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

// --- Receipt request (PO flow) ---
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

// --- Receipt response (works for PO & TO) ---
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
    val lines: List<ReceiptLineResponse>? = null,

    // Oracle sometimes returns generic "Message" for failures
    @SerializedName("Message")
    val Message: String? = null
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

/* =========================================================
   TO FLOW DTOS (NEW) — added with distinct names to avoid
   breaking the PO flow.
   ========================================================= */

// --- STEP 1: Transfer Order Header Search ---
data class TransferOrderHeader(
    @SerializedName("HeaderId") val headerId: Long,
    @SerializedName("HeaderNumber") val headerNumber: String,
    @SerializedName("BusinessUnitName") val businessUnitName: String? = null,
    @SerializedName("Status") val status: String? = null,
    @SerializedName("InterfaceStatus") val interfaceStatus: String? = null
)

data class ToHeaderSearchResponse(
    @SerializedName("items") val items: List<TransferOrderHeader> = emptyList()
)

// --- STEP 2: Transfer Order Lines ---
data class TransferOrderLine(
    @SerializedName("TransferOrderLineId") val transferOrderLineId: Long,
    @SerializedName("TransferOrderHeaderId") val transferOrderHeaderId: Long,
    @SerializedName("ItemNumber") val itemNumber: String,
    @SerializedName("Subinventory") val subinventory: String? = null,
    @SerializedName("UnitOfMeasure") val unitOfMeasure: String? = null,
    @SerializedName("LineNumber") val lineNumber: Int? = null,
    @SerializedName("Quantity") val quantity: Double? = null
)

data class ToLinesResponse(
    @SerializedName("items") val items: List<TransferOrderLine> = emptyList()
)

// --- STEP 3: Shipment Lines (by TO number) ---
data class ShipmentLine(
    @SerializedName("ShipmentNumber") val shipmentNumber: Long? = null,
    @SerializedName("LotNumber") val lotNumber: String? = null
)

data class ShipmentLinesResponse(
    // normal oracle response
    @SerializedName("items") val items: List<ShipmentLine>? = null,
    // some tenants return a bare object (your sample)
    @SerializedName("ShipmentNumber") val shipmentNumber: Long? = null,
    @SerializedName("LotNumber") val lotNumber: String? = null
) {
    fun firstOrNull(): ShipmentLine? =
        when {
            !items.isNullOrEmpty()                     -> items.first()
            shipmentNumber != null || lotNumber != null -> ShipmentLine(shipmentNumber, lotNumber)
            else                                       -> null
        }
}

// --- STEP 4: Receipt (create GRN) against TO ---
data class ReceiptRequestTo(
    @SerializedName("FromOrganizationCode") val fromOrganizationCode: String,
    @SerializedName("OrganizationCode") val organizationCode: String,
    @SerializedName("EmployeeId") val employeeId: Long,
    @SerializedName("ReceiptSourceCode") val receiptSourceCode: String, // "TRANSFER ORDER"
    @SerializedName("ShipmentNumber") val shipmentNumber: Long?,
    @SerializedName("lines") val lines: List<ReceiptLineTo>
)

data class ReceiptLineTo(
    @SerializedName("SourceDocumentCode") val sourceDocumentCode: String,   // "TRANSFER ORDER"
    @SerializedName("ReceiptSourceCode") val receiptSourceCode: String,     // "TRANSFER ORDER"
    @SerializedName("TransactionType") val transactionType: String,         // "RECEIVE"
    @SerializedName("AutoTransactCode") val autoTransactCode: String,       // "DELIVER"
    @SerializedName("DocumentNumber") val documentNumber: Long?,            // ShipmentNumber
    @SerializedName("DocumentLineNumber") val documentLineNumber: Int,      // usually 1..N
    @SerializedName("ItemNumber") val itemNumber: String,
    @SerializedName("OrganizationCode") val organizationCode: String,
    @SerializedName("Quantity") val quantity: Int,
    @SerializedName("UnitOfMeasure") val unitOfMeasure: String? = null,
    @SerializedName("Subinventory") val subinventory: String? = null,
    // TO linkages:
    @SerializedName("TransferOrderHeaderId") val transferOrderHeaderId: Long,
    @SerializedName("TransferOrderLineId") val transferOrderLineId: Long,
    // Lots:
    @SerializedName("lotItemLots") val lotItemLots: List<LotEntryTo>? = null
)

data class LotEntryTo(
    @SerializedName("LotNumber") val lotNumber: String,
    @SerializedName("TransactionQuantity") val transactionQuantity: Int
)
