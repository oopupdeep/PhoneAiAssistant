# 讯飞语音识别SDK配置说明

## 1. 下载SDK

1. 访问[讯飞开放平台](https://www.xfyun.cn/)
2. 注册并登录账号
3. 创建应用，获取AppId
4. 下载Android SDK

## 2. 添加SDK文件

将下载的SDK中的以下文件复制到此目录：
- `Msc.jar` - 讯飞语音核心库

将以下so文件复制到对应的架构目录：
- `app/src/main/jniLibs/armeabi-v7a/libmsc.so`
- `app/src/main/jniLibs/arm64-v8a/libmsc.so`
- `app/src/main/jniLibs/x86/libmsc.so`
- `app/src/main/jniLibs/x86_64/libmsc.so`

## 3. 配置AppId

在应用设置中配置讯飞AppId，或者在代码中设置：
```kotlin
appPreferences.xfAppId = "your_xunfei_app_id"
```

## 4. 权限说明

以下权限已在AndroidManifest.xml中配置：
- INTERNET - 网络访问
- RECORD_AUDIO - 录音权限
- ACCESS_NETWORK_STATE - 网络状态
- ACCESS_WIFI_STATE - WiFi状态
- CHANGE_NETWORK_STATE - 修改网络状态
- READ_PHONE_STATE - 读取手机状态
- WRITE_EXTERNAL_STORAGE - 存储权限

## 5. 注意事项

- 确保在真实设备上测试，模拟器可能不支持语音识别
- 首次使用需要网络连接以验证AppId
- 请保护好您的AppId，不要泄露