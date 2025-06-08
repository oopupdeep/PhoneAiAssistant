package com.wang.phoneaiassistant.data.network.entity

import com.google.gson.annotations.SerializedName

data class ModelListResponse(
    @SerializedName("object")
    val objectType: String,

    @SerializedName("data")
    val data: List<ModelInfo> // 关键点：这里包含了模型信息列表
)