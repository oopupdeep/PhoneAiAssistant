package com.wang.phoneaiassistant.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.wang.phoneaiassistant.data.Authenticate.CompanyManager
//import com.wang.phoneaiassistant.ui.chat.ChatScreen
import com.wang.phoneaiassistant.ui.chat.ChatScreenNew
import dagger.hilt.android.AndroidEntryPoint
import com.wang.phoneaiassistant.ui.theme.PhoneAiAssistantTheme
import com.wang.phoneaiassistant.ui.navigation.UnifiedAppNavigation
import javax.inject.Inject
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts

@AndroidEntryPoint
class ChatActivity : ComponentActivity() {

    @Inject
    lateinit var companyManager: CompanyManager
    
    // 使用新的权限请求API
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 权限已授予
        } else {
            // 权限被拒绝，可以显示提示
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化默认公司配置（只在首次启动时需要）
        companyManager.initDefaults()
        
        // 请求录音权限
        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

        setContent {
            PhoneAiAssistantTheme {
                UnifiedAppNavigation()
            }
        }
    }
}
