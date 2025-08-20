// app/src/main/java/com/lazymohan/zebraprinter/scan/data/OcrDtos.kt
package com.lazymohan.zebraprinter.scan.data

import com.google.gson.annotations.SerializedName

data class OcrUploadResponse(
    val id: Int,
    val filename: String?,
    @SerializedName("file_path") val filePath: String?,
    val label: String?,
    @SerializedName("task_id") val taskId: String?,
    val status: String?,
    val message: String?,
    @SerializedName("check_status_url") val checkStatusUrl: String?
)

data class OcrContentResponse(
    val id: Int?,
    val filename: String?,
    val label: String?,
    val status: String?,
    @SerializedName("task_id") val taskId: String?,
    @SerializedName("extracted_text") val extractedText: ExtractedText?,
    @SerializedName("easy_ocr_text") val easyOcrText: String?,
    @SerializedName("tesseract_text") val tesseractText: String?,
    @SerializedName("created_at") val createdAt: String?
)

data class ExtractedText(
    @SerializedName("invoice_no") val invoiceNo: String?,
    @SerializedName("invoice_date") val invoiceDate: String?,
    @SerializedName("PO_no") val poNo: String?,
    val items: List<ExtractedItem> = emptyList()
)

data class ExtractedItem(
    val description: String?,
    @SerializedName("qty_delivered") val qtyDelivered: String?,
    @SerializedName("expiry_date") val expiryDate: String?,
    @SerializedName("batch_no") val batchNo: String?
)
