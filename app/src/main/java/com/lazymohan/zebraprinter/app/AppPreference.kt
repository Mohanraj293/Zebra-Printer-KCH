package com.lazymohan.zebraprinter.app

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object AppPref {

    private const val PREF_NAME = "my_app_prefs"
    private lateinit var preferences: SharedPreferences

    private const val KEY_CAN_DISCOVER= "can_discover"

    fun init(context: Context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    var canDiscover: Boolean
        get() = preferences.getBoolean(KEY_CAN_DISCOVER, false) // default = false
        set(value) = preferences.edit { putBoolean(KEY_CAN_DISCOVER, value) }

    fun clear() {
        preferences.edit { clear() }
    }
}