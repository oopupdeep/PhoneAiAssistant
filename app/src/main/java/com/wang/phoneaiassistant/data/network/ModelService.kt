package com.wang.phoneaiassistant.data.network

import com.wang.phoneaiassistant.data.entity.network.ModelListResponse
import retrofit2.http.GET

interface ModelService {
    @GET("models")
    suspend fun getModels(): ModelListResponse
}