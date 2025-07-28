package com.lazymohan.zebraprinter

import android.Manifest.permission
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.lazymohan.zebraprinter.exception.PrinterConnectionException
import com.lazymohan.zebraprinter.exception.PrinterHardwareException
import com.lazymohan.zebraprinter.exception.PrinterLanguageException
import com.lazymohan.zebraprinter.exception.PrinterUnknownException
import com.lazymohan.zebraprinter.model.DiscoveredPrinterInfo
import com.lazymohan.zebraprinter.model.PrintContentModel
import com.zebra.sdk.comm.BluetoothConnection
import com.zebra.sdk.comm.Connection
import com.zebra.sdk.comm.ConnectionException
import com.zebra.sdk.printer.PrinterStatus
import com.zebra.sdk.printer.SGD
import com.zebra.sdk.printer.ZebraPrinter
import com.zebra.sdk.printer.ZebraPrinterFactory
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException
import com.zebra.sdk.printer.ZebraPrinterLinkOs
import com.zebra.sdk.printer.discovery.BluetoothDiscoverer
import com.zebra.sdk.printer.discovery.DiscoveredPrinter
import com.zebra.sdk.printer.discovery.DiscoveryHandler
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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

    @SuppressLint("MissingPermission")
    override fun discoverPrinters(): Flow<DiscoveredPrinterInfo> = callbackFlow {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter

        fun getBondedZebraPrinters(): List<DiscoveredPrinterInfo> {
            if (!ensureBluetoothPermission(context)) return emptyList()
            return adapter?.bondedDevices
                ?.filter { isZebraDevice(it) }
                ?.map {
                    DiscoveredPrinterInfo(
                        macAddress = it.address,
                        model = it.name
                    )
                }.orEmpty()
        }

        val bondedPrinters = getBondedZebraPrinters()

        if (bondedPrinters.isNotEmpty()) {
            bondedPrinters.forEach { trySend(it) }
            channel.close()
        } else {
            BluetoothDiscoverer.findPrinters(
                context,
                object : DiscoveryHandler {
                    override fun foundPrinter(printer: DiscoveredPrinter) {
                        trySend(
                            DiscoveredPrinterInfo(
                                macAddress = printer.address,
                                model = printer.discoveryDataMap["FRIENDLY_NAME"]
                                    ?: printer.discoveryDataMap["SYSTEM_MODEL"]
                                    ?: "Unknown"
                            )
                        )
                    }

                    override fun discoveryFinished() {
                        channel.close()
                    }

                    override fun discoveryError(message: String) {
                        channel.close(ConnectionException(message))
                    }
                }
            )
        }
        awaitClose()
    }

    override fun connectAndPrint(macAddress: String, itemData: PrintContentModel, noOfCopies: Int) {
        try {
            connection = BluetoothConnection(macAddress).apply { open() }
            if (connection?.isConnected == true) {
                zebraPrinter = ZebraPrinterFactory.getInstance(connection)
                printLabel(itemData = itemData, noOfCopies = noOfCopies)
            }
        } catch (e: Exception) {
            throw when (e) {
                is ConnectionException -> PrinterConnectionException(R.string.select_desired_device)
                is ZebraPrinterLanguageUnknownException -> PrinterLanguageException()
                else -> PrinterUnknownException()
            }
        } finally {
            disconnect()
        }
    }

    override fun disconnect() {
        try {
            connection?.close()
        } catch (e: ConnectionException) {
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
            throw PrinterUnknownException()
        }
    }

    private fun createLabelZPL(contentModel: PrintContentModel, copies: Int): ByteArray {
        SGD.SET("device.language", "zpl,", connection)
        return """
            ^XA
            ^FX^CF0,20^PW400^LL300
            ^FS^FO0,50^FB400,1,0,C^FD#${contentModel.itemNum}
            ^FS^FO0,90^FB400,5,,,C^FD${contentModel.description}
            ^FS^FO0,160^FB400,1,0,C^GB420,1,1
            ^FS^FO140,175^BY2,2^BQN,2,5^FD000${contentModel.itemNum}
            ^FS^PQ$copies^XZ
        """.trimIndent().toByteArray()
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

    fun isZebraDevice(device: BluetoothDevice): Boolean {
        val mac = device.address.uppercase()
        return ZEBRA_MAC_PREFIXES.any { mac.startsWith(it) }
    }
}