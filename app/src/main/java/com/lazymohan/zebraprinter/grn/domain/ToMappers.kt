package com.lazymohan.zebraprinter.grn.domain

import com.lazymohan.zebraprinter.grn.data.LotEntryTo
import com.lazymohan.zebraprinter.grn.data.ReceiptLineTo
import com.lazymohan.zebraprinter.grn.data.ReceiptRequestTo
import com.lazymohan.zebraprinter.grn.data.TransferOrderLine

/**
 * Helpers to build the TO (Transfer Order) receipt payload,
 * using the *TO-specific* DTOs (ReceiptRequestTo, ReceiptLineTo).
 *
 * IMPORTANT: Do not use the PO DTOs here (ReceiptRequest / ReceiptLine).
 */

// Single line mapper (TO)
fun toReceiptLineTo(
    line: TransferOrderLine,
    headerId: Long,
    documentLineNumber: Int,
    toOrganizationCode: String,
    quantity: Int,
    shipmentNumber: Long?,         // can be null when not available
    lotNumber: String?             // optional
): ReceiptLineTo {
    val lots = lotNumber
        ?.takeIf { it.isNotBlank() }
        ?.let { listOf(LotEntryTo(lotNumber = it, transactionQuantity = quantity)) }

    return ReceiptLineTo(
        sourceDocumentCode = "TRANSFER ORDER",
        receiptSourceCode = "TRANSFER ORDER",
        transactionType = "RECEIVE",
        autoTransactCode = "DELIVER",
        documentNumber = shipmentNumber,                // ShipmentNumber
        documentLineNumber = documentLineNumber,       // usually 1..N
        itemNumber = line.itemNumber,
        organizationCode = toOrganizationCode,
        quantity = quantity,
        unitOfMeasure = line.unitOfMeasure ?: "EA",
        subinventory = line.subinventory,
        transferOrderHeaderId = headerId,
        transferOrderLineId = line.transferOrderLineId,
        lotItemLots = lots
    )
}

// Full request mapper (TO)
fun buildReceiptRequestTo(
    fromOrganizationCode: String,
    toOrganizationCode: String,
    employeeId: Long,
    shipmentNumber: Long?,
    lines: List<ReceiptLineTo>
): ReceiptRequestTo {
    return ReceiptRequestTo(
        fromOrganizationCode = fromOrganizationCode,
        organizationCode = toOrganizationCode,
        employeeId = employeeId,
        receiptSourceCode = "TRANSFER ORDER",
        shipmentNumber = shipmentNumber,
        lines = lines
    )
}
