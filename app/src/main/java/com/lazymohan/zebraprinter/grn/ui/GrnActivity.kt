package com.lazymohan.zebraprinter.grn.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.lazymohan.zebraprinter.utils.DateTimeConverter
import com.tarkalabs.tarkaui.theme.TUITheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class GrnActivity : ComponentActivity() {

    @Inject lateinit var dateTimeConverter: DateTimeConverter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialPo =
            intent.getStringExtra("po_number") // if it is from isFromPickUpSlip, this is the orderNumber
        val scanJson = intent.getStringExtra("scan_extract_json")
        val scanImageCachePaths: ArrayList<String>? =
            intent.getStringArrayListExtra("scan_image_cache_paths")
        val isFromPickSlip = intent.getBooleanExtra("isFromPickSlip", false)

        Log.d(
            "GrnActivity",
            "onCreate: po=$initialPo, json=${scanJson?.length ?: 0} chars, cachePaths=${scanImageCachePaths?.size ?: 0}"
        )

        setContent {
            TUITheme {
                val vm: GrnViewModel = hiltViewModel()
                val toVm: TransferGrnViewModel = hiltViewModel()
                val ui by vm.state.collectAsState()
                val snack = remember { SnackbarHostState() }
                val context = LocalContext.current

                LaunchedEffect(initialPo, scanJson, scanImageCachePaths) {
                    Log.d(
                        "GrnActivity",
                        "prefillFromScan(po=$initialPo, jsonLen=${scanJson?.length ?: 0}, paths=${scanImageCachePaths?.size ?: 0})"
                    )
                    if (isFromPickSlip) {
                        toVm.prefillFromScan(
                            toNumber = initialPo.orEmpty(),
                            scanJson = scanJson,
                            cachePaths = scanImageCachePaths ?: arrayListOf(),
                        )
                    } else {
                        vm.prefillFromScan(
                            po = initialPo.orEmpty(),
                            scanJson = scanJson,
                            cachePaths = scanImageCachePaths ?: arrayListOf(),
                        )
                    }
                }

                GrnScreens(
                    ui = ui,
                    snackbarHostState = snack,
                    dateTimeConverter = dateTimeConverter,

                    // choose which VM to call
                    onEnterPo = {
                        if (isFromPickSlip) toVm.setToNumber(it)
                        else vm.setPoNumber(it)
                    },
                    onFetchPo = {
                        if (isFromPickSlip) toVm.fetchTransferOrder()
                        else vm.fetchPo()
                    },
                    onUpdateLine = { ln, q, l, e ->
                        if (isFromPickSlip) toVm.updateLineSection(
                            lineNumber = ln,
                            expiry = e,
                            qty = q,
                            lot = l
                        )
                        else vm.updateLine(ln, q, l, e)
                    },
                    onRemoveLine = { ln ->
                        if (isFromPickSlip) toVm.removeLine(ln)
                        else vm.removeLine(ln)
                    },
                    onAddLine = {
                        if (isFromPickSlip) toVm.addLineFromTo(it)
                        else vm.addLineFromPo(it)
                    },
                    onAddSection = {
                        if (isFromPickSlip) toVm.addSection(it)
                        else vm.addSection(it)
                    },
                    onRemoveSection = { ln, sec ->
                        if (isFromPickSlip) toVm.removeSection(ln, sec)
                        else vm.removeSection(ln, sec)
                    },
                    onUpdateSection = { ln, sec, q, l, e ->
                        if (isFromPickSlip) {
                            toVm.updateLineSection(
                                lineNumber = ln,
                                sectionIndex = sec,
                                qty = q,
                                lot = l,
                                expiry = e
                            )
                        } else {
                            vm.updateLineSection(
                                lineNumber = ln,
                                sectionIndex = sec,
                                qty = q,
                                lot = l,
                                expiry = e
                            )
                        }
                    },
                    onEditReceive = {
                        if (isFromPickSlip) toVm.backToReceive()
                        else vm.backToReceive()
                    },
                    onReview = {
                        if (isFromPickSlip) toVm.buildTOPayloadsAndReview()
                        else vm.buildPayloadsAndReview()
                    },
                    onSubmit = {
                        if (isFromPickSlip) toVm.submitTransferReceipt()
                        else vm.submitReceipt()
                    },
                    onStartOver = {
                        if (isFromPickSlip) toVm.startOver()
                        else vm.startOver()
                    },
                    onBack = { finish() },
                    isFromPickSlip = isFromPickSlip,
                    toUiState = toVm.state.collectAsState().value,
                    onAddAttachments = { uris -> vm.addManualAttachmentsFromUris(context, uris) }
                )
            }
        }
    }
}
