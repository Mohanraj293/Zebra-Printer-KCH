package com.lazymohan.zebraprinter.scan.data

import com.lazymohan.zebraprinter.scan.model.*
import kotlinx.coroutines.delay
import javax.inject.Inject

interface ScanGrnRepository {
    suspend fun fetchPo(poNumber: String): Result<Pair<PoHeader, List<PoLine>>>
    suspend fun submitReceipt(payload: ReceiptPayload): Result<Pair<ReceiptResponse, List<LineError>>>
}

/** Fake impl so UI runs E2E; swap with real Fusion-backed repo later */
class FakeScanGrnRepository @Inject constructor() : ScanGrnRepository {
    override suspend fun fetchPo(poNumber: String): Result<Pair<PoHeader, List<PoLine>>> {
        delay(600)
        val header = PoHeader(
            OrderNumber = poNumber,
            Supplier = "MediSupply LLC",
            SupplierSite = "Dubai Main",
            ProcurementBU = "KCH-DXB",
            SoldToLegalEntity = "King's College Hospital Dubai"
        )
        val lines = listOf(
            PoLine(1, "URP-50AMP", "TACHYBEN (URAPIDIL) 50MG/10ML AMP 5'S", 10.0, "BOX"),
            PoLine(2, "NUT-TRACE10", "NUTRYELT 10ML AMP (TRACE EL.) ADULT-10'S", 20.0, "BOX")
        )
        return Result.success(header to lines)
    }

    override suspend fun submitReceipt(payload: ReceiptPayload): Result<Pair<ReceiptResponse, List<LineError>>> {
        delay(800)
        return if (payload.lines.isNotEmpty())
            Result.success(ReceiptResponse("RCPT-102938", "SUCCESS") to emptyList())
        else
            Result.success(ReceiptResponse(null, "NO_LINES") to emptyList())
    }
}
