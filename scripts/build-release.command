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
      cat <<'HELP'
Flux Reader — local release build

Usage:
  scripts/build-release.command
  scripts/build-release.command --with-ocr

Builds a non-debuggable release APK. For alpha update compatibility it is signed with the
same local Android debug keystore/certificate as previous local builds.
HELP
      exit 0 ;;
    *) echo "Неизвестный аргумент: $arg"; exit 64 ;;
  esac
done

if (( WITH_OCR )); then
  scripts/fetch-ocr-models.command
fi

if ! command -v java >/dev/null 2>&1; then
  echo "❌ Java не найдена. Нужен JDK 17+."
  exit 1
fi
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
if [[ ! -d "$SDK" ]]; then
  echo "❌ Android SDK не найден: $SDK"
  exit 2
fi
export ANDROID_HOME="$SDK"
export ANDROID_SDK_ROOT="$SDK"
printf 'sdk.dir=%s\n' "$SDK" > local.properties

[[ -x ./gradlew ]] || chmod +x ./gradlew
echo "Flux Reader $VERSION — release build"
./gradlew :app:testDebugUnitTest :app:assembleRelease --console=plain

SOURCE="app/build/outputs/apk/release/app-release.apk"
[[ -f "$SOURCE" ]] || { echo "❌ APK не найден: $SOURCE"; exit 3; }
mkdir -p dist
APK="dist/FluxReader-$VERSION-release.apk"
cp -f "$SOURCE" "$APK"

echo
echo "✅ Release APK: $PWD/$APK"
echo "ℹ️ APK non-debuggable; подпись остаётся совместимой с предыдущими локальными alpha-сборками."
if command -v shasum >/dev/null 2>&1; then
  echo "SHA-256: $(shasum -a 256 "$APK" | awk '{print $1}')"
fi
