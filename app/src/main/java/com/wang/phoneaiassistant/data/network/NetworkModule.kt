package com.wang.phoneaiassistant.data.network

import android.content.Context
import com.wang.phoneaiassistant.data.preferences.AppPreference
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import com.wang.phoneaiassistant.BuildConfig

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreference {
        return AppPreference(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(prefs: AppPreference): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            // 拦截器1：动态设置 BaseUrl (已修正)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val latestBaseUrl = prefs.baseUrl // 例如 "https://api.deepseek.com/v1/"

                val newHttpUrl = latestBaseUrl.toHttpUrlOrNull()

                if (newHttpUrl != null) {
                    // 1. 从新的baseUrl（包含/v1/路径）创建Builder
                    val newUrlBuilder = newHttpUrl.newBuilder()

                    // 2. 将原始请求的路径（如 "chat/completions"）追加到新Builder的路径后面
                    //    originalRequest.url.encodedPath 是 "/chat/completions"，我们去掉开头的"/"
                    newUrlBuilder.addEncodedPathSegments(originalRequest.url.encodedPath.removePrefix("/"))

                    // 3. 如果原始请求有查询参数，也一并追加
                    if (originalRequest.url.query != null) {
                        newUrlBuilder.encodedQuery(originalRequest.url.encodedQuery)
                    }

                    // 4. 构建出最终的、正确的URL
                    val finalUrl = newUrlBuilder.build()

                    // 使用最终的URL构建新请求
                    val newRequest = originalRequest.newBuilder()
                        .url(finalUrl)
                        .build()

                    return@addInterceptor chain.proceed(newRequest)
                }

                chain.proceed(originalRequest)
            }
            // 拦截器2：添加认证头
            .addInterceptor { chain ->
                val apiKey = prefs.apiKey
                val request = chain.request().newBuilder().apply {
                    if (!apiKey.isNullOrBlank()) {
                        addHeader("Authorization", "Bearer $apiKey")
                    }
                }.build()
                chain.proceed(request)
            }
            // 拦截器3：日志
            .addInterceptor(loggingInterceptor)
            // 超时设置
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(600, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        // BaseUrl 仍然是一个占位符
        val placeholderBaseUrl = "http://localhost/"

        return Retrofit.Builder()
            .baseUrl(placeholderBaseUrl)
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