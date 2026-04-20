package com.qdev.pro

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var apiUrl: String
        get() = prefs.getString("api_url", "") ?: ""
        set(value) = prefs.edit().putString("api_url", value).apply()

    var apiToken: String
        get() = prefs.getString("api_token", "qd_secret_webhook_778899") ?: ""
        set(value) = prefs.edit().putString("api_token", value).apply()

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean("service_enabled", true)
        set(value) = prefs.edit().putBoolean("service_enabled", value).apply()
}
