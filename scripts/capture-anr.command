#!/usr/bin/env bash
set -euo pipefail
PACKAGE="${1:-io.fluxreader.app}"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
ADB="${ADB:-}"
[[ -n "$ADB" ]] || ADB="$(command -v adb 2>/dev/null || true)"
[[ -n "$ADB" ]] || ADB="$SDK/platform-tools/adb"
[[ -x "$ADB" ]] || { echo "❌ adb не найден: $ADB"; exit 1; }
OUT="${2:-$HOME/Desktop}"
mkdir -p "$OUT"
STAMP="$(date +%Y%m%d-%H%M%S)"
LOG="$OUT/FluxReader-0.2-ANR-$STAMP-logcat.txt"
ANR="$OUT/FluxReader-0.2-ANR-$STAMP-lastanr.txt"
THREADS="$OUT/FluxReader-0.2-ANR-$STAMP-focus.txt"
"$ADB" start-server >/dev/null
PID="$("$ADB" shell pidof -s "$PACKAGE" 2>/dev/null | tr -d '\r' || true)"
[[ -n "$PID" ]] || { echo "❌ Процесс $PACKAGE не найден. Откройте Flux Reader."; exit 2; }
"$ADB" shell kill -3 "$PID" || true
sleep 2
"$ADB" logcat -d -v threadtime > "$LOG"
"$ADB" shell dumpsys activity lastanr > "$ANR" || true
grep -nEi '"main"|ANR in|Input dispatching timed out|io\.fluxreader|AppViewModel|ReaderScreen|SpeedReaderScreen|PdfReaderScreen|EpubParser|Fb2Parser|PdfRenderer' "$LOG" "$ANR" > "$THREADS" || true
echo "✅ Сохранено:"
ls -lh "$LOG" "$ANR" "$THREADS"
