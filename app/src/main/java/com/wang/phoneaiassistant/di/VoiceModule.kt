package com.wang.phoneaiassistant.di

import com.wang.phoneaiassistant.data.voice.IVoiceInputManager
import com.wang.phoneaiassistant.data.voice.VoiceInputManager
import com.wang.phoneaiassistant.data.voice.XfVoiceInputManager
import com.wang.phoneaiassistant.data.preferences.AppPreference
import com.wang.phoneaiassistant.data.voice.WhisperVoiceInputManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(SingletonComponent::class)
object VoiceModule {
    
    @Provides
    @Singleton
    fun provideVoiceInputManager(
        @ApplicationContext context: Context,
        appPreferences: AppPreference
    ): IVoiceInputManager {
        // 根据用户配置返回相应的语音识别服务
        return when (appPreferences.voiceProvider) {
            "google" -> VoiceInputManager()
            "whisper" -> WhisperVoiceInputManager(context)
            "xunfei" -> XfVoiceInputManager(appPreferences)
            else -> WhisperVoiceInputManager(context) // 默认使用Whisper
        }
    }
}