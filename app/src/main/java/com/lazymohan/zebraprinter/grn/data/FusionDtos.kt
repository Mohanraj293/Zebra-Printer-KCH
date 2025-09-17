package com.lazymohan.zebraprinter.grn.data

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

// PO FLOW DTOS

data class PoResponse(@SerializedName("items") val items: List<PoItem> = emptyList())

data class PoItem(
    @SerializedName("OrderNumber") val OrderNumber: String = "",
    @SerializedName("Supplier") val Supplier: String = "",
    @SerializedName(value = "SupplierSite", alternate = ["VendorSiteCode"]) val SupplierSite: String = "",
    @SerializedName(value = "ProcurementBU", alternate = ["BusinessUnit"]) val ProcurementBU: String = "",
    @SerializedName("SoldToLegalEntity") val SoldToLegalEntity: String = "",
    @SerializedName(value = "POHeaderId", alternate = ["PoHeaderId"]) val POHeaderId: String = ""
)

data class PoLinesResponse(@SerializedName("items") val items: List<PoLineItem> = emptyList())

data class PoLineItem(
    @SerializedName("LineNumber") val LineNumber: Int = 0,
    @SerializedName("Item") val Item: String = "",
    @SerializedName(value = "Quantity", alternate = ["OrderedQuantity"]) val Quantity: Double = 0.0,
    @SerializedName(value = "UOM", alternate = ["UnitOfMeasure"]) val UOM: String = "EA",
    @SerializedName(value = "Description", alternate = ["ItemDescription"]) val Description: String? = null,
    val GTIN: String? = null,
    @SerializedName("Price") val Price: Double? = null
)

data class GtinResponse(@SerializedName("items") val items: List<GtinItem> = emptyList())

data class GtinItem(@SerializedName("Item") val Item: String? = null, @SerializedName("GTIN") val GTIN: String? = null)

// PO Receipt request
data class ReceiptRequest(
    @SerializedName("ReceiptSourceCode") val ReceiptSourceCode: String = "VENDOR",
    @SerializedName("ReceiptHeaderId") val ReceiptHeaderId: Long? = null,
    @SerializedName(value = "OrganizationCode", alternate = ["ShipToOrganizationCode"]) val OrganizationCode: String,
    @SerializedName(value = "VendorName", alternate = ["Supplier"]) val VendorName: String,
    @SerializedName(value = "VendorSiteCode", alternate = ["SupplierSite"]) val VendorSiteCode: String,
    @SerializedName(value = "BusinessUnit", alternate = ["ProcurementBU"]) val BusinessUnit: String,
    @SerializedName("EmployeeId") val EmployeeId: Long,
    @SerializedName(value = "lines", alternate = ["Lines"]) val lines: List<ReceiptLine>,
    @SerializedName("InvoiceNumber") val InvoiceNumber: String? = null,
    @SerializedName("Comments") val Comments: String? = null,
    @SerializedName("DFF") val DFF: List<ReceiptRequestDff>? = null
)
data class ReceiptLine(
    @SerializedName("ReceiptSourceCode") val ReceiptSourceCode: String = "VENDOR",
    @SerializedName("SourceDocumentCode") val SourceDocumentCode: String = "PO",
    @SerializedName("TransactionType") val TransactionType: String = "RECEIVE",
    @SerializedName("AutoTransactCode") val AutoTransactCode: String = "DELIVER",
    @SerializedName(value = "OrganizationCode", alternate = ["ShipToOrganizationCode"]) val OrganizationCode: String,
    @SerializedName(value = "DocumentNumber", alternate = ["OrderNumber"]) val DocumentNumber: String,
    @SerializedName(value = "DocumentLineNumber", alternate = ["LineNumber"]) val DocumentLineNumber: Int,
    @SerializedName(value = "ItemNumber", alternate = ["Item"]) val ItemNumber: String,
    @SerializedName(value = "Quantity", alternate = ["OrderedQuantity"]) val Quantity: Int,
    @SerializedName(value = "UnitOfMeasure", alternate = ["UOM"]) val UnitOfMeasure: String = "EA",
    @SerializedName(value = "SoldtoLegalEntity", alternate = ["SoldToLegalEntity"]) val SoldtoLegalEntity: String,
    @SerializedName(value = "Subinventory", alternate = ["SubInventory"]) val Subinventory: String,
    @SerializedName("Locator") val Locator: String,
    @SerializedName("DestinationTypeCode") val DestinationTypeCode: String = "INVENTORY",
    @SerializedName("lotItemLots") val lotItemLots: List<LotItem>
)
data class ReceiptRequestDff(@SerializedName("supplierInvoiceNumber") val supplierInvoiceNumber: BigDecimal, @SerializedName("__FLEX_Context") val flexContext: String? = null)
data class LotItem(@SerializedName("LotNumber") val LotNumber: String, @SerializedName(value = "TransactionQuantity", alternate = ["Quantity"]) val TransactionQuantity: Int, @SerializedName(value = "LotExpirationDate", alternate = ["ExpirationDate"]) val LotExpirationDate: String)

// Common Receipt response
data class ReceiptResponse(
    @SerializedName("ReceiptNumber") val ReceiptNumber: String? = null,
    @SerializedName("ReturnStatus") val ReturnStatus: String? = null,
    @SerializedName("HeaderInterfaceId") val HeaderInterfaceId: String? = null,
    @SerializedName("ReceiptHeaderId") val ReceiptHeaderId: Long? = null,
    @SerializedName(value = "lines", alternate = ["Lines"]) val lines: List<ReceiptLineResponse>? = null,
    @SerializedName("Message") val Message: String? = null,
    @SerializedName("ReturnMessage") val ReturnMessage: String? = null,
    @SerializedName("ProcessingStatusCode") val ProcessingStatusCode: String? = null,
    @SerializedName("VendorName") val VendorName: String? = null,
    @SerializedName("EmployeeName") val EmployeeName: String? = null
)
data class ReceiptLineResponse(
    @SerializedName("InterfaceTransactionId") val InterfaceTransactionId: String? = null,
    @SerializedName(value = "ItemDescription", alternate = ["Description"]) val ItemDescription: String? = null
)
data class ProcessingErrorsResponse(@SerializedName("items") val items: List<ProcessingError> = emptyList())
data class ProcessingError(
    @SerializedName(value = "ItemDescription", alternate = ["Description"]) val ItemDescription: String? = null,
    @SerializedName(value = "ErrorMessage", alternate = ["ProcessingErrorMessage", "Message"]) val ErrorMessage: String? = null
)
data class AttachmentRequest(
    @SerializedName(value = "UploadedFileName", alternate = ["FileName"]) val UploadedFileName: String,
    @SerializedName("CategoryName") val CategoryName: String = "MISC",
    @SerializedName(value = "FileContents", alternate = ["Content"]) val FileContents: String,
    @SerializedName("Title") val Title: String? = null
)
data class AttachmentResponse(
    @SerializedName("AttachmentId") val AttachmentId: String? = null,
    @SerializedName(value = "UploadedFileName", alternate = ["FileName"]) val UploadedFileName: String? = null
)


// TO FLOW DTOS

// STEP 1 response wrapper
data class ExpectedShipmentLinesResponse(@SerializedName("items") val items: List<ExpectedShipmentLine> = emptyList())

/**
 * Raw shape from receivingReceiptExpectedShipmentLines (subset used).
 */
data class ExpectedShipmentLine(
    @SerializedName("FromOrganizationCode") val fromOrganizationCode: String? = null,
    @SerializedName("OrganizationCode") val organizationCode: String? = null,
    @SerializedName("ItemNumber") val itemNumber: String,
    @SerializedName("ItemDescription") val itemDescription: String? = null,
    @SerializedName("UnitOfMeasure") val unitOfMeasure: String? = null,
    @SerializedName("Subinventory") val subinventory: String? = null,
    @SerializedName("TransferOrderHeaderId") val transferOrderHeaderId: Long,
    @SerializedName("TransferOrderLineId") val transferOrderLineId: Long,
    @SerializedName("TransferOrderNumber") val transferOrderNumber: String? = null,
    @SerializedName("TransferOrderLineNumber") val transferOrderLineNumber: String? = null,
    @SerializedName("IntransitShipmentNumber") val intransitShipmentNumber: String? = null,
    @SerializedName("ShipmentLineNumber") val shipmentLineNumber: Int? = null,
    @SerializedName("DocumentLineId") val documentLineId: Long? = null,
    @SerializedName("AvailableQuantity") val availableQuantity: Double? = null,
    @SerializedName("OrderedQuantity") val orderedQuantity: Double? = null,
    @SerializedName("DestinationTypeCode") val destinationTypeCode: String? = null
)

/**
 * We keep using a single UI-friendly line type.
 * Filled from ExpectedShipmentLine.
 */
data class TransferOrderLine(
    @SerializedName("LineId") val transferOrderLineId: Long,
    @SerializedName("HeaderId") val transferOrderHeaderId: Long,
    @SerializedName("ItemDescription") val itemDescription: String? = null,
    @SerializedName("ItemNumber") val itemNumber: String,
    @SerializedName("Subinventory") val subinventory: String? = null,
    @SerializedName("QuantityUOMName") val unitOfMeasure: String? = null,
    @SerializedName("LineNumber") val lineNumber: Int? = null,              // parsed from TransferOrderLineNumber
    @SerializedName("RequestedQuantity") val quantity: Double? = null,      // OrderedQuantity
    @SerializedName("UnitPrice") val unitPrice: Double? = null,

    val destinationOrganizationCode: String? = null, // OrganizationCode (to)
    val sourceOrganizationCode: String? = null,      // FromOrganizationCode (from)
    val intransitShipmentNumber: String? = null,     // header & per-line DocumentNumber
    val shipmentLineNumber: Int? = null,             // DocumentLineNumber
    val documentLineId: Long? = null                 // stable unique id for UI
)

data class ToLinesResponse(@SerializedName("items") val items: List<TransferOrderLine> = emptyList())

// STEP 2: Shipment lines (lot / locator / shipped qty)
data class ShipmentLine(
    @SerializedName(value = "ShipmentNumber", alternate = ["Shipment"])
    val shipmentRaw: String? = null,
    @SerializedName("ShipmentId") val shipmentId: Long? = null,
    @SerializedName("ShipmentLine") val shipmentLineId: Long? = null,
    @SerializedName("Item") val item: String? = null,
    @SerializedName("LotNumber") val lotNumber: String? = null,
    @SerializedName("ShippedQuantity") val shippedQuantity: Double? = null,
    @SerializedName("SourceOrder") val sourceOrder: String? = null,
    @SerializedName("Order") val order: String? = null,
    @SerializedName("Locator") val locator: String? = null,
    @SerializedName("Subinventory") val subinventory: String? = null
) {
    val shipmentNumber: Long? get() = shipmentRaw?.toLongOrNull()
}
data class ShipmentLinesResponse(
    @SerializedName("items") val items: List<ShipmentLine>? = null,
    @SerializedName("ShipmentNumber") val shipmentNumberFallback: Long? = null,
    @SerializedName("LotNumber") val lotNumberFallback: String? = null
) {
    fun list(): List<ShipmentLine> =
        when {
            !items.isNullOrEmpty() -> items
            shipmentNumberFallback != null || lotNumberFallback != null ->
                listOf(ShipmentLine(shipmentRaw = shipmentNumberFallback?.toString(), lotNumber = lotNumberFallback))
            else -> emptyList()
        }
    fun firstOrNull(): ShipmentLine? = list().firstOrNull()
}

// STEP 3: Inventory item lots (expiry)
data class InventoryLotsResponse(@SerializedName("items") val items: List<InventoryLot> = emptyList())
data class InventoryLot(
    @SerializedName("LotNumber") val lotNumber: String? = null,
    @SerializedName(value = "LotExpirationDate", alternate = ["ExpirationDate"]) val lotExpirationDate: String? = null
)

// ===== Receipt (TO) payload =====
data class ReceiptRequestTo(
    @SerializedName("FromOrganizationCode") val fromOrganizationCode: String,
    @SerializedName("OrganizationCode") val organizationCode: String,
    @SerializedName("EmployeeId") val employeeId: Long,
    @SerializedName("ReceiptSourceCode") val receiptSourceCode: String = "TRANSFER ORDER",
    @SerializedName("ShipmentNumber") val shipmentNumber: String?,             // String now
    @SerializedName("lines") val lines: List<ReceiptLineTo>
)

data class ReceiptLineTo(
    @SerializedName("SourceDocumentCode") val sourceDocumentCode: String = "TRANSFER ORDER",
    @SerializedName("ReceiptSourceCode") val receiptSourceCode: String = "TRANSFER ORDER",
    @SerializedName("TransactionType") val transactionType: String = "RECEIVE",
    @SerializedName("AutoTransactCode") val autoTransactCode: String = "DELIVER",
    @SerializedName("DocumentNumber") val documentNumber: String?,             // String now (IntransitShipmentNumber)
    @SerializedName("DocumentLineNumber") val documentLineNumber: Int,         // ShipmentLineNumber from STEP-1
    @SerializedName("ItemNumber") val itemNumber: String,
    @SerializedName("OrganizationCode") val organizationCode: String,
    @SerializedName("Quantity") val quantity: Int,
    @SerializedName("UnitOfMeasure") val unitOfMeasure: String? = null,
    @SerializedName("Subinventory") val subinventory: String? = null,
    @SerializedName("TransferOrderHeaderId") val transferOrderHeaderId: Long,
    @SerializedName("TransferOrderLineId") val transferOrderLineId: Long,
    @SerializedName("Locator") val locator: String? = null,
    @SerializedName("lotItemLots") val lotItemLots: List<LotEntryTo>? = null
)

data class LotEntryTo(
    @SerializedName("LotNumber") val lotNumber: String,
    @SerializedName("TransactionQuantity") val transactionQuantity: Int,
    @SerializedName("LotExpirationDate") val lotExpirationDate: String? = null
)

// ===== Lifecycle + header searches =====
data class TransferOrderHeader(
    @SerializedName("HeaderId") val headerId: Long,
    @SerializedName("HeaderNumber") val headerNumber: String,
    @SerializedName("BusinessUnitName") val businessUnitName: String? = null,
    @SerializedName("Status") val status: String? = null,
    @SerializedName("InterfaceStatus") val interfaceStatus: String? = null
)
data class ToHeaderSearchResponse(@SerializedName("items") val items: List<TransferOrderHeader> = emptyList())

data class PoLifecycleReceiptsResponse(@SerializedName("items") val items: List<PoLifecycleReceipt> = emptyList())
data class PoLifecycleReceipt(
    @SerializedName("Receipt") val Receipt: String? = null,
    @SerializedName("ReceiptId") val ReceiptId: Long? = null,
    @SerializedName("ReceiptDate") val ReceiptDate: String? = null,
    @SerializedName("ReceivedBy") val ReceivedBy: String? = null
)
data class ReceiptRequestsSearchResponse(@SerializedName("items") val items: List<ReceiptResponse> = emptyList())

// Helper used by older code paths / tests
data class ToReceiveLineInput(
    val line: TransferOrderLine,
    val quantity: Int,
    val lotNumber: String? = null,
    val lotExpirationDate: String? = null
)
