package com.lazymohan.zebraprinter.product.data

import android.util.Base64
import android.util.Log
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.Serializable

class ProductsRepo {

    suspend fun getProducts(searchQuery: String, limit: Int, offset: Int): InventoryResponse? =
        withContext(Dispatchers.IO) {
            val service = ApiClient.create()
            try {
                val response = service.getInventoryItemLots(
                    query = "ItemDescription LIKE '%$searchQuery%'",
                    limit = limit,
                    offset = offset
                )
                if (response.isSuccessful) {
                    response.body()
                } else {
                    Log.e("API", "Error: ${response.errorBody()?.string()}")
                    null
                }
            } catch (e: Exception) {
                Log.e("API", "Exception: ${e.localizedMessage}", e)
                null
            }
        }
}

interface OracleApiService {
    @GET("fscmRestApi/resources/11.13.18.05/inventoryItemLots")
    suspend fun getInventoryItemLots(
        @Query("onlyData") onlyData: Boolean = true,
        @Query("q") query: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Response<InventoryResponse>
}

object ApiClient {

    private const val BASE_URL = "https://effb-test.fa.em3.oraclecloud.com/"

    fun create(): OracleApiService {
        val logging = HttpLoggingInterceptor().apply {
            setLevel(HttpLoggingInterceptor.Level.BODY)
        }
        val username = "RCA.Automation"
        val password = "Kch12345"
        val credentials = "$username:$password"
        val encoded = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
        val basicAuthHeader = "Basic $encoded"

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", basicAuthHeader)
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Or Moshi
            .client(okHttpClient)
            .build()

        return retrofit.create(OracleApiService::class.java)
    }
}

data class InventoryResponse(
    @SerializedName("items") val items: List<Item>,
    @SerializedName("count") val count: Int,
    @SerializedName("hasMore") val hasMore: Boolean,
    @SerializedName("limit") val limit: Int,
    @SerializedName("offset") val offset: Int
)

data class Item(
    @SerializedName("OrganizationId") val organizationId: Long,
    @SerializedName("OrganizationCode") val organizationCode: String?,
    @SerializedName("OrganizationName") val organizationName: String?,
    @SerializedName("InventoryItemId") val inventoryItemId: Long,
    @SerializedName("ItemNumber") val itemNumber: String?,
    @SerializedName("ItemDescription") val itemDescription: String?,
    @SerializedName("LotNumber") val lotNumber: String?,
    @SerializedName("ParentLotNumber") val parentLotNumber: String?,
    @SerializedName("ActiveLotCode") val activeLotCode: String?,
    @SerializedName("ActiveLot") val activeLot: String?,
    @SerializedName("OriginationTypeCode") val originationTypeCode: String?,
    @SerializedName("OriginationType") val originationType: String?,
    @SerializedName("StatusId") val statusId: Int?,
    @SerializedName("StatusCode") val statusCode: String?,
    @SerializedName("Grade") val grade: String?,
    @SerializedName("ExpirationActionCode") val expirationActionCode: String?,
    @SerializedName("ExpirationAction") val expirationAction: String?,
    @SerializedName("OriginationDate") val originationDate: String?,
    @SerializedName("ExpirationDate") val expirationDate: String?,
    @SerializedName("MaturityDate") val maturityDate: String?,
    @SerializedName("ExpirationActionDate") val expirationActionDate: String?,
    @SerializedName("HoldUntilDate") val holdUntilDate: String?,
    @SerializedName("RetestDate") val retestDate: String?,
    @SerializedName("DisabledCode") val disabledCode: String?,
    @SerializedName("Disabled") val disabled: String?,
    @SerializedName("UniqueDeviceIdentifier") val uniqueDeviceIdentifier: String?,
    @SerializedName("ItemPrimaryImageURL") val itemPrimaryImageUrl: String?,
    @SerializedName("LotStatusMeaning") val lotStatusMeaning: String?,
    @SerializedName("GradeDescription") val gradeDescription: String?,
    @SerializedName("ElectronicRecordId") val electronicRecordId: String?,
    @SerializedName("ElectronicRecordApprovalStatus") val electronicRecordApprovalStatus: String?,
    @SerializedName("UpdateSourceCode") val updateSourceCode: String?
): Serializable