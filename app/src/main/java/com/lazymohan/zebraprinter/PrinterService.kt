package com.lazymohan.zebraprinter

import com.lazymohan.zebraprinter.model.DiscoveredPrinterInfo
import com.lazymohan.zebraprinter.model.PrintContentModel
import kotlinx.coroutines.flow.Flow

interface PrinterService {
    fun discoverPrinters(): Flow<DiscoveredPrinterInfo>
    fun connectAndPrint(macAddress: String, itemData: PrintContentModel, noOfCopies: Int = 1)
    fun disconnect()
}