package com.wang.phoneaiassistant.ui


import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import android.webkit.WebView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun ShimmerText(text: String, modifier: Modifier = Modifier) {
    // 1. 定义 Shimmer 效果的颜色列表
    val shimmerColors = listOf(
        Color.Gray.copy(alpha = 0.8f), // 较深的灰色
        Color.Gray.copy(alpha = 0.4f), // 亮灰色（光带）
        Color.Gray.copy(alpha = 0.8f)  // 较深的灰色
    )

    // 2. 创建一个无限循环的过渡动画
    val transition = rememberInfiniteTransition(label = "ShimmerTransition")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1500f, // 动画移动的目标距离，要足够大以覆盖文本
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500, // 动画持续时间
                easing = FastOutSlowInEasing // 动画速度曲线
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerTextAnimation"
    )

    // 3. 创建一个线性渐变笔刷，并用动画驱动它
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnimation.value, y = translateAnimation.value)
    )

    // 4. 将笔刷应用到 Text 的 style 上
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(
            brush = brush,
            fontSize = 16.sp,   // 确保字体大小等样式与原 Text 一致
            lineHeight = 20.sp
        )
    )
}

@Composable
fun LatexView(
    latex: String,
    modifier: Modifier = Modifier
) {
    // 获取当前主题的文本颜色，以便 WebView 中的文本颜色与 App 统一
    val textColor = MaterialTheme.colorScheme.onSurface

    // 将 Compose Color 转换为 CSS 使用的十六进制颜色字符串 (e.g., #FFFFFF)
    val textColorHex = String.format("#%06X", (0xFFFFFF and textColor.toArgb()))

    // 使用 AndroidView 来嵌入传统的 Android View
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                // 基本配置
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                // 设置背景透明，使其与 Compose 的背景融合
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = { webView ->
            // 每当 latex 文本或颜色更新时，重新加载数据

            // 关键：对 LaTeX 字符串中的特殊字符进行转义，以便在 JS 字符串中安全使用
            val escapedLatex = latex.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "")

            val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.css" integrity="sha384-n8MVd4RsNIU0KOVEMcAgsUFkSSJNECEHFLAn4UNsCNIILH48AQoJxpEkzzBhOVRq" crossorigin="anonymous">
                <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.js" integrity="sha384-XjKyOOlGwcjNTAIQHIpgOno0Hl1YQqzUOEleOLALmuqehneUG+vnGohHnVvGIlwQ" crossorigin="anonymous"></script>
                <style>
                    body {
                        margin: 0;
                        padding: 0;
                        background-color: transparent; /* 背景透明 */
                        color: $textColorHex; /* 动态设置文本颜色 */
                        font-family: sans-serif;
                    }
                    .katex {
                        font-size: 1.1em; /* 稍微放大公式，使其更清晰 */
                    }
                </style>
            </head>
            <body>
                <div id="content"></div>
                <script>
                    // KaTeX 会自动查找并渲染带有特定分隔符的数学公式
                    katex.render("$escapedLatex", document.getElementById('content'), {
                        throwOnError: false, // 出错时不抛出异常，而是显示原始文本
                        displayMode: true,   // 使用显示模式渲染，公式会居中并有独立行
                        delimiters: [        // KaTeX 将在这些分隔符内查找公式
                            {left: "$$", right: "$$", display: true},
                            {left: "\\[", right: "\\]", display: true},
                            {left: "$", right: "$", display: false},
                            {left: "\\(", right: "\\)", display: false}
                        ]
                    });
                </script>
            </body>
            </html>
            """.trimIndent()

            // 加载我们动态生成的 HTML
            // 使用 aplication/x-www-form-urlencoded 是为了更好地处理UTF-8编码
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        },
        modifier = modifier
    )
}