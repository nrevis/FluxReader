#!/bin/zsh
set -euo pipefail
cd "$(dirname "$0")/.."
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
ADB="$SDK/platform-tools/adb"
EMU="$SDK/emulator/emulator"
SERIAL="emulator-5580"
SCENE="${1:-settings}"
THEME="${2:-wasteland}"
export ANDROID_AVD_HOME="${FLUX_REVIEW_AVD_HOME:-${ANDROID_AVD_HOME:-$HOME/.android/avd}}"
[[ -x "$ADB" && -x "$EMU" ]] || { echo "Нужны Android Emulator и platform-tools. См. VISUAL-REVIEW.md"; exit 2; }
mkdir -p dist/visual-review
if ! "$ADB" -s "$SERIAL" get-state >/dev/null 2>&1; then
  nohup "$EMU" -avd FluxReaderVisual -port 5580 -no-snapshot -no-boot-anim -no-audio -gpu host -memory 2048 \
    >dist/visual-review/emulator.log 2>&1 </dev/null &
fi
READY=0
for attempt in {1..120}; do
  if [[ "$("$ADB" -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; then
    READY=1
    break
  fi
  sleep 1
done
[[ "$READY" == "1" ]] || { echo "Эмулятор не загрузился. См. dist/visual-review/emulator.log"; exit 3; }
bash scripts/verify-version.command
printf 'sdk.dir=%s\n' "$SDK" > local.properties
./gradlew :app:assembleDebug --console=plain --no-daemon
"$ADB" -s "$SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk
"$ADB" -s "$SERIAL" shell am start -S -n io.fluxreader.app/.debug.VisualReviewActivity \
  --es scene "$SCENE" --es theme "$THEME"
echo "Открыта проверочная сцена: $SCENE / $THEME. Управляйте приложением в окне эмулятора."
