package com.lazymohan.zebraprinter.app

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject

class AppPref @Inject constructor(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "my_app_prefs"
        private const val KEY_PERSON_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_LOGGED_IN = "is_logged_in"
        private const val KEY_CAN_DISCOVER = "can_discover"
    }

    var canDiscover: Boolean
        get() = preferences.getBoolean(KEY_CAN_DISCOVER, false)
        set(value) = preferences.edit { putBoolean(KEY_CAN_DISCOVER, value) }

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

    fun clear() {
        preferences.edit { clear() }
    }
}