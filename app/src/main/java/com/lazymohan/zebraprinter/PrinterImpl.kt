package com.lazymohan.zebraprinter

import android.Manifest.permission
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.lazymohan.zebraprinter.exception.PrinterConnectionException
import com.lazymohan.zebraprinter.exception.PrinterHardwareException
import com.lazymohan.zebraprinter.exception.PrinterLanguageException
import com.lazymohan.zebraprinter.exception.PrinterUnknownException
import com.lazymohan.zebraprinter.model.DiscoveredPrinterInfo
import com.lazymohan.zebraprinter.model.PrintContentModel
import com.zebra.sdk.btleComm.BluetoothLeConnection
import com.zebra.sdk.comm.Connection
import com.zebra.sdk.comm.ConnectionException
import com.zebra.sdk.printer.PrinterStatus
import com.zebra.sdk.printer.ZebraPrinter
import com.zebra.sdk.printer.ZebraPrinterFactory
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException
import com.zebra.sdk.printer.ZebraPrinterLinkOs

val ZEBRA_MAC_PREFIXES = listOf(
    "00:15:70",
    "AC:3F:A4",
    "28:6A:BA",
    "44:44:44",
    "40:4C:CA",
    "0C:EC:80"
)

class PrinterImpl(
    private val context: Context,
) : PrinterService {

    private var connection: Connection? = null
    private var zebraPrinter: ZebraPrinter? = null


    override fun connectAndPrint(macAddress: String, itemData: PrintContentModel, noOfCopies: Int) {
        var connection: Connection? = null
        try {
            // Open BLE connection
            connection = BluetoothLeConnection(macAddress, context).apply { open() }

            if (connection.isConnected) {
                // Build and send label ZPL
                val zpl = createLabelZPL(itemData, noOfCopies)
                connection.write(zpl)
            } else {
                throw PrinterConnectionException(R.string.select_desired_device)
            }
        } catch (e: Exception) {
            Firebase.crashlytics.recordException(e)
            throw when (e) {
                is ConnectionException -> PrinterConnectionException(R.string.select_desired_device)
                is ZebraPrinterLanguageUnknownException -> PrinterLanguageException()
                else -> PrinterUnknownException()
            }

        } finally {
            try {
                connection?.close()
            } catch (e: Exception) {
                Firebase.crashlytics.recordException(e)
            }
        }
    }

    @RequiresPermission(permission.BLUETOOTH_CONNECT)
    override fun getBondedZebraPrinters(): List<DiscoveredPrinterInfo> {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        if (!ensureBluetoothPermission(context)) return emptyList()
        return adapter?.bondedDevices?.map {
            DiscoveredPrinterInfo(
                macAddress = it.address,
                model = it.name
            )
        }.orEmpty()
    }

    override fun disconnect() {
        try {
            connection?.close()
        } catch (e: ConnectionException) {
            Firebase.crashlytics.recordException(e)
        }
    }

    private fun printLabel(itemData: PrintContentModel, noOfCopies: Int) {
        try {
            val linkOsPrinter: ZebraPrinterLinkOs =
                ZebraPrinterFactory.createLinkOsPrinter(zebraPrinter)
            val printerStatus: PrinterStatus = linkOsPrinter.currentStatus

            when {
                printerStatus.isReadyToPrint -> {
                    connection?.write(createLabelZPL(contentModel = itemData, copies = noOfCopies))
                }

                printerStatus.isHeadOpen -> {
                    throw PrinterHardwareException(R.string.printer_head_open)
                }

                printerStatus.isPaused -> {
                    throw PrinterHardwareException(R.string.printer_paused)
                }

                printerStatus.isPaperOut -> {
                    throw PrinterHardwareException(R.string.label_empty)
                }

                else -> {
                    throw PrinterHardwareException(R.string.unknown_error)
                }
            }
        } catch (e: ConnectionException) {
            Firebase.crashlytics.recordException(e)
            throw PrinterHardwareException(R.string.unknown_error)
        }
    }

    private fun createLabelZPL(contentModel: PrintContentModel, copies: Int): ByteArray {
        // Ensure GTIN is 14 digits
        val gtin14 = contentModel.gtinNum.padStart(14, '0')

        // Expiry date must be YYMMDD
        val expiry = contentModel.expiryDate.replace("-", "")

        // Build GS1 string (without GS for now)
        val gs1Data = "((01)$gtin14" + "(17)$expiry" + "(10)${contentModel.batchNo}"

        val zpl = """
        ^XA
        ^CF0,20
        ^PW400
        ^LL300
        ^FO20,50^FB350,1,0,L^FDItem Number: ${contentModel.itemNum}^FS
        ^FO20,75^FB350,1,0,L^FDDescription: ${contentModel.description}^FS
        ^FO20,100^FB350,1,0,L^FDLot/Batch No:${contentModel.batchNo}^FS
        ^FO20,125^FB350,1,0,L^FDExpiry Date: ${contentModel.labelExpiryDate}^FS
        ^FO120,160
        ^BY3,2
        ^BQN,2,6
        ^FDMA$gs1Data^FS
        ^PQ$copies
        ^XZ
        """.trimIndent()
        return zpl.toByteArray(Charsets.UTF_8)
    }

    private fun ensureBluetoothPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not needed before Android 12
        }

    @RequiresPermission(permission.BLUETOOTH_CONNECT)
    fun isZebraDevice(device: BluetoothDevice): Boolean {
        val mac = device.address.uppercase()
        val type = device.type
        return ZEBRA_MAC_PREFIXES.any { mac.startsWith(it) }
    }
}