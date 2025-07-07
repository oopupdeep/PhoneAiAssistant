package com.wang.phoneaiassistant.di

import android.content.Context
import com.wang.phoneaiassistant.data.ChatModeManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChatModeModule {
    
    @Provides
    @Singleton
    fun provideChatModeManager(
        @ApplicationContext context: Context,
        companyManager: com.wang.phoneaiassistant.data.Authenticate.CompanyManager
    ): ChatModeManager {
        return ChatModeManager(context, companyManager)
    }
}