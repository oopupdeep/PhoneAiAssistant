package com.wang.phoneaiassistant.di

import android.content.Context
import com.wang.phoneaiassistant.data.embeddings.EmbeddingService
import com.wang.phoneaiassistant.data.agent.ContextMemoryAgent
import com.wang.phoneaiassistant.data.repository.EmbeddingRepository
import com.wang.phoneaiassistant.data.repository.ConversationRepository
import com.wang.phoneaiassistant.data.database.MessageEmbeddingDao
import com.wang.phoneaiassistant.data.database.MessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EmbeddingModule {
    
    @Provides
    @Singleton
    fun provideEmbeddingService(@ApplicationContext context: Context): EmbeddingService {
        return EmbeddingService(context)
    }
    
    @Provides
    @Singleton
    fun provideEmbeddingRepository(
        messageEmbeddingDao: MessageEmbeddingDao,
        messageDao: MessageDao,
        embeddingService: EmbeddingService
    ): EmbeddingRepository {
        return EmbeddingRepository(messageEmbeddingDao, messageDao, embeddingService)
    }
    
    @Provides
    @Singleton
    fun provideContextMemoryAgent(
        embeddingRepository: EmbeddingRepository,
        conversationRepository: ConversationRepository
    ): ContextMemoryAgent {
        return ContextMemoryAgent(embeddingRepository, conversationRepository)
    }
}