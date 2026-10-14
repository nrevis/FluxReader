#!/bin/zsh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/app/src/main/assets/tessdata"
BASE="https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/main"
mkdir -p "$DEST"

fetch_model() {
  local lang="$1"
  local target="$DEST/$lang.traineddata"
  local temp="$target.download"
  echo "▶ OCR model: $lang"
  curl --fail --location --retry 3 --retry-delay 2 --connect-timeout 15 --max-time 180 \
    "$BASE/$lang.traineddata" -o "$temp"
  local size
  size="$(wc -c < "$temp" | tr -d ' ')"
  if [[ "$size" -lt 100000 ]]; then
    rm -f "$temp"
    echo "❌ $lang.traineddata повреждён: ${size} bytes" >&2
    exit 1
  fi
  mv "$temp" "$target"
  echo "✅ $lang.traineddata — ${size} bytes"
}

fetch_model rus
fetch_model ukr
fetch_model eng

echo
echo "✅ rus + ukr + eng добавлены в app/src/main/assets/tessdata/."
echo "Следующая APK-сборка будет содержать офлайн Tesseract-модели."
