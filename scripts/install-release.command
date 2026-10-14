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
      echo "Usage: scripts/install-release.command [--with-ocr]"
      exit 0 ;;
    *) echo "Неизвестный аргумент: $arg"; exit 64 ;;
  esac
done

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
export ANDROID_HOME="$SDK"
export ANDROID_SDK_ROOT="$SDK"
if (( WITH_OCR )); then
  scripts/build-release.command --with-ocr
else
  scripts/build-release.command
fi

APK="$PWD/dist/FluxReader-$VERSION-release.apk"
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
echo "▶ Install release APK"
set +e
INSTALL_OUTPUT=$("$ADB" install -r "$APK" 2>&1)
STATUS=$?
set -e
printf '%s\n' "$INSTALL_OUTPUT"
if (( STATUS != 0 )); then
  if [[ "$INSTALL_OUTPUT" == *"INSTALL_FAILED_UPDATE_INCOMPATIBLE"* ]]; then
    echo
    echo "❌ Подпись установленной копии отличается. Я не удаляю приложение автоматически, чтобы не потерять библиотеку/прогресс."
    echo "   Если данные не нужны: adb uninstall io.fluxreader.app && scripts/install-release.command"
  fi
  exit "$STATUS"
fi

echo
echo "▶ Launch"
"$ADB" shell am start -n io.fluxreader.app/.MainActivity >/dev/null || true
echo "✅ Flux Reader $VERSION release установлен и запущен."
echo "ℹ️ Если Play Protect всё равно показывает предупреждение, это уже репутационная проверка sideload, а не debug-флаг APK."
