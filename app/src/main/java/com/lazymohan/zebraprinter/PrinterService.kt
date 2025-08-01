package com.lazymohan.zebraprinter

import com.lazymohan.zebraprinter.model.DiscoveredPrinterInfo
import com.lazymohan.zebraprinter.model.PrintContentModel

interface PrinterService {
    fun connectAndPrint(macAddress: String, itemData: PrintContentModel, noOfCopies: Int)
    fun disconnect()
    fun getBondedZebraPrinters(): List<DiscoveredPrinterInfo>
}