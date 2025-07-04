package com.wang.phoneaiassistant.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString("base_url", "https://api.deepseek.com/v1/") ?: "https://api.deepseek.com/v1/"
        set(value) = prefs.edit { putString("base_url", value) }

    var apiKey: String
        get() = prefs.getString("api_key", "") ?: ""
        set(value) = prefs.edit { putString("api_key", value) }

    val defaultApiKey = "sk-668a4b63c3374bfd885bb08b3bc4dbc5"
}