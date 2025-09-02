// app/src/main/java/com/lazymohan/zebraprinter/login/AuthRepo.kt
package com.lazymohan.zebraprinter.login

import android.util.Log
import com.lazymohan.zebraprinter.network.FusionApiClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepo @Inject constructor(
    apiClient: FusionApiClient
) {
    private val service: LoginApiService = apiClient.retrofit.create(LoginApiService::class.java)

    suspend fun fetchUserByUsername(username: String): Result<UserResponse> {
        Log.d("AuthRepo", "fetchUserByUsername called with username=$username")
        return runCatching {
            val response = service.getUserAccount(query = "Username=\"$username\"")
            Log.d("AuthRepo", "fetchUserByUsername SUCCESS → items=${response.items.size}")
            response
        }.onFailure { e ->
            Log.e("AuthRepo", "fetchUserByUsername FAILED: ${e.message}", e)
        }
    }
}
