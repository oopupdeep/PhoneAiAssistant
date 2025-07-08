# PhoneAiAssistant 项目架构文档

## 项目概述

PhoneAiAssistant 是一个基于现代 Android 开发技术栈构建的 AI 聊天助手应用。它支持多个 AI 服务提供商，包括通过 API 和 WebView 两种模式进行交互。

## 技术栈

- **编程语言**: Kotlin
- **UI 框架**: Jetpack Compose (Material 3)
- **架构模式**: MVVM + Clean Architecture
- **依赖注入**: Hilt (Dagger)
- **数据库**: Room
- **网络请求**: Retrofit + OkHttp
- **异步编程**: Kotlin Coroutines + Flow
- **导航**: Navigation Compose
- **最低 SDK**: 34 (Android 14)
- **目标 SDK**: 35

## 架构设计

### 整体架构

项目采用 **MVVM (Model-View-ViewModel)** 架构模式，结合 **Clean Architecture** 原则：

```
┌─────────────────────────────────────────────────┐
│              Presentation Layer                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────┐ │
│  │   Screens   │  │ Components  │  │ViewModels│ │
│  └─────────────┘  └─────────────┘  └─────────┘ │
└─────────────────────────────────────────────────┘
                        ↕
┌─────────────────────────────────────────────────┐
│                Domain Layer                      │
│  ┌─────────────┐  ┌─────────────┐              │
│  │   Entities  │  │ Repositories│              │
│  └─────────────┘  └─────────────┘              │
└─────────────────────────────────────────────────┘
                        ↕
┌─────────────────────────────────────────────────┐
│                 Data Layer                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────┐ │
│  │   Database  │  │   Network   │  │  Prefs  │ │
│  └─────────────┘  └─────────────┘  └─────────┘ │
└─────────────────────────────────────────────────┘
```

## 项目结构详解

### 1. 数据层 (Data Layer) - `/data/`

#### 1.1 认证管理 - `/Authenticate/`
- **Companies.kt**: 定义支持的 AI 服务提供商枚举
- **CompanyManager.kt**: 管理不同 AI 服务的配置和 API 密钥

#### 1.2 数据库 - `/database/`
- **AppDatabase.kt**: Room 数据库主类，管理数据库实例
- **ConversationDao.kt**: 对话数据访问对象，提供对话的 CRUD 操作
- **MessageDao.kt**: 消息数据访问对象，提供消息的 CRUD 操作
- **ConversationEntity.kt**: 对话实体类，映射数据库表
- **MessageEntity.kt**: 消息实体类，映射数据库表
- **Converters.kt**: Room 类型转换器，处理复杂类型存储
- **DatabaseModule.kt**: Hilt 模块，提供数据库相关依赖

#### 1.3 实体类 - `/entity/`
- **/chat/**
  - **Conversation.kt**: 对话领域模型
  - **Message.kt**: 消息领域模型
  - **ModelInfo.kt**: AI 模型信息
- **/network/**
  - **ChatRequest.kt**: 聊天请求数据传输对象
  - **ChatResponse.kt**: 聊天响应数据传输对象
  - **StreamResponse.kt**: 流式响应数据传输对象

#### 1.4 网络层 - `/network/`
- **ChatService.kt**: 聊天 API 接口定义，支持流式响应
- **ModelService.kt**: 模型列表 API 接口定义
- **NetworkModule.kt**: Hilt 模块，配置 Retrofit 和 OkHttp，支持动态 BaseURL

#### 1.5 偏好设置 - `/preferences/`
- **AppPreferences.kt**: SharedPreferences 封装，管理应用设置

#### 1.6 仓库层 - `/repository/`
- **ChatRepository.kt**: 处理聊天相关业务逻辑，支持流式响应
- **ConversationRepository.kt**: 管理对话的创建、更新、删除
- **ModelRepository.kt**: 管理 AI 模型列表获取

### 2. 依赖注入 - `/di/`
- **ChatModeModule.kt**: 提供聊天模式相关的依赖

### 3. UI 层 (Presentation Layer) - `/ui/`

#### 3.1 活动 - `/activities/`
- **ChatActivity.kt**: 主活动，承载 Compose UI

#### 3.2 聊天功能 - `/chat/`
- **ChatScreen.kt**: 主聊天界面
- **ChatViewModel.kt**: 聊天功能的 ViewModel，管理聊天状态和业务逻辑
- **/components/**: 可复用 UI 组件
  - **AppDrawer.kt**: 侧边栏导航抽屉，显示对话历史
  - **ChatInputBar.kt**: 消息输入组件
  - **ChatTopAppBar.kt**: 顶部应用栏
  - **MessageBubble.kt**: 单个消息气泡组件
  - **MessageList.kt**: 消息列表容器
  - **ModelDropDown.kt**: AI 模型选择下拉框

#### 3.3 屏幕 - `/screens/`
- **UnifiedChatScreen.kt**: 统一聊天界面，支持多种模式
- **SettingsScreen.kt**: 设置界面
- **WebViewScreen.kt**: WebView 模式界面
- **AiServiceSelectionScreen.kt**: AI 服务选择界面

#### 3.4 视图模型 - `/viewmodels/`
- **UnifiedChatViewModel.kt**: 统一聊天界面的 ViewModel
- **SettingsViewModel.kt**: 设置界面的 ViewModel
- **WebViewViewModel.kt**: WebView 界面的 ViewModel

#### 3.5 导航 - `/navigation/`
- **UnifiedAppNavigation.kt**: 应用导航图定义

#### 3.6 主题 - `/theme/`
- Material 3 主题相关组件

## 核心功能流程

### 1. 对话管理流程
```
用户操作 → ChatViewModel → ConversationRepository → ConversationDao → Database
                ↓                    ↓
            UI 更新  ←  StateFlow 更新
```

### 2. 消息发送流程
```
1. 用户输入消息
2. ChatViewModel.sendMessageStream()
3. 保存用户消息到数据库
4. 创建 ChatRequest
5. 调用 ChatRepository.getChatStream()
6. 处理流式响应，实时更新 UI
7. 保存 AI 响应到数据库
```

### 3. 依赖注入流程
```
@HiltAndroidApp → @AndroidEntryPoint → @HiltViewModel → @Inject
     ↓                    ↓                   ↓            ↓
Application →        Activity →         ViewModel →   Repository
```

## 关键特性

### 1. 多模式支持
- **API 模式**: 直接调用 AI 服务 API
- **WebView 模式**: 通过 WebView 访问 AI 服务网页版
- 支持多个 AI 提供商 (DeepSeek, OpenAI, 通义千问等)

### 2. 流式响应
- 支持 Server-Sent Events (SSE) 流式响应
- 实时显示 AI 生成的内容
- 优化用户体验

### 3. 数据持久化
- 使用 Room 数据库存储对话和消息
- 支持对话历史搜索
- 支持删除对话功能

### 4. 响应式编程
- 使用 Kotlin Flow 进行数据流管理
- StateFlow 管理 UI 状态
- 自动更新 UI

### 5. 现代 UI
- 完全使用 Jetpack Compose 构建
- Material 3 设计系统
- 支持深色主题

## 开发指南

### 添加新的 AI 服务提供商
1. 在 `Companies.kt` 中添加新的枚举值
2. 在 `CompanyManager.kt` 中配置相应的 BaseURL
3. 实现相应的 API 接口（如需要）
4. 在 UI 中添加选择选项

### 添加新功能
1. 在相应的 Repository 中添加业务逻辑
2. 在 ViewModel 中暴露状态和方法
3. 在 UI 层创建或修改 Composable 函数
4. 更新导航图（如需要）

### 数据库迁移
1. 修改实体类
2. 在 `AppDatabase.kt` 中增加版本号
3. 创建 Migration 对象
4. 测试迁移流程

## 注意事项

1. **最低 SDK 版本为 34**，确保使用的 API 兼容
2. 所有网络请求都应该在 Repository 层进行
3. ViewModel 不应该直接依赖 Android Framework
4. 使用 StateFlow 而不是 LiveData 在 Compose 中
5. 遵循 Material 3 设计规范

## 测试命令

```bash
# 运行单元测试
./gradlew test

# 运行 Android 测试
./gradlew connectedAndroidTest

# 构建 Debug APK
./gradlew assembleDebug

# 运行 lint 检查
./gradlew lint
```