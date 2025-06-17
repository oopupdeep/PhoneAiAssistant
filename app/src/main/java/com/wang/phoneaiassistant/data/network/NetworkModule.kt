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
import java.util.concurrent.TimeUnit // 1. 导入 TimeUnit
import javax.inject.Singleton
import com.wang.phoneaiassistant.BuildConfig

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences {
        return AppPreferences(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(prefs: AppPreferences): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val apiKey = prefs.defaultApiKey
                val request = chain.request().newBuilder().apply {
                    if (!apiKey.isNullOrBlank()) {
                        addHeader("Authorization", "Bearer $apiKey")
                    }
                }.build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)

            // --- ✨ 添加的超时设置 ✨ ---
            // 连接超时：设置与服务器建立连接的最大时间
            .connectTimeout(20, TimeUnit.SECONDS)
            // 读取超时：成功连接后，等待服务器返回数据的最大时间。这是解决您问题的关键！
            .readTimeout(600, TimeUnit.SECONDS)
            // 写入超时：向服务器发送数据的最大时间
            .writeTimeout(20, TimeUnit.SECONDS)
            // --- ✨ 设置结束 ✨ ---

            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, prefs: AppPreferences): Retrofit {
        val baseUrl = prefs.baseUrl
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // ... (provideModelService 和 provideChatService 保持不变)
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
