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

    suspend fun getLots(searchQuery: String, limit: Int, offset: Int): InventoryResponse? =
        withContext(Dispatchers.IO) {
            val service = ApiClient.create()
            try {
                val response = service.getItemLots(
                    query = "ItemNumber='%$searchQuery%'",
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
    @GET("fscmRestApi/resources/11.13.18.05/itemsV2")
    suspend fun getInventoryItemLots(
        @Query("onlyData") onlyData: Boolean = true,
        @Query("q") query: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("fields") fields: String = "ItemNumber,ItemDescription"
    ): Response<InventoryResponse>

    @GET("fscmRestApi/resources/11.13.18.05/inventoryItemLots")
    suspend fun getItemLots(
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
    @SerializedName("ItemNumber") val itemNumber: String?,
    @SerializedName("ItemDescription") val itemDescription: String?,
): Serializable


data class Lots(
    @SerializedName("LotNumber") val lotNumber: String?,
    @SerializedName("ItemNumber") val itemDescription: String?,
    @SerializedName("InventoryItemId") val quantity: String?
): Serializable