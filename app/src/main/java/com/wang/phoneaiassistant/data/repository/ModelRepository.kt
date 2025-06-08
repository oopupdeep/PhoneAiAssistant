package com.wang.phoneaiassistant.data.repository

import com.wang.phoneaiassistant.data.network.entity.ModelInfo
import com.wang.phoneaiassistant.data.network.ModelService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // 确保Repository是单例，由Hilt管理
class ModelRepository @Inject constructor(private val modelService: ModelService) {

    /**
     * 从远程服务获取可用的模型列表。
     * 此方法已处理线程切换和基本错误。
     * @return 模型信息列表，如果失败则返回空列表。
     */
    suspend fun getAvailableModels(): List<ModelInfo> = withContext(Dispatchers.IO) {
        try {
            // 1. 调用 modelService.getModels() 会返回一个 ModelListResponse 对象
            val response = modelService.getModels()
            // 2. 从该响应对象中，通过 .data 属性提取出真正的 List<ModelInfo>
            response.data
        } catch (e: Exception) {
            // 在实际项目中，这里最好记录一下日志 e
            // Log.e("ModelRepository", "Failed to fetch models", e)

            // 你的注释很棒：向上抛出自定义异常是更稳健的做法，
            // 这里我们暂时保持返回空列表的逻辑。
            emptyList()
        }
    }
}