// app/src/main/java/com/lazymohan/zebraprinter/grn/domain/ToDomain.kt
package com.lazymohan.zebraprinter.grn.domain

import com.google.gson.annotations.SerializedName
import com.lazymohan.zebraprinter.grn.data.ShipmentLine
import com.lazymohan.zebraprinter.grn.data.TransferOrderHeader
import com.lazymohan.zebraprinter.grn.data.TransferOrderLine

data class ToConfig(
    val fromOrganizationCode: String,
    val organizationCode: String,
    val employeeId: Long
)

data class ToReceiveLineInput(
    val line: TransferOrderLine,
    val quantity: Int,
    val lotNumber: String? = null,
    val lotExpirationDate: String? = null
)

data class ToReceiptRequestPayload(
    @SerializedName("FromOrganizationCode") val fromOrganizationCode: String,
    @SerializedName("OrganizationCode") val organizationCode: String,
    @SerializedName("EmployeeId") val employeeId: Long,
    @SerializedName("ReceiptSourceCode") val receiptSourceCode: String = "TRANSFER ORDER",
    @SerializedName("ShipmentNumber") val shipmentNumber: Long?,
    @SerializedName("lines") val lines: List<ToReceiptLinePayload>
)

data class ToReceiptLinePayload(
    @SerializedName("SourceDocumentCode") val sourceDocumentCode: String = "TRANSFER ORDER",
    @SerializedName("ReceiptSourceCode") val receiptSourceCode: String = "TRANSFER ORDER",
    @SerializedName("TransactionType") val transactionType: String = "RECEIVE",
    @SerializedName("AutoTransactCode") val autoTransactCode: String = "DELIVER",
    @SerializedName("DocumentNumber") val documentNumber: Long?,      // ShipmentNumber
    @SerializedName("DocumentLineNumber") val documentLineNumber: Int,
    @SerializedName("ItemNumber") val itemNumber: String,
    @SerializedName("OrganizationCode") val organizationCode: String,
    @SerializedName("Quantity") val quantity: Int,
    @SerializedName("UnitOfMeasure") val unitOfMeasure: String? = null,
    @SerializedName("Subinventory") val subinventory: String? = null,
    @SerializedName("TransferOrderHeaderId") val transferOrderHeaderId: Long,
    @SerializedName("TransferOrderLineId") val transferOrderLineId: Long,
    @SerializedName("lotItemLots") val lotItemLots: List<ToLotEntryPayload>? = null
)

data class ToLotEntryPayload(
    @SerializedName("LotNumber") val lotNumber: String,
    @SerializedName("TransactionQuantity") val transactionQuantity: Int,
    @SerializedName("LotExpirationDate") val lotExpirationDate: String? = null
)

fun buildToReceiptRequest(
    cfg: ToConfig,
    header: TransferOrderHeader,
    shipment: ShipmentLine?,
    inputs: List<ToReceiveLineInput>
): ToReceiptRequestPayload {
    val lines = inputs.mapIndexed { idx, inp ->
        ToReceiptLinePayload(
            documentNumber = shipment?.shipmentNumber,
            documentLineNumber = (inp.line.lineNumber ?: (idx + 1)),
            itemNumber = inp.line.itemNumber,
            organizationCode = cfg.organizationCode,
            quantity = inp.quantity,
            unitOfMeasure = inp.line.unitOfMeasure,
            subinventory = inp.line.subinventory,
            transferOrderHeaderId = header.headerId,
            transferOrderLineId = inp.line.transferOrderLineId,
            lotItemLots = inp.lotNumber?.let {
                listOf(
                    ToLotEntryPayload(
                        lotNumber = it,
                        transactionQuantity = inp.quantity,
                        lotExpirationDate = inp.lotExpirationDate
                    )
                )
            }
        )
    }

    return ToReceiptRequestPayload(
        fromOrganizationCode = cfg.fromOrganizationCode,
        organizationCode = cfg.organizationCode,
        employeeId = cfg.employeeId,
        shipmentNumber = shipment?.shipmentNumber,
        lines = lines
    )
}
