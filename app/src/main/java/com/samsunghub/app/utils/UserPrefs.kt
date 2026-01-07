package com.samsunghub.app.utils

import android.content.Context
import android.content.SharedPreferences

object UserPrefs {
    private const val PREF_NAME = "SamsungHubPrefs"
    private const val KEY_OUTLET_NAME = "outlet_name"
    private const val KEY_SEC_NAME = "sec_name"
    private const val KEY_PIN = "user_pin"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveOutletDetails(context: Context, name: String, sec: String) {
        getPrefs(context).edit().putString(KEY_OUTLET_NAME, name).putString(KEY_SEC_NAME, sec).apply()
    }

    fun getOutletName(context: Context): String = getPrefs(context).getString(KEY_OUTLET_NAME, "") ?: ""
    fun getSecName(context: Context): String = getPrefs(context).getString(KEY_SEC_NAME, "") ?: ""

    // PIN Logic
    fun savePin(context: Context, pin: String) {
        getPrefs(context).edit().putString(KEY_PIN, pin).apply()
    }

    fun getPin(context: Context): String? = getPrefs(context).getString(KEY_PIN, null)

    fun isPinSet(context: Context): Boolean = getPin(context) != null
}
