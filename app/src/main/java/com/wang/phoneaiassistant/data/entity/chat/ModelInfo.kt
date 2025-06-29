package com.wang.phoneaiassistant.data.entity.chat

import com.google.gson.annotations.SerializedName

data class ModelInfo(
    @SerializedName("id")
    val id: String,

    @SerializedName("object")
    val objectType: String, // 避免使用Kotlin关键字 object

    @SerializedName("owned_by")
    val ownedBy: String
)