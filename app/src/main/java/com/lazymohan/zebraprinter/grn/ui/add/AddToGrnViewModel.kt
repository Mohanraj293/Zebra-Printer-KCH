package com.lazymohan.zebraprinter.grn.ui.add

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazymohan.zebraprinter.BuildConfig
import com.lazymohan.zebraprinter.app.AppPref
import com.lazymohan.zebraprinter.grn.data.*
import com.lazymohan.zebraprinter.grn.ui.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.File
import javax.inject.Inject

data class ExistingGrn(
    val headerInterfaceId: Long?,
    val receiptHeaderId: Long?,
    val receiptNumber: String?,
    val vendorName: String?,
    val employeeName: String?,
    val processingStatus: String?
)

data class AddToGrnUiState(
    val step: AddStep = AddStep.ENTER_PO,
    val base: GrnUiState = GrnUiState(),
    val loading: Boolean = false,
    val error: String? = null,
    val existing: List<ExistingGrn> = emptyList(),
    val selected: ExistingGrn? = null
)

@HiltViewModel
class AddToGrnViewModel @Inject constructor(
    private val repo: GrnRepository,
    private val appPref: AppPref
) : ViewModel() {

    private val _ui = MutableStateFlow(AddToGrnUiState())
    val ui: StateFlow<AddToGrnUiState> = _ui

    private fun updateBase(transform: (GrnUiState) -> GrnUiState) {
        _ui.value = _ui.value.copy(base = transform(_ui.value.base))
    }

    fun setPoNumber(po: String) = updateBase { it.copy(poNumber = po) }

    fun updateInvoiceNumber(text: String) = updateBase { it.copy(invoiceNumber = text) }

    /** Step 1 -> Step 2: Find SUCCESS receipts by PO using lifecycle API, then enrich from headers. */
    fun findExistingGrns() = viewModelScope.launch {
        val po = _ui.value.base.poNumber
        if (po.isBlank()) return@launch

        _ui.value = _ui.value.copy(loading = true, error = null, existing = emptyList(), step = AddStep.SELECT_GRN)

        // A) lifecycle receipts under this PO (these are posted receipts)
        val lifecycle = repo.getLifecycleReceiptsByPo(po).getOrElse { e ->
            _ui.value = _ui.value.copy(loading = false, error = e.message ?: "Lifecycle lookup failed")
            return@launch
        }

        if (lifecycle.isEmpty()) {
            _ui.value = _ui.value.copy(loading = false, existing = emptyList())
            return@launch
        }

        // B) enrich each receipt by querying receivingReceiptRequests (SUCCESS) by ReceiptHeaderId
        val enriched = lifecycle.map { r ->
            async {
                val hdr = r.ReceiptId
                if (hdr == null) return@async ExistingGrn(
                    headerInterfaceId = null,
                    receiptHeaderId = null,
                    receiptNumber = r.Receipt,
                    vendorName = null,
                    employeeName = r.ReceivedBy,        // from lifecycle
                    processingStatus = "SUCCESS"
                )
                val found = repo.findHeaderByReceiptHeaderIdSuccess(hdr).getOrNull()
                ExistingGrn(
                    headerInterfaceId = found?.HeaderInterfaceId?.toLongOrNull(),
                    receiptHeaderId = hdr,
                    receiptNumber = found?.ReceiptNumber ?: r.Receipt,
                    vendorName = found?.VendorName,
                    employeeName = found?.EmployeeName ?: r.ReceivedBy,
                    processingStatus = found?.ProcessingStatusCode ?: "SUCCESS"
                )
            }
        }.map { it.await() }

        _ui.value = _ui.value.copy(loading = false, existing = enriched)
    }

    /** Select GRN -> Prefetch PO + lines -> SHOW_PO */
    fun selectExistingGrn(grn: ExistingGrn) = viewModelScope.launch {
        _ui.value = _ui.value.copy(selected = grn, loading = true, error = null)

        val poNumber = _ui.value.base.poNumber
        val po = repo.fetchPo(poNumber).getOrElse { e ->
            _ui.value = _ui.value.copy(loading = false, error = e.message ?: "PO not found")
            return@launch
        }
        updateBase { it.copy(po = po) }

        val lines = repo.fetchPoLines(po.POHeaderId).getOrElse { e ->
            _ui.value = _ui.value.copy(loading = false, error = e.message ?: "PO lines failed")
            return@launch
        }

        val inputs = lines.map {
            LineInput(
                lineNumber = it.LineNumber,
                itemNumber = it.Item.trim(),
                uom = it.UOM,
                maxQty = it.Quantity,
                description = it.Description ?: "",
                gtin = it.GTIN,
                sections = listOf(LineSectionInput(1))
            )
        }

        updateBase {
            it.copy(
                allPoLines = lines,
                lines = lines,
                lineInputs = inputs,
                linesLoaded = true,
                step = GrnStep.SHOW_PO
            )
        }

        _ui.value = _ui.value.copy(loading = false, step = AddStep.SHOW_PO)
    }

    // ===== copy of helpers from your GRN VM =====

    fun addSection(lineNumber: Int) = updateBase {
        it.copy(lineInputs = it.lineInputs.map { li ->
            if (li.lineNumber == lineNumber) {
                val nextIdx = (li.sections.maxOfOrNull { s -> s.section } ?: 0) + 1
                li.copy(sections = li.sections + LineSectionInput(section = nextIdx))
            } else li
        })
    }

    fun removeSection(lineNumber: Int, sectionIndex: Int) = updateBase {
        it.copy(lineInputs = it.lineInputs.map { li ->
            if (li.lineNumber == lineNumber) {
                val pruned = li.sections.filterNot { s -> s.section == sectionIndex }
                    .mapIndexed { idx, sec -> sec.copy(section = idx + 1) }
                li.copy(sections = if (pruned.isEmpty()) listOf(LineSectionInput(1)) else pruned)
            } else li
        })
    }

    fun updateLineSection(lineNumber: Int, sectionIndex: Int, qty: Int, lot: String?, expiry: String?) =
        updateBase {
            it.copy(lineInputs = it.lineInputs.map { li ->
                if (li.lineNumber != lineNumber) li else li.copy(
                    sections = li.sections.map { sec ->
                        if (sec.section != sectionIndex) sec else sec.copy(
                            qty = qty, lot = lot ?: sec.lot, expiry = expiry ?: sec.expiry
                        )
                    }
                )
            })
        }

    fun updateLine(lineNumber: Int, qty: Int, lot: String?, expiry: String?) =
        updateLineSection(lineNumber, 1, qty, lot, expiry)

    fun removeLine(lineNumber: Int) = updateBase {
        it.copy(
            lines = it.lines.filterNot { ln -> ln.LineNumber == lineNumber },
            lineInputs = it.lineInputs.filterNot { ln -> ln.lineNumber == lineNumber }
        )
    }

    fun addLineFromPo(lineNumber: Int) = updateBase {
        if (it.lineInputs.any { li -> li.lineNumber == lineNumber }) it
        else {
            val poLine = it.allPoLines.firstOrNull { l -> l.LineNumber == lineNumber } ?: return@updateBase it
            it.copy(
                lines = it.lines + poLine,
                lineInputs = it.lineInputs + LineInput(
                    lineNumber = poLine.LineNumber,
                    itemNumber = poLine.Item.trim(),
                    uom = poLine.UOM,
                    maxQty = poLine.Quantity,
                    description = poLine.Description ?: "",
                    gtin = poLine.GTIN,
                    sections = listOf(LineSectionInput(1))
                )
            )
        }
    }

    private fun LineSectionInput.isValidFor(line: PoLineItem): Boolean =
        qty > 0.0 && lot.isNotBlank() && expiry.isNotBlank()

    fun buildPayloadsAndReview() {
        val s = _ui.value.base
        val po = s.po ?: return

        val maxSection = s.lineInputs.maxOfOrNull { it.sections.size } ?: 0
        if (maxSection == 0) return

        val staged = mutableListOf<StagedReceipt>()
        val progress = mutableListOf<PartProgress>()

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
                        ReceiptHeaderId = null, // inject selected header at submit time
                        OrganizationCode = BuildConfig.ORGANIZATION_CODE,
                        VendorName = s.po.Supplier,
                        InvoiceNumber = s.invoiceNumber.ifBlank { null },
                        VendorSiteCode = s.po.SupplierSite,
                        BusinessUnit = s.po.ProcurementBU,
                        EmployeeId = appPref.personId,
                        lines = linesForThisSection
                    )
                )
                progress += PartProgress(sectionIndex = secIdx, lines = linesForThisSection.size)
            }
        }

        updateBase { it.copy(staged = staged, progress = progress, step = GrnStep.REVIEW) }
        _ui.value = _ui.value.copy(step = AddStep.REVIEW)
    }

    fun backToReceive() {
        updateBase { it.copy(step = GrnStep.SHOW_PO) }
        _ui.value = _ui.value.copy(step = AddStep.SHOW_PO)
    }

    /** Submit: ADD to chosen ReceiptHeaderId (no create). */
    fun submitAddToExisting() = viewModelScope.launch {
        val base = _ui.value.base
        val staged = base.staged
        val chosen = _ui.value.selected ?: return@launch
        if (staged.isEmpty()) return@launch

        val existingReceiptHeaderId: Long? = chosen.receiptHeaderId

        updateBase { it.copy(loading = true, error = null) }

        var lastResp: ReceiptResponse? = null
        var progress = _ui.value.base.progress.toMutableList()

        staged.forEach { stage ->
            progress = progress.map {
                if (it.sectionIndex == stage.sectionIndex) it.copy(status = PartStatus.SUBMITTING) else it
            }.toMutableList()
            updateBase { it.copy(progress = progress) }

            val body = stage.request.copy(ReceiptHeaderId = existingReceiptHeaderId)
            val result = repo.createReceipt(body)

            result.onSuccess { resp ->
                lastResp = resp
                val isSuccess = resp.ReturnStatus.equals("SUCCESS", true)
                progress = progress.map {
                    if (it.sectionIndex == stage.sectionIndex)
                        it.copy(
                            status = if (isSuccess) PartStatus.SUCCESS else PartStatus.FAILED,
                            message = if (isSuccess) "Added items to GRN" else "Add failed",
                            receiptHeaderId = existingReceiptHeaderId,
                            returnStatus = resp.ReturnStatus,
                            errors = emptyList()
                        )
                    else it
                }.toMutableList()
                updateBase { it.copy(progress = progress) }
            }.onFailure { e ->
                val httpCode: Int?
                val url: String?
                val bodyText: String?

                if (e is HttpException) {
                    httpCode = e.code()
                    url = e.response()?.raw()?.request?.url?.toString()
                    bodyText = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                } else {
                    httpCode = null; url = null; bodyText = null
                }

                val failMsg = buildString {
                    appendLine("Add to GRN API failed${httpCode?.let { " ($it)" } ?: ""}")
                    if (!url.isNullOrBlank()) appendLine("URL: $url")
                    if (!bodyText.isNullOrBlank()) appendLine(bodyText)
                    e.message?.let { appendLine("Exception: $it") }
                }.trim()

                progress = progress.map {
                    if (it.sectionIndex == stage.sectionIndex)
                        it.copy(
                            status = PartStatus.FAILED,
                            message = "Part ${stage.sectionIndex} failed",
                            returnStatus = "ERROR",
                            errors = listOf(failMsg),
                            httpCode = httpCode,
                            url = url,
                            errorBody = bodyText,
                            exception = e::class.java.simpleName + (e.message?.let { ": $it" } ?: "")
                        )
                    else it
                }.toMutableList()
                updateBase { it.copy(progress = progress) }
            }
        }

        // Upload any attachments to selected ReceiptHeaderId (string)
        val imagePaths = _ui.value.base.scanImageCachePaths
        val extra = _ui.value.base.extraAttachments
        var uploadedCount = 0
        val uploadId = (chosen.receiptHeaderId ?: 0L).toString()

        if (uploadId.isNotBlank() && (imagePaths.isNotEmpty() || extra.isNotEmpty())) {
            for ((i, p) in imagePaths.withIndex()) {
                try {
                    val f = File(p); if (!f.exists()) continue
                    val b64 = android.util.Base64.encodeToString(f.readBytes(), android.util.Base64.NO_WRAP)
                    val fileName = "delivery_slip_${i + 1}.jpg"
                    val req = AttachmentRequest(
                        UploadedFileName = fileName,
                        CategoryName = "MISC",
                        FileContents = b64,
                        Title = fileName
                    )
                    repo.uploadAttachment(uploadId, req).onSuccess {
                        uploadedCount++; f.delete()
                    }
                } catch (e: Exception) {
                    Log.e("ADD-GRN", "Attachment error for $p", e)
                }
            }
            for (att in extra) {
                try {
                    val f = File(att.path); if (!f.exists()) continue
                    val b64 = android.util.Base64.encodeToString(f.readBytes(), android.util.Base64.NO_WRAP)
                    val fileName = att.displayName.ifBlank { f.name }
                    val req = AttachmentRequest(
                        UploadedFileName = fileName,
                        CategoryName = "MISC",
                        FileContents = b64,
                        Title = fileName
                    )
                    repo.uploadAttachment(uploadId, req).onSuccess {
                        uploadedCount++; f.delete()
                    }
                } catch (e: Exception) {
                    Log.e("ADD-GRN", "Attachment error for ${att.path}", e)
                }
            }
        }

        updateBase {
            it.copy(
                loading = false,
                receipt = lastResp,
                attachmentsUploaded = uploadedCount,
                step = GrnStep.SUMMARY
            )
        }
        _ui.value = _ui.value.copy(step = AddStep.SUMMARY)
    }

    // Attachments cache (same as your GRN VM)
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

                var target = File(context.cacheDir, display!!)
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
                Log.e("ADD-GRN", "Failed to cache attachment from $u", e)
            }
        }

        if (added.isNotEmpty()) {
            updateBase { it.copy(extraAttachments = it.extraAttachments + added) }
        }
    }

    fun startOver() { _ui.value = AddToGrnUiState() }
}
