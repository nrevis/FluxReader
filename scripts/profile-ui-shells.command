#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
ADB="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}/platform-tools/adb"
[[ -x "$ADB" ]] || ADB="$(command -v adb || true)"
[[ -x "$ADB" ]] || { echo "Нужен adb в PATH или Android SDK" >&2; exit 1; }
PACKAGE=io.fluxreader.app
DIR="$PWD/ui-performance-results"
mkdir -p "$DIR"
echo "Процедура: откройте одну и ту же книгу / главу в одной и той же области экрана."
echo "Сначала выберите обычную тему. Нажмите Enter, когда экран полностью открылся."
read -r _
"$ADB" shell dumpsys gfxinfo "$PACKAGE" reset >/dev/null
printf 'В течение 15 секунд перелистывайте или открывайте аналогичные разделы стандартной темы. Затем Enter.\n'
read -r _
"$ADB" shell dumpsys gfxinfo "$PACKAGE" > "$DIR/standard-gfxinfo.txt"
"$ADB" shell dumpsys gfxinfo "$PACKAGE" framestats > "$DIR/standard-frames.txt"
echo "Теперь включите экспериментальную оболочку, используйте ту же книгу/главу и размер текста. Нажмите Enter."
read -r _
"$ADB" shell dumpsys gfxinfo "$PACKAGE" reset >/dev/null
printf 'Повторите те же действия за сопоставимое время. Затем Enter.\n'
read -r _
"$ADB" shell dumpsys gfxinfo "$PACKAGE" > "$DIR/experimental-gfxinfo.txt"
"$ADB" shell dumpsys gfxinfo "$PACKAGE" framestats > "$DIR/experimental-frames.txt"
echo "Логи сохранены в $DIR. Сравнивайте total frames / janky frames / 90th-99th percentile, учитывайте разные объемы нагрузки."
