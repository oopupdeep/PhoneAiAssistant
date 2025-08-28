package com.wang.phoneaiassistant.ui.chat.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun VoiceInputAnimation(
    soundLevel: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    // 创建动画效果
    val infiniteTransition = rememberInfiniteTransition()
    
    // 基础波动动画
    val waveAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Box(
        modifier = modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            
            // 绘制3个圆圈，根据声音大小和动画进行缩放
            for (i in 0..2) {
                val delay = i * 0.3f
                val animationProgress = (waveAnimation + delay) % 1f
                val radius = size.minDimension / 4 + 
                    (size.minDimension / 3) * animationProgress +
                    (soundLevel.coerceIn(0f, 10f) / 10f) * (size.minDimension / 6)
                val alpha = (1f - animationProgress) * 0.5f
                
                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(centerX, centerY)
                )
            }
            
            // 中心圆圈，根据声音大小变化
            val centerRadius = size.minDimension / 6 + 
                (soundLevel.coerceIn(0f, 10f) / 10f) * (size.minDimension / 12)
            drawCircle(
                color = color,
                radius = centerRadius,
                center = Offset(centerX, centerY)
            )
        }
    }
}