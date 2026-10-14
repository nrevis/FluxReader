#!/bin/zsh
set -euo pipefail
cd "$(dirname "$0")"

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
export ANDROID_HOME="$SDK"
export ANDROID_SDK_ROOT="$SDK"

if [[ ! -d "$SDK" ]]; then
  echo "❌ Android SDK не найден: $SDK"
  echo "   Установите Android Studio / SDK или задайте ANDROID_HOME."
  exit 2
fi

./scripts/build-release.command

ADB="$SDK/platform-tools/adb"
if [[ ! -x "$ADB" ]]; then
  ADB="$(command -v adb 2>/dev/null || true)"
fi
[[ -n "$ADB" && -x "$ADB" ]] || { echo "❌ adb не найден"; exit 3; }

VERSION="$(sed -n 's/^versionName=//p' VERSION.txt)"
bash scripts/verify-version.command
APK="$PWD/dist/FluxReader-$VERSION-release.apk"
[[ -f "$APK" ]] || { echo "❌ APK не создан: $APK"; exit 4; }

"$ADB" start-server >/dev/null
"$ADB" install -r "$APK"
"$ADB" shell am start -n io.fluxreader.app/.MainActivity >/dev/null || true

echo "✅ Flux Reader $VERSION установлен и запущен."
