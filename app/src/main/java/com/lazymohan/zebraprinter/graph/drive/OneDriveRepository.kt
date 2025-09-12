package com.lazymohan.zebraprinter.graph.drive

import com.lazymohan.zebraprinter.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OneDriveRepository @Inject constructor(
    private val api: GraphDriveApi
) {
    private val driveId: String = BuildConfig.GRAPH_DRIVE_ID
    private val folderPath: String = BuildConfig.GRAPH_FOLDER_PATH

    private val masterCsvFile: String = BuildConfig.MASTER_CSV_FILE

    private val countTemplateCsvFile: String = BuildConfig.COUNTS_CSV_FILE

    /** Download the latest OnHand master CSV from OneDrive. */
    suspend fun downloadMasterCsv(): ByteArray = withContext(Dispatchers.IO) {
        api.downloadFile(driveId, folderPath, masterCsvFile).bytes()
    }

    /** Download the Physical Count CSV template from OneDrive (first line used as header). */
    suspend fun downloadCountTemplateCsv(): ByteArray = withContext(Dispatchers.IO) {
        api.downloadFile(driveId, folderPath, countTemplateCsvFile).bytes()
    }

    /** Upload a CSV string as a new file under .../<GRAPH_FOLDER_PATH>/physical count files/{fileName}. */
    suspend fun uploadCountsFromString(csv: String, fileName: String) = withContext(Dispatchers.IO) {
        val csvBody = csv.toRequestBody("text/csv".toMediaType())
        val subFolder = encodeSegment("physical count files")
        val uploadFolder = "$folderPath/$subFolder"
        api.uploadFile(
            driveId = driveId,
            folder = uploadFolder,
            fileName = fileName,
            body = csvBody,
            contentType = "text/csv"
        )
    }

    private fun encodeSegment(seg: String): String = seg.replace(" ", "%20")
}
