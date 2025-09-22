package com.lazymohan.zebraprinter.scan.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.lazymohan.zebraprinter.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.nio.charset.Charset
import javax.inject.Inject

sealed class OcrResult {
    data class Success(val content: OcrContentResponse) : OcrResult()
    data class HttpError(val code: Int, val message: String?, val raw: String?) : OcrResult()
    data class ExceptionError(val message: String?) : OcrResult()
}

class OcrRepository @Inject constructor(
    private val api: OcrApi
) {
    private val base = BuildConfig.OCR_BASE_URL
    private val apiKey = BuildConfig.OCR_API_KEY
    private val TAG = "OCR"
    private val gson = Gson()

    // sample payload for fake mode
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
    "order_number": "DXB-06",
    "sales_order_no": "5285622",
    "customer_ref_purchase_order_no": "KHQ/PO/99373",
    "items": [
      {
        "description": "CAMAZIN K2 TABLETS 30'S",
        "details": [
          {
            "qty_delivered": "50",
            "expiry_date": "2027-03-31",
            "batch_no": "SR328"
          },
          {
            "qty_delivered": "2",
            "expiry_date": "2027-03-31",
            "batch_no": "SR328"
          }
        ]
      },
      {
        "description": "XIGDUO XR 10/500 OF 30'S",
        "details": [
          {
            "qty_delivered": "25",
            "expiry_date": "2027-03-31",
            "batch_no": "WH0319"
          },
          {
            "qty_delivered": "1",
            "expiry_date": "2027-03-31",
            "batch_no": "WH0319"
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
        gson.fromJson(json, OcrContentResponse::class.java)

    private fun logD(msg: String) {
        Log.d(TAG, msg)
        Firebase.crashlytics.log("[$TAG] $msg")
    }

    private fun String?.isMeaningful(): Boolean =
        !this.isNullOrBlank() && this.trim() != "null"

    private fun loadAssetOrNull(context: Context, assetName: String): String? = try {
        context.assets.open(assetName).use { it.readBytes().toString(Charset.defaultCharset()) }
    } catch (_: Exception) { null }

    /** Robust status read via JSON tree so it works regardless of field mapping/minify. */
    private fun readStatus(obj: OcrContentResponse): String {
        return try {
            val tree: JsonObject = gson.toJsonTree(obj).asJsonObject
            when {
                tree.has("status") && !tree["status"].isJsonNull -> tree["status"].asString
                else -> ""
            }
        } catch (_: Throwable) { "" }
    }

    suspend fun uploadAndPoll(
        context: Context,
        imageUri: Uri,
        label: String = "delivery",
        maxAttempts: Int = 60,
        intervalMs: Long = 1500
    ): OcrResult = withContext(Dispatchers.IO) {
        logD("OCR: start")

        // ---- FAKE SHORT-CIRCUIT ----
        if (BuildConfig.OCR_FAKE) {
            delay(200)
            val json = if (BuildConfig.OCR_FAKE_ASSET.isMeaningful())
                loadAssetOrNull(context, BuildConfig.OCR_FAKE_ASSET) ?: FAKE_JSON
            else FAKE_JSON

            return@withContext try {
                logD("OCR: FAKE success")
                OcrResult.Success(parseFake(json))
            } catch (e: Exception) {
                logD("OCR: FAKE parse error ${e.message}")
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

            val uploadUrl = "$base/infer-with-ocr"
            val upload: Response<OcrUploadResponse> =
                api.upload(uploadUrl, filePart, labelBody, apiKey)

            if (!upload.isSuccessful) {
                val raw = upload.errorBody()?.string()
                logD("OCR: upload failed code=${upload.code()}")
                return@withContext OcrResult.HttpError(upload.code(), "Upload failed", raw)
            }

            val body = upload.body()
            val checkUrl = when {
                body?.checkStatusUrl.isNullOrBlank() ->
                    return@withContext OcrResult.ExceptionError("Missing check_status_url")
                body.checkStatusUrl.startsWith("http") -> body.checkStatusUrl
                else -> "$base${body.checkStatusUrl}"
            }
            logD("OCR: checkUrl=$checkUrl")

            // Poll until status == "completed" (or timeout)
            repeat(maxAttempts) { attempt ->
                delay(intervalMs)
                val resp = api.getContent(checkUrl, apiKey)
                if (!resp.isSuccessful) {
                    logD("OCR: poll #${attempt + 1} HTTP ${resp.code()} (continue)")
                    return@repeat
                }

                val ok = resp.body()
                if (ok == null) {
                    logD("OCR: poll #${attempt + 1} empty body (continue)")
                    return@repeat
                }

                val status = readStatus(ok).lowercase()
                logD("OCR: poll #${attempt + 1} status=$status")

                if (status == "completed") {
                    logD("OCR: completed -> STOP")
                    return@withContext OcrResult.Success(ok)
                }
            }

            logD("OCR: timeout waiting for status=completed")
            OcrResult.ExceptionError("Timed out waiting for status=completed")
        } catch (e: Exception) {
            logD("OCR: exception ${e.message}")
            Firebase.crashlytics.recordException(e)
            OcrResult.ExceptionError(e.message)
        }
    }
}
