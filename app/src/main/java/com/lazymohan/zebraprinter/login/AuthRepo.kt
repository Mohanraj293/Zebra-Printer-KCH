package com.lazymohan.zebraprinter.login

import android.util.Base64
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

object AuthRepo {
    suspend fun login(username: String, password: String): Result<UserResponse> {
        return try {
            val credentials = "$username:$password"
            val authHeader =
                "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
            val response = LoginApiClient.service.getUserAccount(
                authHeader = authHeader,
                query = "Username=\"$username\""
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

object LoginApiClient {
    private const val BASE_URL = "https://effb-test.fa.em3.oraclecloud.com/"

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: LoginApiService = retrofit.create(LoginApiService::class.java)
}

interface LoginApiService {
    @GET("hcmRestApi/resources/11.13.18.05/userAccounts")
    suspend fun getUserAccount(
        @Header("Authorization") authHeader: String,
        @Query("q") query: String
    ): UserResponse
}

data class UserResponse(
    @SerializedName("items") val items: List<UserAccount>
)

data class UserAccount(
    @SerializedName("UserId") val userId: String?,
    @SerializedName("Username") val username: String?,
    @SerializedName("SuspendedFlag") val suspendedFlag: String?,
    @SerializedName("PersonId") val personId: String?,
    @SerializedName("PersonNumber") val personNumber: String?,
    @SerializedName("CredentialsEmailSentFlag") val credentialsEmailSentFlag: String?,
    @SerializedName("GUID") val guid: String?,
    @SerializedName("CreatedBy") val createdBy: String?,
    @SerializedName("CreationDate") val creationDate: String?,
    @SerializedName("LastUpdatedBy") val lastUpdatedBy: String?,
    @SerializedName("LastUpdateDate") val lastUpdateDate: String?
)