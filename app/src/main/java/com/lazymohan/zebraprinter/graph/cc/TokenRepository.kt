package com.lazymohan.zebraprinter.graph.cc

import com.lazymohan.zebraprinter.BuildConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TokenRepository(private val api: TokenApi) {
    private val mutex = Mutex()
    @Volatile private var cachedToken: String? = null
    @Volatile private var expiryEpochSec: Long = 0L

    suspend fun getAccessToken(nowEpochSec: Long = System.currentTimeMillis() / 1000): String {
        return mutex.withLock {
            val token = cachedToken
            if (token != null && nowEpochSec < expiryEpochSec - 60) return token

            val resp = api.clientCredentialsToken(
                tenantId = BuildConfig.GRAPH_TENANT_ID,
                clientId = BuildConfig.GRAPH_CLIENT_ID,
                clientSecret = BuildConfig.GRAPH_CLIENT_SECRET,
                scope = BuildConfig.GRAPH_SCOPES
            )
            cachedToken = resp.accessToken
            expiryEpochSec = nowEpochSec + resp.expiresIn
            resp.accessToken
        }
    }
}
