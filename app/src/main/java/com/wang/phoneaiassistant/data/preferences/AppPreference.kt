package com.wang.phoneaiassistant.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class AppPreference(context: Context) {

    val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString("base_url", "https://api.deepseek.com/v1/") ?: "https://api.deepseek.com/v1/"
        set(value) = prefs.edit { putString("base_url", value) }

    var apiKey: String
        get() = prefs.getString("api_key", "") ?: ""
        set(value) = prefs.edit { putString("api_key", value) }

    var chatBackgroundUri: String?
        get() = prefs.getString("chat_background_uri", null)
        set(value) = prefs.edit { putString("chat_background_uri", value) }
    
    var contextMemoryEnabled: Boolean
        get() = prefs.getBoolean("context_memory_enabled", true)
        set(value) = prefs.edit { putBoolean("context_memory_enabled", value) }
}