#!/bin/zsh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

INSTALL=0
WITH_OCR=0
for arg in "$@"; do
  case "$arg" in
    --install) INSTALL=1 ;;
    --with-ocr) WITH_OCR=1 ;;
    -h|--help)
      cat <<'HELP'
Flux Reader — macOS Android SDK bootstrap

Usage:
  scripts/bootstrap-build-apk.command
  scripts/bootstrap-build-apk.command --install
  scripts/bootstrap-build-apk.command --with-ocr --install

Installs official Android command-line tools into ~/Library/Android/sdk when
needed, Platform 37.0, Build Tools 37.0.0 and Platform Tools, then builds APK.
HELP
      exit 0 ;;
    *) echo "Неизвестный аргумент: $arg"; exit 64 ;;
  esac
done

[[ "$(uname -s)" == "Darwin" ]] || { echo "❌ Bootstrap рассчитан на macOS."; exit 1; }

if /usr/libexec/java_home -v 17 >/dev/null 2>&1; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
  export PATH="$JAVA_HOME/bin:$PATH"
elif command -v java >/dev/null 2>&1; then
  echo "ℹ️ JDK 17 через java_home не найден, использую текущий java:"
else
  echo "❌ Нужен JDK 17+."
  exit 2
fi
java -version 2>&1 | head -n 1

SDK_ROOT="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export ANDROID_HOME="$SDK_ROOT"
export ANDROID_SDK_ROOT="$SDK_ROOT"
mkdir -p "$SDK_ROOT"
SDKMANAGER="$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"

install_cmdline_tools() {
  local arch package sha url tmp zip extracted actual
  arch="$(uname -m)"
  case "$arch" in
    x86_64)
      package="commandlinetools-mac_x86_64-15859902_latest.zip"
      sha="c5a6378ab5cf7e0d5701921405115befff13e9ff7417fb588389338f8bd050f3" ;;
    arm64)
      package="commandlinetools-mac_arm64-15859902_latest.zip"
      sha="835b62a26162b229b441d1f6d4680383815a270809eb33522c0d480fa5002c4e" ;;
    *) echo "❌ Неподдерживаемая архитектура: $arch"; exit 3 ;;
  esac
  url="https://dl.google.com/android/repository/$package"
  tmp="$(mktemp -d "${TMPDIR:-/tmp}/flux-sdk.XXXXXX")"
  zip="$tmp/$package"
  extracted="$tmp/extracted"
  echo "▶ Скачиваю Android command-line tools ($arch)"
  curl -fL --retry 4 --retry-delay 2 --connect-timeout 20 "$url" -o "$zip"
  actual="$(shasum -a 256 "$zip" | awk '{print $1}')"
  [[ "$actual" == "$sha" ]] || { echo "❌ SHA-256 command-line tools не совпал"; rm -rf "$tmp"; exit 4; }
  mkdir -p "$extracted"
  /usr/bin/unzip -q "$zip" -d "$extracted"
  [[ -d "$extracted/cmdline-tools" ]] || { echo "❌ Неожиданная структура command-line tools"; rm -rf "$tmp"; exit 5; }
  rm -rf "$SDK_ROOT/cmdline-tools/latest"
  mkdir -p "$SDK_ROOT/cmdline-tools/latest"
  cp -R "$extracted/cmdline-tools/." "$SDK_ROOT/cmdline-tools/latest/"
  chmod +x "$SDK_ROOT/cmdline-tools/latest/bin/"* 2>/dev/null || true
  rm -rf "$tmp"
}

[[ -x "$SDKMANAGER" ]] || install_cmdline_tools
[[ -x "$SDKMANAGER" ]] || { echo "❌ sdkmanager не найден после bootstrap"; exit 6; }

export PATH="$SDK_ROOT/cmdline-tools/latest/bin:$SDK_ROOT/platform-tools:$PATH"
yes | "$SDKMANAGER" --sdk_root="$SDK_ROOT" --licenses >/dev/null || true
"$SDKMANAGER" --sdk_root="$SDK_ROOT" \
  "platform-tools" \
  "platforms;android-37.0" \
  "build-tools;37.0.0"

[[ -f "$SDK_ROOT/platforms/android-37.0/android.jar" ]] || { echo "❌ Platform 37.0 не установлена"; exit 7; }
printf 'sdk.dir=%s\n' "$SDK_ROOT" > local.properties

if (( WITH_OCR )); then
  scripts/build-debug.command --with-ocr
else
  scripts/build-debug.command
fi
if (( INSTALL )); then
  # APK is already built; install directly to avoid a second build.
  SDK="$SDK_ROOT"
  ADB="$SDK/platform-tools/adb"
  APK="$ROOT/dist/FluxReader-0.2.0-alpha.69-debug.apk"
  [[ -x "$ADB" && -f "$APK" ]] || { echo "❌ adb/APK не найден"; exit 8; }
  "$ADB" start-server >/dev/null
  "$ADB" install -r "$APK"
  "$ADB" shell am start -n io.fluxreader.app/.MainActivity >/dev/null || true
fi
