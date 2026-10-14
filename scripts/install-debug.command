#!/bin/zsh
set -euo pipefail
cd "$(dirname "$0")/.."

VERSION="$(sed -n 's/^versionName=//p' VERSION.txt)"
bash scripts/verify-version.command
WITH_OCR=0
for arg in "$@"; do
  case "$arg" in
    --with-ocr) WITH_OCR=1 ;;
    -h|--help)
      echo "Usage: scripts/install-debug.command [--with-ocr]"
      exit 0 ;;
    *) echo "Неизвестный аргумент: $arg"; exit 64 ;;
  esac
done

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
export ANDROID_HOME="$SDK"
export ANDROID_SDK_ROOT="$SDK"
if (( WITH_OCR )); then
  scripts/build-debug.command --with-ocr
else
  scripts/build-debug.command
fi
APK="$PWD/dist/FluxReader-$VERSION-debug.apk"
ADB="$SDK/platform-tools/adb"
if [[ ! -x "$ADB" ]]; then
  ADB="$(command -v adb 2>/dev/null || true)"
fi
[[ -n "$ADB" && -x "$ADB" ]] || { echo "❌ adb не найден"; exit 4; }

echo
echo "▶ Devices"
"$ADB" start-server >/dev/null
"$ADB" devices -l

echo
echo "▶ Install"
"$ADB" install -r "$APK"

echo
echo "▶ Launch"
"$ADB" shell am start -n io.fluxreader.app/.MainActivity >/dev/null || true
echo "✅ Flux Reader $VERSION установлен и запущен."
