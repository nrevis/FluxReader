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
Flux Reader — debug build

Usage:
  scripts/build-debug.command
  scripts/build-debug.command --with-ocr

--with-ocr downloads and bundles rus + ukr + eng tessdata_fast before build.
HELP
      exit 0 ;;
    *) echo "Неизвестный аргумент: $arg"; exit 64 ;;
  esac
done

echo "Flux Reader $VERSION — debug build"
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
  echo "Запустите scripts/bootstrap-build-apk.command"
  exit 2
fi
export ANDROID_HOME="$SDK"
export ANDROID_SDK_ROOT="$SDK"
printf 'sdk.dir=%s\n' "$SDK" > local.properties

echo "✅ Android SDK: $SDK"
[[ -x ./gradlew ]] || chmod +x ./gradlew
./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain

SOURCE="app/build/outputs/apk/debug/app-debug.apk"
[[ -f "$SOURCE" ]] || { echo "❌ APK не найден: $SOURCE"; exit 3; }
mkdir -p dist
APK="dist/FluxReader-$VERSION-debug.apk"
cp -f "$SOURCE" "$APK"

echo
echo "✅ APK: $PWD/$APK"
if command -v shasum >/dev/null 2>&1; then
  echo "SHA-256: $(shasum -a 256 "$APK" | awk '{print $1}')"
fi
