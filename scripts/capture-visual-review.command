#!/bin/zsh
set -euo pipefail
cd "$(dirname "$0")/.."
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
ADB="$SDK/platform-tools/adb"
NAME="${1:-screen}"
[[ "$NAME" =~ '^[a-zA-Z0-9_-]+$' ]] || { echo "Имя снимка: латинские буквы, цифры, - и _."; exit 2; }
mkdir -p dist/visual-review
"$ADB" -s emulator-5580 exec-out screencap -p > "dist/visual-review/$NAME.png"
echo "$PWD/dist/visual-review/$NAME.png"
