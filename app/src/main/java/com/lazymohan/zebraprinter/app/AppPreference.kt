package com.lazymohan.zebraprinter.app

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.edit
import com.lazymohan.zebraprinter.login.LoginActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Includes OAuth token storage (access/refresh/expiry/type/scope/idToken).
 * NOTE: Consider EncryptedSharedPreferences for production.
 */
class AppPref @Inject constructor(
    @ApplicationContext context: Context
) {

    private val appContext: Context = context.applicationContext

    private val preferences: SharedPreferences =
        appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "my_app_prefs"

        // User/session
        private const val KEY_PERSON_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_LOGGED_IN = "is_logged_in"
        private const val KEY_CAN_DISCOVER = "can_discover"

        // OAuth tokens
        private const val KEY_ACCESS_TOKEN = "oauth_access_token"
        private const val KEY_REFRESH_TOKEN = "oauth_refresh_token"
        private const val KEY_TOKEN_EXPIRY = "oauth_token_expiry"
        private const val KEY_TOKEN_TYPE = "oauth_token_type"
        private const val KEY_SCOPE = "oauth_scope"
        private const val KEY_ID_TOKEN = "oauth_id_token"

        // >>> used when we hard-redirect to Login <<<
        const val EXTRA_LOGOUT_REASON = "extra_logout_reason"
        const val LOGOUT_REASON_401 = "SESSION_EXPIRED_401"
        const val EXTRA_LOGOUT_MESSAGE = "extra_logout_message"
    }

    // ---------- Feature flags / misc ----------
    var canDiscover: Boolean
        get() = preferences.getBoolean(KEY_CAN_DISCOVER, false)
        set(value) = preferences.edit { putBoolean(KEY_CAN_DISCOVER, value) }

    // ---------- Login / Profile ----------
    var isLoggedIn: Boolean
        get() = preferences.getBoolean(KEY_LOGGED_IN, false)
        set(value) = preferences.edit { putBoolean(KEY_LOGGED_IN, value) }

    var personId: Long
        get() = preferences.getLong(KEY_PERSON_ID, 0L)
        set(value) = preferences.edit { putLong(KEY_PERSON_ID, value) }

    var username: String?
        get() = preferences.getString(KEY_USERNAME, null)
        set(value) = preferences.edit { putString(KEY_USERNAME, value) }

    fun saveUser(personId: Long, username: String) {
        preferences.edit {
            putLong(KEY_PERSON_ID, personId)
            putString(KEY_USERNAME, username)
            putBoolean(KEY_LOGGED_IN, true)
        }
    }

    fun clearUser() {
        preferences.edit {
            remove(KEY_PERSON_ID)
            remove(KEY_USERNAME)
            putBoolean(KEY_LOGGED_IN, false)
        }
    }

    // ---------- OAuth: Accessors ----------
    var accessToken: String?
        get() = preferences.getString(KEY_ACCESS_TOKEN, null)
        set(value) = preferences.edit { putString(KEY_ACCESS_TOKEN, value) }

    var refreshToken: String?
        get() = preferences.getString(KEY_REFRESH_TOKEN, null)
        set(value) = preferences.edit { putString(KEY_REFRESH_TOKEN, value) }

    var tokenType: String
        get() = preferences.getString(KEY_TOKEN_TYPE, "Bearer") ?: "Bearer"
        set(value) = preferences.edit { putString(KEY_TOKEN_TYPE, value) }

    var tokenExpiryMillis: Long
        get() = preferences.getLong(KEY_TOKEN_EXPIRY, 0L)
        set(value) = preferences.edit { putLong(KEY_TOKEN_EXPIRY, value) }

    var scope: String?
        get() = preferences.getString(KEY_SCOPE, null)
        set(value) = preferences.edit { putString(KEY_SCOPE, value) }

    var idToken: String?
        get() = preferences.getString(KEY_ID_TOKEN, null)
        set(value) = preferences.edit { putString(KEY_ID_TOKEN, value) }

    /**
     * Save all tokens at once after a successful OAuth exchange.
     */
    fun saveTokens(
        accessToken: String,
        refreshToken: String? = null,
        expiresAtMillis: Long? = null,
        tokenType: String = "Bearer",
        scope: String? = null,
        idToken: String? = null
    ) {
        preferences.edit {
            putString(KEY_ACCESS_TOKEN, accessToken)
            if (!refreshToken.isNullOrEmpty()) putString(KEY_REFRESH_TOKEN, refreshToken)
            if (expiresAtMillis != null) putLong(KEY_TOKEN_EXPIRY, expiresAtMillis)
            putString(KEY_TOKEN_TYPE, tokenType)
            if (!scope.isNullOrEmpty()) putString(KEY_SCOPE, scope)
            if (!idToken.isNullOrEmpty()) putString(KEY_ID_TOKEN, idToken)
        }
    }

    /**
     * Convenience helpers used by ViewModel.
     */
    fun saveAccessToken(token: String) {
        accessToken = token
    }

    fun saveRefreshToken(token: String) {
        refreshToken = token
    }

    fun saveTokenExpiry(expiresAtMillis: Long) {
        tokenExpiryMillis = expiresAtMillis
    }

    /**
     * Returns true if the access token is considered expired.
     * @param leewaySeconds How many seconds early to treat the token as expired (clock skew buffer).
     */
    fun isAccessTokenExpired(leewaySeconds: Long = 60): Boolean {
        val expiry = tokenExpiryMillis
        if (expiry <= 0L) return true
        val now = System.currentTimeMillis()
        val leewayMs = leewaySeconds * 1000
        return now >= (expiry - leewayMs)
    }

    /**
     * Builds "Authorization" header value or null if missing/expired.
     */
    fun authHeaderOrNull(leewaySeconds: Long = 60): String? {
        val token = accessToken ?: return null
        return if (!isAccessTokenExpired(leewaySeconds)) {
            "${tokenType.trim()} $token"
        } else {
            null
        }
    }

    /** Clear only OAuth tokens */
    fun clearTokens() {
        preferences.edit {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_TOKEN_EXPIRY)
            remove(KEY_TOKEN_TYPE)
            remove(KEY_SCOPE)
            remove(KEY_ID_TOKEN)
        }
    }

    /** Full logout: clears user and tokens. */
    fun logout() {
        clearTokens()
        clearUser()
    }

    /**
     * 🚪 Hard-kick to Login screen with a reason/message:
     * - Clears tokens & user
     * - Starts LoginActivity as a fresh task (clears back stack)
     * Safe to call from any thread (uses application context + NEW_TASK).
     */
    fun forceLogoutToLogin(
        reason: String = LOGOUT_REASON_401,
        message: String = "Session expired. Please sign in again."
    ) {
        // Idempotent logout
        logout()
        val intent = Intent(appContext, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(EXTRA_LOGOUT_REASON, reason)
            putExtra(EXTRA_LOGOUT_MESSAGE, message)
        }
        appContext.startActivity(intent)
    }

    fun clear() {
        preferences.edit { clear() }
    }
}