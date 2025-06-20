package com.wang.phoneaiassistant.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 1. 定义你的亮色配色方案
private val LightColorScheme = lightColorScheme(
    primary = accentColor,
    onPrimary = Color.White,
    // secondary, tertiary 等都可以设置为 accentColor，以保持简约
    secondary = accentColor,
    onSecondary = Color.White,
    background = backgroundColor,
    onBackground = primaryTextColor,
    surface = surfaceColor,
    onSurface = primaryTextColor,
    // 用于辅助文本、禁用状态等的颜色
    onSurfaceVariant = secondaryTextColor,
    // 定义输入框等元素的轮廓颜色
    outline = Color(0xFFE0E0E0)
)

@Composable
fun PhoneAiAssistantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // 我们强制使用亮色主题
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb() // 状态栏颜色
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true // 状态栏图标和文字变暗
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // 引用同目录下的 Typography.kt
        content = content
    )
}