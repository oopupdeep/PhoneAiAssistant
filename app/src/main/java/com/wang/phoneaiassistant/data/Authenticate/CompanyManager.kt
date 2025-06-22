package com.wang.phoneaiassistant.data.Authenticate

import android.content.Context
import com.wang.phoneaiassistant.data.preferences.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class CompanyManager @Inject constructor(
    @ApplicationContext context: Context,
    private val appPreferences: AppPreferences
) {
    private val apiPrefs = context.getSharedPreferences("company_api_keys", Context.MODE_PRIVATE)
    private val urlPrefs = context.getSharedPreferences("company_base_urls", Context.MODE_PRIVATE)

    // 设置当前使用的公司
    fun setCompany(company: String) {
        appPreferences.baseUrl = getUrl(company) ?: ""
        appPreferences.apiKey = getApiKey(company) ?: ""
    }

    fun getCompanyNames(): List<String> {
        return urlPrefs.all.keys.toList()
    }

    // 初始化默认公司配置
    fun initDefaults() {
        saveUrl("DeepSeek", "https://api.deepseek.com/v1/")
        saveUrl("Qwen", "https://dashscope.aliyuncs.com/compatible-mode/v1")
        saveApiKey("DeepSeek", appPreferences.defaultApiKey)
        setCompany("DeepSeek")
    }

    // ↓↓↓ 底层数据访问 ↓↓↓

    fun saveApiKey(company: String, key: String) {
        apiPrefs.edit().putString(company, key).apply()
    }

    fun getApiKey(company: String): String? {
        return apiPrefs.getString(company, null)
    }

    fun saveUrl(company: String, url: String) {
        urlPrefs.edit().putString(company, url).apply()
    }

    fun getUrl(company: String): String? {
        return urlPrefs.getString(company, null)
    }

    fun deleteCompany(company: String) {
        apiPrefs.edit().remove(company).apply()
        urlPrefs.edit().remove(company).apply()
    }
}
