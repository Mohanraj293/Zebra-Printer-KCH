// app/src/main/java/com/lazymohan/zebraprinter/scan/data/OcrApi.kt
package com.lazymohan.zebraprinter.scan.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface OcrApi {
    @Multipart
    @POST
    suspend fun upload(
        @Url url: String,
        @Part file: MultipartBody.Part,
        @Part("label") label: RequestBody
    ): Response<OcrUploadResponse>

    @GET
    suspend fun getContent(@Url url: String): Response<OcrContentResponse>
}
