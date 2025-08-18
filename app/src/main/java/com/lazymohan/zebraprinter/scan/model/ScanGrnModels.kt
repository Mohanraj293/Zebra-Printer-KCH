package com.lazymohan.zebraprinter.scan.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class ScanGrnStep { SCAN, RESULTS, ENTER_PO, SHOW_PO, REVIEW, SUMMARY }

/** Header parsed from delivery slip OCR */
@Parcelize
data class ExtractedHeader(
    val supplierName: String? = null,
    val invoiceNo: String? = null,
    val invoiceDate: String? = null,
    val poGuess: String? = null
) : Parcelable

/** Line parsed from delivery slip OCR (editable in Results) */
@Parcelize
data class ExtractedItem(
    val description: String,
    val uom: String? = null,
    val itemNumber: String? = null,
    val orderedQtyHint: Double? = null,
    var qtyDelivered: Double? = null,
    var batchNo: String? = null,
    var expiryDate: String? = null
) : Parcelable

/* ---- PO / Receipt shapes (mirror your manual flow) ---- */

data class PoHeader(
    val OrderNumber: String,
    val Supplier: String,
    val SupplierSite: String,
    val ProcurementBU: String,
    val SoldToLegalEntity: String
)

data class PoLine(
    val LineNumber: Int,
    val Item: String?,
    val Description: String?,
    val Quantity: Double,
    val UOM: String
)

data class LineInput(
    val lineNumber: Int,
    val qty: Double,
    val lot: String,
    val expiry: String
)

data class LotInfo(val LotNumber: String, val LotExpirationDate: String)

data class ReceiptLine(
    val DocumentLineNumber: Int,
    val ItemNumber: String,
    val Quantity: Double,
    val UnitOfMeasure: String,
    val lotItemLots: List<LotInfo>
)

data class ReceiptPayload(
    val VendorName: String,
    val VendorSiteCode: String,
    val BusinessUnit: String,
    val lines: List<ReceiptLine>
)

data class ReceiptResponse(val ReceiptNumber: String?, val ReturnStatus: String?)
data class LineError(val ItemDescription: String?, val ErrorMessage: String?)

/** Screen state for the scan-based GRN flow */
data class ScanGrnUiState(
    val step: ScanGrnStep = ScanGrnStep.RESULTS,
    val loading: Boolean = false,
    val error: String? = null,

    // RESULTS
    val extractedHeader: ExtractedHeader? = null,
    val extractedItems: List<ExtractedItem> = emptyList(),

    // ENTER PO
    val poNumber: String = "",

    // RECEIVE
    val po: PoHeader? = null,
    val lines: List<PoLine> = emptyList(),
    val lineInputs: List<LineInput> = emptyList(),

    // REVIEW / SUMMARY
    val payload: ReceiptPayload? = null,
    val receipt: ReceiptResponse? = null,
    val lineErrors: List<LineError> = emptyList()
)
