package com.lazymohan.zebraprinter.di

import com.lazymohan.zebraprinter.BuildConfig
import com.lazymohan.zebraprinter.app.AppPref
import com.lazymohan.zebraprinter.grn.data.FusionApi
import com.lazymohan.zebraprinter.grn.data.GrnRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Authenticator
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FusionModule {

    @Provides
    @Singleton
    fun provideOkHttp(appPref: AppPref): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }

        // Adds Bearer header and tries proactive refresh if expired
        val bearerInterceptor = Interceptor { chain ->
//            // refresh if expired
//            if (appPref.isAccessTokenExpired()) {
//                tryRefreshTokenBlocking(appPref)
//            }

            val original = chain.request()
            val builder = original.newBuilder()
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")

            // Only attach Authorization if we have a valid token
            appPref.authHeaderOrNull()?.let { auth ->
                builder.header("Authorization", auth)
            }

            val response = chain.proceed(builder.build())

            // If Fusion says 401, *immediately* force a logout and kick to Login.
            if (response.code == 401) {
                response.close()
                appPref.forceLogoutToLogin()
                throw IOException("Unauthorized (401) — forced logout")
            }
            response
        }

//        // On 401, attempt to refresh once and retry
//        val refreshAuthenticator = object : Authenticator {
//            override fun authenticate(route: Route?, response: Response): Request? {
//                // Avoid infinite loops: if we've already tried with a new token, don't loop
//                val priorAuth = response.request.header("Authorization")
//                val refreshed = tryRefreshTokenBlocking(appPref)
//                val newAuth = appPref.authHeaderOrNull()
//
//                return if (refreshed && newAuth != null && newAuth != priorAuth) {
//                    response.request.newBuilder()
//                        .header("Authorization", newAuth)
//                        .build()
//                } else {
//                    null
//                }
//            }
//        }


        return OkHttpClient.Builder()
            .addInterceptor(bearerInterceptor)
            .addInterceptor(logging)
//            .authenticator(refreshAuthenticator)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.FUSION_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideFusionApi(retrofit: Retrofit): FusionApi =
        retrofit.create(FusionApi::class.java)

    @Provides
    @Singleton
    fun provideGrnRepository(api: FusionApi): GrnRepository =
        GrnRepository(api)
//
//    /**
//     * === Helper: refresh token synchronously ===
//     *
//     * Uses BuildConfig (IDCS endpoints + client) and AppPref refresh token to get a new access token.
//     * Returns true if we updated tokens successfully.
//     */
//    private fun tryRefreshTokenBlocking(appPref: AppPref): Boolean {
//        val refresh = appPref.refreshToken ?: return false
//        val tokenEndpoint = BuildConfig.IDCS_BASE_URL.trimEnd('/') + BuildConfig.TOKEN_ENDPOINT
//
//        // Build x-www-form-urlencoded body
//        val form = FormBody.Builder()
//            .add("grant_type", "refresh_token")
//            .add("refresh_token", refresh)
//            .add("client_id", BuildConfig.OAUTH_CLIENT_ID)
//            .apply {
//                // If you must use a secret (not recommended for mobile), include it
//                val secret = BuildConfig.OAUTH_CLIENT_SECRET
//                if (secret.isNotBlank()) {
//                    add("client_secret", secret)
//                }
//                // Scope optional on refresh; include only if your IDCS requires it
//                val scope = BuildConfig.OAUTH_SCOPE
//                if (scope.isNotBlank()) {
//                    add("scope", scope)
//                }
//            }
//            .build()
//
//        // Use a short-lived client for the refresh call (no interceptors)
//        val client = OkHttpClient.Builder()
//            .connectTimeout(15, TimeUnit.SECONDS)
//            .readTimeout(20, TimeUnit.SECONDS)
//            .build()
//
//        val request = Request.Builder()
//            .url(tokenEndpoint)
//            .post(form)
//            .header("Accept", "application/json")
//            .header("Content-Type", "application/x-www-form-urlencoded")
//            .build()
//
//        return try {
//            client.newCall(request).execute().use { resp ->
//                if (!resp.isSuccessful) return false
//                val bodyStr = resp.body.string()
//
//                val json = JSONObject(bodyStr)
//                val accessToken = json.optString("access_token", null)
//                if (accessToken.isNullOrEmpty()) return false
//
//                val tokenType = json.optString("token_type", "Bearer")
//                val expiresInSec = json.optLong("expires_in", 3600L)
//                val newRefresh = json.optString("refresh_token", refresh) // may or may not come back
//                val scope = json.optString("scope", null)
//                val idToken = json.optString("id_token", null)
//                val expiresAtMillis = System.currentTimeMillis() + (expiresInSec * 1000L)
//
//                appPref.saveTokens(
//                    accessToken = accessToken,
//                    refreshToken = newRefresh,
//                    expiresAtMillis = expiresAtMillis,
//                    tokenType = tokenType,
//                    scope = scope,
//                    idToken = idToken
//                )
//                true
//            }
//        } catch (_: IOException) {
//            false
//        } catch (_: Exception) {
//            false
//        }
//    }
}
