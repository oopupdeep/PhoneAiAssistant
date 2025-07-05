#!/usr/bin/env bash
set -euo pipefail

# ========= 配置 =========
SDK_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
API_LEVEL=35
BUILD_TOOLS=35.0.0
export ANDROID_SDK_ROOT="$HOME/android-sdk"

# ========= 安装 CLI Tools =========
mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
cd "$ANDROID_SDK_ROOT/cmdline-tools"

rm -rf latest                      # 关键：先删旧目录防止 mv 报错
curl -sSL "$SDK_URL" -o tools.zip
unzip -q tools.zip && rm tools.zip
mv cmdline-tools latest

# ========= 安装平台与构建工具 =========
export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH"
yes | sdkmanager --licenses >/dev/null
sdkmanager --sdk_root="$ANDROID_SDK_ROOT"          \
  "platform-tools"                                 \
  "platforms;android-${API_LEVEL}"                 \
  "build-tools;${BUILD_TOOLS}"

# ========= 写 local.properties =========
echo "sdk.dir=$ANDROID_SDK_ROOT" > "$OLDPWD/local.properties"

# ========= 预拉 Gradle 依赖（在线时执行一次即可） =========
cd "$OLDPWD"
./gradlew --refresh-dependencies --no-daemon
