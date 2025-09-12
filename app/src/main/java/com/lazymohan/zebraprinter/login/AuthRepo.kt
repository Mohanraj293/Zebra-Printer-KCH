package com.lazymohan.zebraprinter.login

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.lazymohan.zebraprinter.network.FusionApiClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepo @Inject constructor(
    apiClient: FusionApiClient
) {
    private val service: LoginApiService = apiClient.retrofit.create(LoginApiService::class.java)

    suspend fun fetchUserByUsername(username: String): Result<UserResponse> {
        val trimmed = username.trim()
        Log.d("AuthRepo", "fetchUserByUsername called with username=$trimmed")
        return runCatching {
            val response = service.getUserAccount(query = "advancedSearch;userID=$trimmed")
            Log.d("AuthRepo", "fetchUserByUsername SUCCESS → items=${response.items.size}")
            response
        }.onFailure { e ->
            Firebase.crashlytics.recordException(e)
            Log.e("AuthRepo", "fetchUserByUsername FAILED: ${e.message}", e)
        }
    }
}
