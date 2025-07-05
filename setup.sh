#!/usr/bin/env bash
set -e
export ANDROID_SDK_ROOT="$HOME/android-sdk"

mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
cd "$ANDROID_SDK_ROOT/cmdline-tools"
curl -sSL -o tools.zip \
  https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q tools.zip && rm tools.zip
mkdir -p latest && mv cmdline-tools/* latest/

export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH"
yes | sdkmanager --licenses >/dev/null
sdkmanager --sdk_root="$ANDROID_SDK_ROOT" \
  "platform-tools" "platforms;android-35" "build-tools;35.0.0"

echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties
