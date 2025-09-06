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

/** Build payload like your sample: 1 line per TO line, with multiple lotItemLots */
fun buildToReceiptRequest(
    cfg: ToConfig,
    header: TransferOrderHeader,
    shipment: ShipmentLine?,
    inputs: List<ToReceiveLineInput>
): ToReceiptRequestPayload {

    // group all sections (inputs) by the TO line
    val grouped = inputs.groupBy { it.line.transferOrderLineId }

    val lines: List<ToReceiptLinePayload> = grouped.values.mapIndexed { idx, group ->
        val first = group.first()
        val line = first.line

        val lotEntries = group.mapNotNull { s ->
            s.lotNumber?.let {
                ToLotEntryPayload(
                    lotNumber = it,
                    transactionQuantity = s.quantity,
                    lotExpirationDate = s.lotExpirationDate
                )
            }
        }

        ToReceiptLinePayload(
            documentNumber = shipment?.shipmentNumber,
            documentLineNumber = line.lineNumber ?: (idx + 1),
            itemNumber = line.itemNumber,
            organizationCode = cfg.organizationCode,
            quantity = group.sumOf { it.quantity },
            unitOfMeasure = line.unitOfMeasure,
            subinventory = line.subinventory,
            transferOrderHeaderId = header.headerId,
            transferOrderLineId = line.transferOrderLineId,
            lotItemLots = lotEntries.ifEmpty { null }
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
