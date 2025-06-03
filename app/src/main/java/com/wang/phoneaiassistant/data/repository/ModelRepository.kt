package com.wang.phoneaiassistant.data.repository

import com.wang.phoneaiassistant.data.network.entity.ModelInfo
import com.wang.phoneaiassistant.data.network.ModelService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ModelRepository(private val modelService: ModelService) {
    suspend fun getAvailableModels(): List<ModelInfo> = withContext(Dispatchers.IO) {
        try {
            modelService.getModels()
        } catch (e: Exception) {
            emptyList()
        }
    }
}