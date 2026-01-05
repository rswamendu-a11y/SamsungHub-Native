package com.samsunghub.app.utils

import android.content.Context

object UserPrefs {
    private const val PREF_NAME = "AppPrefs"
    private const val KEY_OUTLET = "outlet_name"
    private const val KEY_SEC = "sec_name"

    fun getOutletName(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_OUTLET, "") ?: ""
    }

    fun getSecName(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SEC, "") ?: ""
    }

    fun saveOutletDetails(context: Context, outlet: String, sec: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_OUTLET, outlet)
            .putString(KEY_SEC, sec)
            .apply()
    }
}
