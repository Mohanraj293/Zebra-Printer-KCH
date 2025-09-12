package com.lazymohan.zebraprinter.graph.drive

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.*

interface GraphDriveApi {
    @GET("v1.0/drives/{driveId}/root:/{folder}/{fileName}:/content")
    suspend fun downloadFile(
        @Path("driveId") driveId: String,
        @Path("folder", encoded = true) folder: String,
        @Path("fileName") fileName: String
    ): ResponseBody

    @PUT("v1.0/drives/{driveId}/root:/{folder}/{fileName}:/content")
    suspend fun uploadFile(
        @Path("driveId") driveId: String,
        @Path("folder", encoded = true) folder: String,
        @Path("fileName") fileName: String,
        @Body body: RequestBody,
        @Header("Content-Type") contentType: String
    ): ResponseBody
}
