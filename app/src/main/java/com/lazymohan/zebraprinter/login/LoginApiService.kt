package com.lazymohan.zebraprinter.login

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * No explicit @Header("Authorization") here. Your FusionApiClient should inject the
 * "Authorization: Bearer <token>" header automatically via an OkHttp interceptor.
 */
interface LoginApiService {
    @GET("hcmRestApi/resources/11.13.18.05/userAccounts")
    suspend fun getUserAccount(
        @Query("q") query: String
    ): UserResponse
}

data class UserResponse(val items: List<UserAccount>)
data class UserAccount(
    val UserId: String?,
    val Username: String?,
    val SuspendedFlag: String?,
    val PersonId: Long,
    val PersonNumber: String?,
    val CredentialsEmailSentFlag: String?,
    val GUID: String?,
    val CreatedBy: String?,
    val CreationDate: String?,
    val LastUpdatedBy: String?,
    val LastUpdateDate: String?
)
