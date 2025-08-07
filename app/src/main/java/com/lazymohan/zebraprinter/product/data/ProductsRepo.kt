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

class ProductsRepo {

    suspend fun getProducts(searchQuery: String): List<Item> = withContext(Dispatchers.IO) {
        val service = ApiClient.create()
        try {
            val response = service.getInventoryItemLots(query = "ItemDescription LIKE '%$searchQuery%'")
            if (response.isSuccessful) {
                response.body()?.items.orEmpty()
            } else {
                Log.e("API", "Error: ${response.errorBody()?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("API", "Exception: ${e.localizedMessage}", e)
            emptyList()
        }
    }
}

interface OracleApiService {
    @GET("fscmRestApi/resources/11.13.18.05/inventoryItemLots")
    suspend fun getInventoryItemLots(
        @Query("onlyData") onlyData: Boolean = true,
        @Query("q") query: String
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
    @SerializedName("items") val items: List<Item>
)

data class Item(
    @SerializedName("ItemNumber") val itemNum: String,
)