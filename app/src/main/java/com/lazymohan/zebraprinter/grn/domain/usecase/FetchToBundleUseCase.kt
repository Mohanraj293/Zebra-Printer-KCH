package com.lazymohan.zebraprinter.grn.domain.usecase

import com.lazymohan.zebraprinter.grn.data.GrnRepository
import com.lazymohan.zebraprinter.grn.data.ShipmentLine
import com.lazymohan.zebraprinter.grn.data.TransferOrderHeader
import com.lazymohan.zebraprinter.grn.data.TransferOrderLine
import javax.inject.Inject

data class ToBundle(
    val header: TransferOrderHeader,
    val lines: List<TransferOrderLine>,
    val shipment: ShipmentLine?
)

class FetchToBundleUseCase @Inject constructor(
    private val repo: GrnRepository
) {
    suspend operator fun invoke(toNumber: String): Result<ToBundle> = runCatching {
        val header = repo.findToHeaderByNumber(toNumber).getOrThrow()
            ?: error("Transfer Order not found for number=$toNumber")

        val lines = repo.getToLines(header.headerId).getOrThrow()
        val shipment = repo.getShipmentForToNumber(toNumber).getOrNull()

        ToBundle(header = header, lines = lines, shipment = shipment)
    }
}
