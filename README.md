# PhoneAiAssistant 📱🤖

一款功能强大的 Android AI 聊天助手应用，支持多种 AI 服务提供商，提供原生 API 和 WebView 双模式访问。

<p align="center">
  <img src="https://img.shields.io/badge/Android-14%2B-green.svg" alt="Android 14+">
  <img src="https://img.shields.io/badge/Kotlin-1.9.0-blue.svg" alt="Kotlin">
  <img src="https://img.shields.io/badge/Compose-1.5.0-brightgreen.svg" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="MIT License">
</p>

## ✨ 核心特色

### 🔄 双模式架构
- **API 模式**：使用个人 API Key 直接调用 AI 服务，享受原生体验
- **WebView 模式**：无需 API Key，通过内嵌网页即可使用 AI 服务
- 一键切换，灵活选择最适合的使用方式

### 🎨 个性化体验
- **自定义聊天背景**：从相册选择喜欢的图片作为聊天背景（API 模式专享）
- **Material Design 3**：现代化的界面设计，流畅的动画效果
- **深浅色主题**：自动适配系统主题设置

### 💬 智能对话管理
- **对话历史保存**：所有聊天记录本地持久化存储
- **智能标题生成**：AI 自动为每个对话生成简洁标题
- **全文搜索**：快速查找历史对话中的任何内容
- **实时流式响应**：逐字显示 AI 回复，提供打字机效果

## 🚀 主要功能

### 1. 多服务商支持
- ✅ DeepSeek（深度求索）
- ✅ Alibaba Qwen（通义千问）
- 🔜 更多服务商持续接入中...

### 2. 完整的聊天功能
- 流式对话响应，实时查看 AI 思考过程
- Markdown 渲染支持，完美显示代码、列表等格式
- 消息气泡区分用户和 AI，界面清晰直观
- 自动滚动到最新消息

### 3. 便捷的管理功能
- 侧滑抽屉导航，快速切换对话
- 一键新建对话
- 搜索历史消息
- 设置页面集中管理 API Key 和偏好设置

## 🛠️ 技术栈

- **开发语言**：Kotlin
- **UI 框架**：Jetpack Compose
- **架构模式**：MVVM + Clean Architecture
- **依赖注入**：Dagger Hilt
- **网络请求**：Retrofit 2 + OkHttp
- **数据库**：Room
- **异步处理**：Kotlin Coroutines + Flow
- **图片加载**：Coil
- **最低 SDK**：Android 14 (API 34)

## 📲 安装使用

### 环境要求
- Android 14 或更高版本
- 支持的设备：手机、平板

### 构建步骤
```bash
# 克隆仓库
git clone https://github.com/yourusername/PhoneAiAssistant.git

# 进入项目目录
cd PhoneAiAssistant

# 使用 Android Studio 打开项目
# 或使用命令行构建
./gradlew assembleDebug
```

### 首次使用

1. **选择使用模式**
   - API 模式：需要在设置中配置 API Key
   - WebView 模式：直接使用，无需配置

2. **配置 API Key**（API 模式）
   - 进入设置页面
   - 选择 AI 服务商
   - 粘贴对应的 API Key

3. **开始聊天**
   - 在输入框输入问题
   - 享受智能对话体验

## 🎯 使用场景

- 💡 **编程助手**：代码编写、调试、解释
- 📚 **学习伙伴**：知识问答、概念解释
- ✍️ **写作辅助**：文章创作、翻译、润色
- 🎨 **创意激发**：头脑风暴、方案设计
- 💬 **日常对话**：闲聊、建议、问题解答

## 🔒 隐私与安全

- 所有聊天记录仅保存在本地设备
- API Key 安全存储在 SharedPreferences
- 不收集、不上传任何用户数据
- 支持随时清除所有数据

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建你的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的修改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启一个 Pull Request

## 📄 开源协议

本项目采用 MIT 协议开源 - 查看 [LICENSE](LICENSE) 文件了解详情

## 🙏 致谢

- [Jetpack Compose](https://developer.android.com/jetpack/compose) - 现代化的 Android UI 工具包
- [Retrofit](https://square.github.io/retrofit/) - 类型安全的 HTTP 客户端
- [Dagger Hilt](https://dagger.dev/hilt/) - Android 依赖注入框架
- [Coil](https://coil-kt.github.io/coil/) - 图片加载库

---

<p align="center">
  如果这个项目对你有帮助，请给一个 ⭐️ Star！
</p>