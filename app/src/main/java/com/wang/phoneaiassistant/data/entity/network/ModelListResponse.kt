package com.wang.phoneaiassistant.data.entity.network

import com.google.gson.annotations.SerializedName
import com.wang.phoneaiassistant.data.entity.chat.ModelInfo

data class ModelListResponse(
    @SerializedName("object")
    val objectType: String,

    @SerializedName("data")
    val data: List<ModelInfo> // 关键点：这里包含了模型信息列表
)