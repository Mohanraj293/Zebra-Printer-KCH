package com.lazymohan.zebraprinter.scan.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.lazymohan.zebraprinter.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import javax.inject.Inject

sealed class OcrResult {
    data class Success(val content: OcrContentResponse) : OcrResult()
    data class HttpError(val code: Int, val message: String?, val raw: String?) : OcrResult()
    data class ExceptionError(val message: String?) : OcrResult()
}

class OcrRepository @Inject constructor(
    private val api: OcrApi
) {
    private val base = "http://kch-ocr.tarkalabs.com"

    // For testing purposes, we can using sample JSON response
    private val FAKE_JSON: String = """
{
  "id": 205997918,
  "filename": "MPC_205997918_page-0001.jpg",
  "label": "delivery",
  "status": "completed",
  "task_id": "generated-task-id",
  "extracted_text": {
    "invoice_no": "205997918",
    "invoice_date": "30/06/2025",
    "order_number": "101002",
    "sales_order_no": "5285622",
    "customer_ref_purchase_order_no": "KHQ/PO/99387",
    "items": [
      {
        "description": "Ticagrelor 90 mg (Brilinta) Tablets",
        "details": [
          {
            "qty_delivered": "56",
            "expiry_date": "",
            "batch_no": ""
          }
        ]
      }
    ]
  },
  "easy_ocr_text": null,
  "tesseract_text": null,
  "created_at": "CURRENT_TIMESTAMP"
}
""".trimIndent()

    private fun parseFake(json: String): OcrContentResponse =
        Gson().fromJson(json, OcrContentResponse::class.java)

    suspend fun uploadAndPoll(
        context: Context,
        imageUri: Uri,
        label: String,
        maxAttempts: Int = 60,
        intervalMs: Long = 1500
    ): OcrResult = withContext(Dispatchers.IO) {
        // ---- FAKE SHORT-CIRCUIT ----
        if (BuildConfig.OCR_FAKE) {
            delay(400) // tiny delay to mimic network
            return@withContext try {
                val payload = parseFake(FAKE_JSON)
                OcrResult.Success(payload)
            } catch (e: Exception) {
                OcrResult.ExceptionError("Fake JSON parse error: ${e.message}")
            }
        }
        // ----------------------------

        try {
            val resolver = context.contentResolver
            val mime = resolver.getType(imageUri) ?: "image/jpeg"
            val fileName = "KCH_upload.${mime.substringAfterLast('/', "jpg")}"

            val bytes = resolver.openInputStream(imageUri)?.use { it.readBytes() }
                ?: return@withContext OcrResult.ExceptionError("Cannot read image data")

            val fileBody: RequestBody = bytes.toRequestBody(mime.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", fileName, fileBody)
            val labelBody = label.toRequestBody("text/plain".toMediaTypeOrNull())

            val upload: Response<OcrUploadResponse> =
                api.upload("$base/image-to-llm", filePart, labelBody)

            if (upload.code() != 201) {
                return@withContext OcrResult.HttpError(
                    upload.code(),
                    "Upload failed",
                    upload.errorBody()?.string()
                )
            }

            val body = upload.body()
            val checkUrl = when {
                body?.checkStatusUrl.isNullOrBlank() ->
                    return@withContext OcrResult.ExceptionError("Missing check_status_url")
                body.checkStatusUrl.startsWith("http") -> body.checkStatusUrl
                else -> "$base${body.checkStatusUrl}"
            }

            repeat(maxAttempts) {
                delay(intervalMs)
                val content = api.getContent(checkUrl)
                if (content.code() == 200) {
                    val ok = content.body()
                    if (ok != null) return@withContext OcrResult.Success(ok)
                    return@withContext OcrResult.HttpError(200, "Empty body", null)
                }
            }
            OcrResult.ExceptionError("Timed out waiting for OCR result")
        } catch (e: Exception) {
            OcrResult.ExceptionError(e.message)
        }
    }
}
