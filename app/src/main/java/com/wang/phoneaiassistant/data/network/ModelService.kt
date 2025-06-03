package com.wang.phoneaiassistant.data.network

import com.wang.phoneaiassistant.data.network.entity.ModelInfo
import retrofit2.http.GET

interface ModelService {
    @GET("models")
    suspend fun getModels(): List<ModelInfo>
}