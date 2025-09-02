// app/src/main/java/com/lazymohan/zebraprinter/network/AccessTokenInterceptor.kt
package com.lazymohan.zebraprinter.network

import com.lazymohan.zebraprinter.app.AppPref
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Minimal interceptor that reads the access token from AppPref (saved by LoginViewModel)
 * and adds Authorization: Bearer <token> to every request.
 * No auto-refresh here — when token expires, the server will 401 and you can
 * handle re-login at the app level if needed.
 */
class AccessTokenInterceptor @Inject constructor(
    private val appPref: AppPref
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = appPref.accessToken
        val req = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(req)
    }
}
