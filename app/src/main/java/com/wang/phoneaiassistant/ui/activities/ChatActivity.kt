package com.wang.phoneaiassistant.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.wang.phoneaiassistant.data.Authenticate.CompanyManager
//import com.wang.phoneaiassistant.ui.chat.ChatScreen
import com.wang.phoneaiassistant.ui.chat.ChatScreenNew
import dagger.hilt.android.AndroidEntryPoint
import com.wang.phoneaiassistant.ui.theme.PhoneAiAssistantTheme
import javax.inject.Inject

@AndroidEntryPoint
class ChatActivity : ComponentActivity() {

    @Inject
    lateinit var companyManager: CompanyManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化默认公司配置（只在首次启动时需要）
        companyManager.initDefaults()

        setContent {
            PhoneAiAssistantTheme {
                ChatScreenNew()
            }
        }
    }
}
