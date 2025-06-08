package com.wang.phoneaiassistant.data.network

import android.content.Context
import com.wang.phoneaiassistant.data.preferences.AppPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import com.wang.phoneaiassistant.BuildConfig// 确保导入你的BuildConfig

@Module
@InstallIn(SingletonComponent::class) // 整个应用的生命周期内共享实例
object NetworkModule {

    @Provides
    @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences {
        return AppPreferences(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(prefs: AppPreferences): OkHttpClient {
        // 1. 创建 HttpLoggingInterceptor
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // 2. 根据构建类型设置日志级别
            //    如果是 Debug 版本，则打印所有日志 (BODY)
            //    如果是 Release 版本，则不打印日志 (NONE)
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            // 添加用于认证的拦截器，它会在日志拦截器之前执行
            .addInterceptor { chain ->
                val apiKey = prefs.defaultApiKey // 从注入的AppPreferences获取
                val request = chain.request().newBuilder().apply {
                    if (!apiKey.isNullOrBlank()) {
                        addHeader("Authorization", "Bearer $apiKey")
                    }
                }.build()
                chain.proceed(request)
            }
            // 3. 将配置好的日志拦截器添加到 OkHttpClient
            //    建议将日志拦截器放在拦截器链的末尾，这样可以捕获前面所有拦截器对请求的修改
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, prefs: AppPreferences): Retrofit {
        // Retrofit 的 BaseUrl 也可能需要根据Debug/Release环境切换
        // 这里我们保持原样，仅作提醒
        val baseUrl = prefs.baseUrl
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideModelService(retrofit: Retrofit): ModelService {
        return retrofit.create(ModelService::class.java)
    }

    @Provides
    @Singleton
    fun provideChatService(retrofit: Retrofit): ChatService {
        return retrofit.create(ChatService::class.java)
    }
}