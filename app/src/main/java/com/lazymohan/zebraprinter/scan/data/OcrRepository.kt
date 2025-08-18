// app/src/main/java/com/lazymohan/zebraprinter/scan/data/OcrRepository.kt
package com.lazymohan.zebraprinter.scan.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import javax.inject.Inject
import kotlin.io.readBytes

sealed class OcrResult {
    data class Success(val content: OcrContentResponse) : OcrResult()
    data class HttpError(val code: Int, val message: String?, val raw: String?) : OcrResult()
    data class ExceptionError(val message: String?) : OcrResult()
}

class OcrRepository @Inject constructor(
    private val api: OcrApi
) {
    private val base = "http://kch-ocr.tarkalabs.com"

    suspend fun uploadAndPoll(
        context: Context,
        imageUri: Uri,
        label: String = "delivery",
        maxAttempts: Int = 60,
        intervalMs: Long = 1500
    ): OcrResult = withContext(Dispatchers.IO) {
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
                api.upload("$base/upload", filePart, labelBody)

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
                body!!.checkStatusUrl!!.startsWith("http") -> body.checkStatusUrl!!
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
