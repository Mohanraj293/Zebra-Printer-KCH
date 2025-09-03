package com.lazymohan.zebraprinter.product.data

import android.util.Log
import com.google.gson.annotations.SerializedName
import com.lazymohan.zebraprinter.network.FusionApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.Serializable
import java.util.Date
import javax.inject.Inject

class ProductsRepo @Inject constructor(apiClient: FusionApiClient) {

    private val service = apiClient.retrofit.create(OracleApiService::class.java)

    suspend fun getProducts(
        searchQuery: String,
        selectedOrgId: Organisations?,
        limit: Int,
        offset: Int
    ): InventoryResponse? =
        withContext(Dispatchers.IO) {
            try {
                val response = service.getInventoryItemLots(
                    query = "ItemDescription LIKE '%$searchQuery%';OrganizationCode='${selectedOrgId?.organizationCode}'",
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

    suspend fun getLots(
        itemNum: String,
        limit: Int,
        offset: Int,
    ): LotsResponse? =
        withContext(Dispatchers.IO) {
            try {
                val response = service.getItemLots(
                    query = "ItemNumber='$itemNum'",
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

    suspend fun getGTINForItem(inventoryItemId: Long): GTINResponse? =
        withContext(Dispatchers.IO) {
            try {
                val response = service.getGTINForItem(
                    query = "InventoryItemId=$inventoryItemId"
                )
                if (response.isSuccessful && response.body()?.items?.isNotEmpty() == true) {
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

    suspend fun getOrganisations() =
        withContext(Dispatchers.IO) {
            try {
                val response = service.getOrganisations()
                if (response.isSuccessful && response.body()?.items?.isNotEmpty() == true) {
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
    ): Response<LotsResponse>

    @GET("fscmRestApi/resources/11.13.18.05/GTINRelationships")
    suspend fun getGTINForItem(
        @Query("q") query: String,
    ): Response<GTINResponse>

    @GET("fscmRestApi/resources/11.13.18.05/inventoryOrganizations")
    suspend fun getOrganisations(): Response<OrganisationResponse>
}

data class InventoryResponse(
    @SerializedName("items") val items: List<Item>,
    @SerializedName("count") val count: Int,
    @SerializedName("hasMore") val hasMore: Boolean,
    @SerializedName("limit") val limit: Int,
    @SerializedName("offset") val offset: Int
)

data class LotsResponse(
    @SerializedName("items") val items: List<Lots>,
    @SerializedName("count") val count: Int,
    @SerializedName("hasMore") val hasMore: Boolean,
    @SerializedName("limit") val limit: Int,
    @SerializedName("offset") val offset: Int
)

data class GTINResponse(
    @SerializedName("items") val items: List<GTIN>,
)

data class OrganisationResponse(
    @SerializedName("items") val items: List<Organisations>
)

data class GTIN(
    @SerializedName("GTIN") val gtin: String?,
    @SerializedName("InventoryItemId") val inventoryItemId: Long
)

data class Item(
    @SerializedName("ItemNumber") val itemNumber: String?,
    @SerializedName("ItemDescription") val itemDescription: String?,
) : Serializable


data class Lots(
    @SerializedName("LotNumber") val lotNumber: String?,
    @SerializedName("ItemDescription") val itemDescription: String?,
    @SerializedName("ItemNumber") val itemNumber: String?,
    @SerializedName("StatusCode") val statusCode: String?,
    @SerializedName("OriginationDate") val originationDate: Date?,
    @SerializedName("ExpirationDate") val expirationDate: Date?,
    @SerializedName("InventoryItemId") val inventoryItemId: Long
) : Serializable

data class Organisations(
    @SerializedName("OrganizationId") val organizationId: Long,
    @SerializedName("OrganizationCode") val organizationCode: String,
    @SerializedName("OrganizationName") val organizationName: String
)