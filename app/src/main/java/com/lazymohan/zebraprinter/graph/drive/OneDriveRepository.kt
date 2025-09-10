package com.lazymohan.zebraprinter.graph.drive

import com.lazymohan.zebraprinter.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OneDriveRepository @Inject constructor(
    private val api: GraphDriveApi
) {
    private val driveId = BuildConfig.GRAPH_DRIVE_ID
    private val folderPath = BuildConfig.GRAPH_FOLDER_PATH

    suspend fun downloadMasterCsv(): ByteArray = withContext(Dispatchers.IO) {
        api.downloadFile(driveId, folderPath, BuildConfig.MASTER_CSV_FILE).bytes()
    }

    suspend fun uploadCountsAsXlsx(xlsxBytes: ByteArray) = withContext(Dispatchers.IO) {
        val contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        val body = xlsxBytes.toRequestBody(contentType.toMediaType())
        api.uploadFile(
            driveId = driveId,
            folder = folderPath,
            fileName = BuildConfig.COUNTS_XLSX_FILE,
            body = body,
            contentType = contentType
        )
    }

    // small helper if you want to send String directly
    suspend fun uploadCountsFromString(csv: String, charset: Charset = Charsets.UTF_8) {
        uploadCountsAsXlsx(csv.toByteArray(charset))
    }
}
