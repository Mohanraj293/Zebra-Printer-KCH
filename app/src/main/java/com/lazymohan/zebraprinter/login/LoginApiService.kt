package com.lazymohan.zebraprinter.login

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * No explicit @Header("Authorization") here. Your FusionApiClient should inject the
 * "Authorization: Bearer <token>" header automatically via an OkHttp interceptor.
 */
interface LoginApiService {
    // Switched from: hcmRestApi/.../userAccounts?q=...
    // To:           fscmRestApi/.../workflowUsers?finder=advancedSearch;userID=<id>&onlyData=true
    @GET("fscmRestApi/resources/11.13.18.05/workflowUsers")
    suspend fun getUserAccount(
        @Query("finder") query: String,
        @Query("onlyData") onlyData: Boolean = true
    ): UserResponse
}

data class UserResponse(
    @SerializedName("items") val items: List<UserAccount> = emptyList(),
    @SerializedName("count") val count: Int? = null,
    @SerializedName("hasMore") val hasMore: Boolean? = null,
    @SerializedName("limit") val limit: Int? = null,
    @SerializedName("offset") val offset: Int? = null
)

data class UserAccount(
    @SerializedName("Username") val Username: String? = null,
    @SerializedName("FirstName") val FirstName: String? = null,
    @SerializedName("LastName") val LastName: String? = null,
    @SerializedName("EmailAddress") val EmailAddress: String? = null,
    @SerializedName("ManagerName") val ManagerName: String? = null,
    @SerializedName("WorkflowName") val WorkflowName: String? = null,
    @SerializedName("TransactionIdentifier") val TransactionIdentifier: String? = null,
    @SerializedName("LoggedInUser") val LoggedInUser: String? = null,
    @SerializedName("PersonId") private val _PersonId: Any? = null,
    @SerializedName("UserId") val UserId: String? = null,
    @SerializedName("SuspendedFlag") val SuspendedFlag: String? = null,
    @SerializedName("PersonNumber") val PersonNumber: String? = null,
    @SerializedName("CredentialsEmailSentFlag") val CredentialsEmailSentFlag: String? = null,
    @SerializedName("GUID") val GUID: String? = null,
    @SerializedName("CreatedBy") val CreatedBy: String? = null,
    @SerializedName("CreationDate") val CreationDate: String? = null,
    @SerializedName("LastUpdatedBy") val LastUpdatedBy: String? = null,
    @SerializedName("LastUpdateDate") val LastUpdateDate: String? = null
) {
    val PersonId: Long
        get() = when (_PersonId) {
            is Number -> _PersonId.toLong()
            is String -> _PersonId.toLongOrNull() ?: 0L
            else      -> 0L
        }
}
