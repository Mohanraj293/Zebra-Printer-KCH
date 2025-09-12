// grn/ui/GrnViewModel.kt
package com.lazymohan.zebraprinter.grn.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.lazymohan.zebraprinter.BuildConfig
import com.lazymohan.zebraprinter.app.AppPref
import com.lazymohan.zebraprinter.grn.data.AttachmentRequest
import com.lazymohan.zebraprinter.grn.data.GrnRepository
import com.lazymohan.zebraprinter.grn.data.LotItem
import com.lazymohan.zebraprinter.grn.data.PoItem
import com.lazymohan.zebraprinter.grn.data.PoLineItem
import com.lazymohan.zebraprinter.grn.data.ProcessingError
import com.lazymohan.zebraprinter.grn.data.ReceiptLine
import com.lazymohan.zebraprinter.grn.data.ReceiptRequest
import com.lazymohan.zebraprinter.grn.data.ReceiptRequestDff
import com.lazymohan.zebraprinter.grn.data.ReceiptResponse
import com.lazymohan.zebraprinter.grn.util.ExtractedItem
import com.lazymohan.zebraprinter.grn.util.bestMatchIndex
import com.lazymohan.zebraprinter.grn.util.parseToIso
import com.lazymohan.zebraprinter.scan.ScanExtractTransfer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.File
import javax.inject.Inject

enum class GrnStep { ENTER_PO, SHOW_PO, REVIEW, SUMMARY }

data class LineSectionInput(
    val section: Int,
    val qty: Int = 0,
    val lot: String = "",
    val expiry: String = ""
)

data class LineInput(
    val lineNumber: Int,
    val itemNumber: String,
    val uom: String,
    val maxQty: Double,
    val description: String = "",
    val gtin: String? = null,
    val sections: List<LineSectionInput> = listOf(LineSectionInput(section = 1))
)

data class StagedReceipt(val sectionIndex: Int, val request: ReceiptRequest)

enum class PartStatus { PENDING, SUBMITTING, SUCCESS, FAILED }
data class PartProgress(
    val sectionIndex: Int,
    val lines: Int,
    val status: PartStatus = PartStatus.PENDING,
    val message: String? = null,
    val receiptHeaderId: Long? = null,
    val returnStatus: String? = null,
    val errors: List<String> = emptyList(),
    val httpCode: Int? = null,
    val url: String? = null,
    val errorBody: String? = null,
    val exception: String? = null
)

data class AttachmentCache(
    val path: String,
    val displayName: String,
    val mimeType: String? = null
)

data class GrnUiState(
    val step: GrnStep = GrnStep.ENTER_PO,
    val loading: Boolean = false,
    val error: String? = null,
    val poNumber: String = "",
    val po: PoItem? = null,

    // visible lines in UI (matched-only in scan mode)
    val lines: List<PoLineItem> = emptyList(),
    val lineInputs: List<LineInput> = emptyList(),

    // keep the full PO lines for add-line dialog
    val allPoLines: List<PoLineItem> = emptyList(),
    // whether lines fetch is completed (to distinguish from “loading”)
    val linesLoaded: Boolean = false,
    val invoiceNumber: String = "",

    val staged: List<StagedReceipt> = emptyList(),
    val progress: List<PartProgress> = emptyList(),

    val receipt: ReceiptResponse? = null,
    val lineErrors: List<ProcessingError> = emptyList(),

    // scan payload (unchanged)
    val extractedFromScan: List<ExtractedItem> = emptyList(),

    // cache file paths for scanned images (from ScanResultScreen)
    val scanImageCachePaths: List<String> = emptyList(),

    val extraAttachments: List<AttachmentCache> = emptyList(),

    // count of successfully uploaded attachments (informational)
    val attachmentsUploaded: Int = 0
)

@HiltViewModel
class GrnViewModel @Inject constructor(
    private val repo: GrnRepository,
    private val appPref: AppPref
) : ViewModel() {

    private val _state = MutableStateFlow(GrnUiState())
    val state: StateFlow<GrnUiState> = _state

    // NEW: allow UI to change invoice number
    fun updateInvoiceNumber(text: String) {
        _state.value = _state.value.copy(invoiceNumber = text)
    }

    fun setPoNumber(po: String) {
        _state.value = _state.value.copy(poNumber = po)
    }

    fun fetchPo() = viewModelScope.launch {
        val s = _state.value
        if (s.poNumber.isBlank()) return@launch

        val poNumber = s.poNumber.trim().uppercase()

        _state.value = s.copy(loading = true, error = null, poNumber = poNumber)
        repo.fetchPo(poNumber)
            .onSuccess { po ->
                Log.d("GRN", "PO fetched: ${po.OrderNumber}, header=${po.POHeaderId}")
                _state.value = _state.value.copy(loading = false, po = po, step = GrnStep.SHOW_PO)
                prefetchPoLines(po.POHeaderId)
            }
            .onFailure { e ->
                Log.e("GRN", "fetchPo failed: ${e.message}")
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Failed to load PO")
            }
    }


    private fun PoLineItem.toLineInput(initial: LineSectionInput? = null) = LineInput(
        lineNumber = LineNumber,
        itemNumber = Item.trim().orEmpty(),
        uom = UOM,
        maxQty = Quantity,
        description = Description ?: "",
        gtin = GTIN,
        sections = listOf(initial ?: LineSectionInput(section = 1))
    )

    private fun prefetchPoLines(headerId: String) = viewModelScope.launch {
        Log.d("GRN", "Prefetch PO lines for headerId=$headerId")
        repo.fetchPoLines(headerId)
            .onSuccess { items ->
                val extracted = _state.value.extractedFromScan
                Log.d("GRN", "Fetched ${items.size} PO lines. Scan items=${extracted.size}")

                if (extracted.isNotEmpty()) {
                    // Scan-driven; build section-1 from OCR values
                    val used = mutableSetOf<Int>()
                    val matchedPairs = items.mapNotNull { line ->
                        val safeItem = line.Item.trim()
                        val poDesc = (line.Description ?: safeItem).trim()
                        val matchIdx = bestMatchIndex(extracted, poDesc, used, 0.01)
                        if (matchIdx != null) {
                            used += matchIdx
                            val ex = extracted[matchIdx]
                            val isoExpiry = parseToIso(ex.expiryDate)
                            val sec1 = LineSectionInput(
                                section = 1,
                                qty = ex.qtyDelivered.toInt(),
                                lot = ex.batchNo,
                                expiry = isoExpiry
                            )
                            line to line.toLineInput(initial = sec1)
                        } else null
                    }

                    _state.value = _state.value.copy(
                        allPoLines = items,
                        lines = matchedPairs.map { it.first },
                        lineInputs = matchedPairs.map { it.second },
                        linesLoaded = true,
                        step = GrnStep.SHOW_PO
                    )
                } else {
                    // Manual/default: all lines with empty section-1
                    val inputs = items.map { it.toLineInput() }
                    _state.value = _state.value.copy(
                        allPoLines = items,
                        lines = items,
                        lineInputs = inputs,
                        linesLoaded = true,
                        step = GrnStep.SHOW_PO
                    )
                }
            }
            .onFailure { e ->
                Log.e("GRN", "fetchPoLines failed: ${e.message}")
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to load PO lines",
                    linesLoaded = true
                )
            }
    }

    // --- Section helpers ---
    fun addSection(lineNumber: Int) {
        val s = _state.value
        val updated = s.lineInputs.map { li ->
            if (li.lineNumber == lineNumber) {
                val nextIdx = (li.sections.maxOfOrNull { it.section } ?: 0) + 1
                li.copy(sections = li.sections + LineSectionInput(section = nextIdx))
            } else li
        }
        _state.value = s.copy(lineInputs = updated)
    }

    fun removeSection(lineNumber: Int, sectionIndex: Int) {
        val s = _state.value
        val updated = s.lineInputs.map { li ->
            if (li.lineNumber == lineNumber) {
                val pruned = li.sections
                    .filterNot { it.section == sectionIndex }
                    .mapIndexed { idx, sec -> sec.copy(section = idx + 1) } // re-number
                li.copy(sections = if (pruned.isEmpty()) listOf(LineSectionInput(1)) else pruned)
            } else li
        }
        _state.value = s.copy(lineInputs = updated)
    }

    fun updateLineSection(
        lineNumber: Int,
        sectionIndex: Int,
        qty: Int,
        lot: String? = null,
        expiry: String? = null
    ) {
        val s = _state.value
        val updated = s.lineInputs.map { li ->
            if (li.lineNumber != lineNumber) return@map li
            val secs = li.sections.map { sec ->
                if (sec.section != sectionIndex) sec else sec.copy(
                    qty = qty,
                    lot = lot ?: sec.lot,
                    expiry = expiry ?: sec.expiry
                )
            }
            li.copy(sections = secs)
        }
        _state.value = s.copy(lineInputs = updated)
    }

    // Back-compat: old call edits section-1
    fun updateLine(
        lineNumber: Int,
        qty: Int,
        lot: String? = null,
        expiry: String? = null
    ) {
        updateLineSection(
            lineNumber = lineNumber,
            sectionIndex = 1,
            qty = qty,
            lot = lot,
            expiry = expiry
        )
    }

    fun removeLine(lineNumber: Int) {
        _state.value = _state.value.copy(
            lines = _state.value.lines.filterNot { it.LineNumber == lineNumber },
            lineInputs = _state.value.lineInputs.filterNot { it.lineNumber == lineNumber }
        )
    }

    fun addLineFromPo(lineNumber: Int) {
        val s = _state.value
        if (s.lineInputs.any { it.lineNumber == lineNumber }) return
        val poLine = s.allPoLines.firstOrNull { it.LineNumber == lineNumber } ?: return
        val newInput = poLine.toLineInput()
        _state.value = s.copy(
            lines = s.lines + poLine,
            lineInputs = s.lineInputs + newInput
        )
    }

    private fun LineSectionInput.isValidFor(line: PoLineItem): Boolean =
        qty > 0.0 && lot.isNotBlank() && expiry.isNotBlank()


    // Build staged requests: section 1 -> create, sections 2..n -> add
    fun buildPayloadsAndReview() {
        val s = _state.value
        val po = s.po ?: return

        val maxSection = s.lineInputs.maxOfOrNull { it.sections.size } ?: 0
        if (maxSection == 0) return

        val staged = mutableListOf<StagedReceipt>()
        val progress = mutableListOf<PartProgress>()

        val invoiceNo = s.invoiceNumber.trim()
        val supplierInvoiceNumber = invoiceNo.filter { it.isDigit() }
        val headerNote: String? = invoiceNo.takeIf { it.isNotEmpty() }?.let { "Supplier Invoice Number: $it" }

        for (secIdx in 1..maxSection) {
            val linesForThisSection = s.lines.mapNotNull { line ->
                val li = s.lineInputs.firstOrNull { it.lineNumber == line.LineNumber } ?: return@mapNotNull null
                val sec = li.sections.firstOrNull { it.section == secIdx } ?: return@mapNotNull null
                if (!sec.isValidFor(line)) return@mapNotNull null

                ReceiptLine(
                    OrganizationCode = BuildConfig.ORGANIZATION_CODE,
                    DocumentNumber = po.OrderNumber,
                    DocumentLineNumber = li.lineNumber,
                    ItemNumber = li.itemNumber,
                    Quantity = sec.qty,
                    UnitOfMeasure = li.uom,
                    SoldtoLegalEntity = po.SoldToLegalEntity,
                    Subinventory = BuildConfig.DEFAULT_SUBINVENTORY,
                    Locator = BuildConfig.DEFAULT_LOCATOR,
                    lotItemLots = listOf(
                        LotItem(
                            LotNumber = sec.lot,
                            TransactionQuantity = sec.qty,
                            LotExpirationDate = sec.expiry
                        )
                    )
                )
            }


            if (linesForThisSection.isNotEmpty()) {
                staged += StagedReceipt(
                    sectionIndex = secIdx,
                    request = ReceiptRequest(
                        // ReceiptHeaderId is injected only at submit time for sections > 1
                        ReceiptHeaderId = null,
                        OrganizationCode = BuildConfig.ORGANIZATION_CODE,
                        VendorName = po.Supplier,
                        VendorSiteCode = po.SupplierSite,
                        BusinessUnit = po.ProcurementBU,
                        EmployeeId = appPref.personId,
                        lines = linesForThisSection,
                        InvoiceNumber = s.invoiceNumber.takeIf { it.isNotBlank() },
                        Comments = headerNote,
                        DFF = supplierInvoiceNumber.toBigDecimalOrNull()?.let { num ->
                        listOf(ReceiptRequestDff(supplierInvoiceNumber = num, flexContext = null))
                    }

                    )
                )
                progress += PartProgress(sectionIndex = secIdx, lines = linesForThisSection.size)
            }
        }

        _state.value = s.copy(staged = staged, progress = progress, step = GrnStep.REVIEW)
    }

    fun backToReceive() { _state.value = _state.value.copy(step = GrnStep.SHOW_PO) }

    fun submitReceipt() = viewModelScope.launch {
        val s = _state.value
        val staged = s.staged
        if (staged.isEmpty()) return@launch

        _state.value = s.copy(loading = true, error = null)
        var receiptHeaderId: Long? = null
        var lastResp: ReceiptResponse? = null
        var progress = _state.value.progress.toMutableList()
        var headerInterfaceId: String? = null

        // will capture "<supplier>_<invoice>" from the FIRST body we submit
        var prefixFromFirstBody: String? = null

        staged.forEachIndexed { idx, stage ->
            // mark SUBMITTING
            progress = progress.map {
                if (it.sectionIndex == stage.sectionIndex) it.copy(status = PartStatus.SUBMITTING) else it
            }.toMutableList()
            _state.value = _state.value.copy(progress = progress)

            // Inject header id for stages > 1
            val body = if (idx == 0) stage.request else stage.request.copy(ReceiptHeaderId = receiptHeaderId)

            // capture supplier+invoice prefix from the *body*
            if (prefixFromFirstBody == null) {
                fun sanitizeToken(x: String): String =
                    x.trim().replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').take(80)
                val sup = sanitizeToken(body.VendorName)
                val inv = sanitizeToken(body.InvoiceNumber ?: "no_invoice")
                prefixFromFirstBody = "${sup}_${inv}"
            }

            val result = repo.createReceipt(body)
            result.onSuccess { resp ->
                lastResp = resp
                receiptHeaderId = receiptHeaderId ?: resp.ReceiptHeaderId

                val isSuccess = resp.ReturnStatus.equals("SUCCESS", true)
                val inlineErrors = mutableListOf<String>()
                if (!isSuccess) inlineErrors += "ReturnStatus: ${resp.ReturnStatus ?: "UNKNOWN"}"
                resp.ReturnMessage?.takeIf { it.isNotBlank() }?.let { inlineErrors += it }
                resp.Message?.takeIf { it.isNotBlank() }?.let { inlineErrors += it }

                // Add processing errors if any
                val h = resp.HeaderInterfaceId
                if (!h.isNullOrBlank() && headerInterfaceId.isNullOrBlank()) {
                    headerInterfaceId = h
                }
                val iface = resp.lines?.firstOrNull()?.InterfaceTransactionId
                if (!h.isNullOrBlank() && !iface.isNullOrBlank()) {
                    repo.fetchProcessingErrors(h, iface).onSuccess { fetched ->
                        inlineErrors += fetched.mapNotNull { it.ErrorMessage?.trim() }
                            .filter { it.isNotBlank() }
                    }.onFailure { pe ->
                        inlineErrors += "ProcessingErrors fetch failed: ${pe.message}"
                    }
                }

                progress = progress.map {
                    if (it.sectionIndex == stage.sectionIndex)
                        it.copy(
                            status = if (isSuccess) PartStatus.SUCCESS else PartStatus.FAILED,
                            message = if (isSuccess) "Part ${stage.sectionIndex} completed" else "Part ${stage.sectionIndex} failed",
                            receiptHeaderId = receiptHeaderId,
                            returnStatus = resp.ReturnStatus,
                            errors = inlineErrors
                        )
                    else it
                }.toMutableList()
                _state.value = _state.value.copy(progress = progress)

            }.onFailure { e ->
                val inlineErrors = mutableListOf<String>()
                var httpCode: Int? = null
                var url: String? = null
                var bodyText: String? = null

                if (e is HttpException) {
                    httpCode = e.code()
                    url = e.response()?.raw()?.request?.url?.toString()
                    bodyText = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                    val parsed: ReceiptResponse? = try {
                        bodyText?.takeIf { it.isNotBlank() }?.let { Gson().fromJson(it, ReceiptResponse::class.java) }
                    } catch (_: Exception) { null }

                    if (parsed != null) {
                        parsed.ReturnStatus?.let { inlineErrors += "ReturnStatus: $it" }
                        parsed.ReturnMessage?.takeIf { it.isNotBlank() }?.let { inlineErrors += it }
                        parsed.Message?.takeIf { it.isNotBlank() }?.let { inlineErrors += it }
                        // if error still included header/interface ids, keep them
                        if (!parsed.HeaderInterfaceId.isNullOrBlank() && headerInterfaceId.isNullOrBlank()) {
                            headerInterfaceId = parsed.HeaderInterfaceId
                        }
                    } else {
                        // fallback: raw text
                        bodyText?.takeIf { it.isNotBlank() }?.let { inlineErrors += it }
                    }
                }

                e.message?.let { inlineErrors += "Exception: $it" }

                progress = progress.map {
                    if (it.sectionIndex == stage.sectionIndex)
                        it.copy(
                            status = PartStatus.FAILED,
                            message = "Part ${stage.sectionIndex} failed",
                            returnStatus = "ERROR",
                            errors = inlineErrors.ifEmpty { listOf("Create Receipt / Add to Receipt API failed") },
                            httpCode = httpCode,
                            url = url,
                            errorBody = bodyText,
                            exception = e::class.java.simpleName + (e.message?.let { ": $it" } ?: "")
                        )
                    else it
                }.toMutableList()
                _state.value = _state.value.copy(progress = progress)
            }
        }

        // ===== Attachments upload (name = <supplier>_<invoice>_<idx>.<ext>) =====
        val finalHeaderInterfaceId = headerInterfaceId
        val imagePaths = _state.value.scanImageCachePaths
        val extra = _state.value.extraAttachments

        var uploadedCount = 0
        var attachIndex = 1
        val namePrefix = prefixFromFirstBody ?: run {
            fun sanitizeToken(x: String) = x.trim().replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').take(80)
            val sup = sanitizeToken(_state.value.po?.Supplier ?: "supplier")
            val inv = sanitizeToken(_state.value.invoiceNumber.ifBlank { "no_invoice" })
            "${sup}_${inv}"
        }

        if (finalHeaderInterfaceId != null && (imagePaths.isNotEmpty() || extra.isNotEmpty())) {
            fun inferExtFromMime(mime: String?): String? = when {
                mime.isNullOrBlank() -> null
                mime.endsWith("pdf", ignoreCase = true) -> "pdf"
                mime.endsWith("png", ignoreCase = true) -> "png"
                mime.endsWith("jpeg", ignoreCase = true) -> "jpg"
                mime.endsWith("jpg", ignoreCase = true) -> "jpg"
                else -> null
            }
            fun extOr(defaultExt: String, candidate: String?): String {
                val c = candidate?.lowercase()?.trim()?.removePrefix(".")
                return if (!c.isNullOrBlank()) c else defaultExt
            }

            // 1) scanned images → jpg
            for (p in imagePaths) {
                try {
                    val file = File(p)
                    if (!file.exists()) { attachIndex++; continue }
                    val bytes = file.readBytes()
                    val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    val fileName = "${namePrefix}_${attachIndex}.jpg"
                    val req = AttachmentRequest(
                        UploadedFileName = fileName,
                        CategoryName = "MISC",
                        FileContents = b64,
                        Title = fileName
                    )
                    repo.uploadAttachment(finalHeaderInterfaceId, req).onSuccess {
                        uploadedCount++
                        file.delete()
                    }.onFailure {
                        Log.e("GRN", "Attachment upload failed for $fileName: ${it.message}")
                    }
                } catch (e: Exception) {
                    Log.e("GRN", "Attachment error for $p", e)
                } finally {
                    attachIndex++
                }
            }

            // 2) user-picked attachments → infer extension
            for (att in extra) {
                try {
                    val file = File(att.path)
                    if (!file.exists()) { attachIndex++; continue }
                    val bytes = file.readBytes()
                    val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    val extFromName = file.extension.takeIf { it.isNotBlank() }
                    val extFromMime = inferExtFromMime(att.mimeType)
                    val ext = extOr("bin", extFromMime ?: extFromName)
                    val fileName = "${namePrefix}_${attachIndex}.${ext}"
                    val req = AttachmentRequest(
                        UploadedFileName = fileName,
                        CategoryName = "MISC",
                        FileContents = b64,
                        Title = fileName
                    )
                    repo.uploadAttachment(finalHeaderInterfaceId, req).onSuccess {
                        uploadedCount++
                        file.delete()
                    }.onFailure {
                        Log.e("GRN", "Attachment upload failed for $fileName: ${it.message}")
                    }
                } catch (e: Exception) {
                    Log.e("GRN", "Attachment error for ${att.path}", e)
                } finally {
                    attachIndex++
                }
            }
        }

        _state.value = _state.value.copy(
            loading = false,
            receipt = lastResp,
            attachmentsUploaded = uploadedCount,
            step = GrnStep.SUMMARY
        )
    }


    fun startOver() {
        _state.value = GrnUiState()
    }

    // NOTE: third param represents CACHE PATHS for scanned images
    fun prefillFromScan(po: String?, scanJson: String?, cachePaths: List<String>) {
        Log.d(
            "GRN",
            "prefillFromScan(po=$po, jsonLen=${scanJson?.length ?: 0}, cachePaths=${cachePaths.size})"
        )

        var next = _state.value
        if (!po.isNullOrBlank()) next = next.copy(poNumber = po)

        if (!scanJson.isNullOrBlank()) {
            try {
                val transfer = Gson().fromJson(
                    scanJson,
                    ScanExtractTransfer::class.java
                )
                val normalized = transfer.items.map {
                    ExtractedItem(
                        description = it.description,
                        qtyDelivered = it.qty,
                        expiryDate = it.expiry,
                        batchNo = it.batchNo
                    )
                }
                val poNum = if (!po.isNullOrBlank()) po else transfer.poNumber
                val invoiceNumber = transfer.invoiceNo
                next = next.copy(
                    poNumber = poNum,
                    invoiceNumber = invoiceNumber,
                    extractedFromScan = normalized,
                    scanImageCachePaths = cachePaths
                )
            } catch (e: Exception) {
                Log.e("GRN", "Failed to read scan payload: ${e.message}")
                next = next.copy(
                    error = "Failed to read scan payload: ${e.message}",
                    scanImageCachePaths = cachePaths
                )
            }
        } else {
            if (cachePaths.isNotEmpty()) next = next.copy(scanImageCachePaths = cachePaths)
        }

        _state.value = next
        if (_state.value.poNumber.isNotBlank() && _state.value.step == GrnStep.ENTER_PO) {
            fetchPo()
        }
    }


    fun addManualAttachmentsFromUris(context: Context, uris: List<Uri>) = viewModelScope.launch {
        val cr = context.contentResolver
        val added = mutableListOf<AttachmentCache>()
        fun sanitize(name: String) = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        var fallback = 1

        uris.forEach { u ->
            try {
                var display = run {
                    var name: String? = null
                    cr.query(u, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                        if (c.moveToFirst()) name = c.getString(0)
                    }
                    name?.takeIf { it.isNotBlank() }?.let(::sanitize)
                }
                val mime = cr.getType(u)
                if (display.isNullOrBlank()) {
                    val ext = when {
                        (mime ?: "").endsWith("pdf") -> "pdf"
                        (mime ?: "").endsWith("png") -> "png"
                        (mime ?: "").endsWith("jpeg") || (mime ?: "").endsWith("jpg") -> "jpg"
                        else -> "bin"
                    }
                    display = "attach_${fallback++}.$ext"
                }

                // ensure unique filename in cache
                var target = File(context.cacheDir, display)
                var i = 1
                while (target.exists()) {
                    val base = target.nameWithoutExtension
                    val ext = target.extension.takeIf { it.isNotBlank() }?.let { ".$it" } ?: ""
                    target = File(context.cacheDir, "${base}_$i$ext")
                    i++
                }

                cr.openInputStream(u)?.use { input ->
                    target.outputStream().use { out -> input.copyTo(out) }
                } ?: return@forEach

                added += AttachmentCache(path = target.absolutePath, displayName = target.name, mimeType = mime)
            } catch (e: Exception) {
                Log.e("GRN", "Failed to cache attachment from $u", e)
            }
        }

        if (added.isNotEmpty()) {
            _state.value = _state.value.copy(extraAttachments = _state.value.extraAttachments + added)
        }
    }
}
