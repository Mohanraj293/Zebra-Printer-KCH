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

/**
 * New gatekeeping:
 * 1) Fetch header. If header not "Open", return header only.
 * 2) Fetch EXPECTED shipment lines for org KDH and the given TO number.
 * 3) Do not over-filter: UI will allow user to pick sections & quantities.
 */
class FetchToBundleUseCase @Inject constructor(
    private val repo: GrnRepository
) {
    suspend operator fun invoke(toNumber: String): Result<ToBundle> = runCatching {
        val header = repo.findToHeaderByNumber(toNumber).getOrThrow()
            ?: error("Transfer Order not found for number=$toNumber")

        val isOpen = header.status?.equals("open", ignoreCase = true) == true
        if (!isOpen) return@runCatching ToBundle(header = header, lines = emptyList(), shipment = null)

        val expectedLines = repo.fetchExpectedShipmentLines(toNumber, orgCode = "KDH").getOrThrow()

        ToBundle(header = header, lines = expectedLines, shipment = null)
    }
}
