#!/bin/zsh
set -euo pipefail

PACKAGE="io.fluxreader.app"
ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
[[ -x "$ADB" ]] || ADB="$(command -v adb || true)"
[[ -n "$ADB" && -x "$ADB" ]] || { echo "❌ adb не найден"; exit 2; }

DEVICE_COUNT="$($ADB devices | awk 'NR>1 && $2=="device" {n++} END {print n+0}')"
[[ "$DEVICE_COUNT" -eq 1 ]] || { echo "❌ Нужен ровно один подключённый Android-устройство (сейчас: $DEVICE_COUNT)"; exit 3; }

PID="$($ADB shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r' | awk '{print $1}')"
if [[ -z "$PID" ]]; then
  echo "ℹ️ Flux Reader не запущен. Запускаю…"
  $ADB shell am start -W -n "$PACKAGE/.MainActivity" >/dev/null
  sleep 1
  PID="$($ADB shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r' | awk '{print $1}')"
fi

[[ -n "$PID" ]] || { echo "❌ Не удалось получить PID Flux Reader"; exit 4; }

echo "Flux Reader runtime profile · pid $PID"
echo
echo "=== MEMORY ==="
$ADB shell dumpsys meminfo "$PACKAGE"
echo
echo "=== PROCESS ==="
$ADB shell top -b -n 1 -p "$PID" -o PID,CPU%,RES,VIRT,ARGS 2>/dev/null || true
echo
echo "=== PACKAGE SIZE ==="
$ADB shell pm path "$PACKAGE" | tr -d '\r'
