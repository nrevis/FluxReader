#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if ! command -v kotlinc >/dev/null 2>&1; then
  echo 'SKIP: kotlinc недоступен. Тесты также входят в обычный Gradle test.'
  exit 0
fi
TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"' EXIT
kotlinc \
  app/src/main/java/io/fluxreader/app/reader/SpeedDialogueAnalyzer.kt \
  app/src/main/java/io/fluxreader/app/reader/BookDialogueAnalysis.kt \
  app/src/main/java/io/fluxreader/app/reader/RussianDialogueRoles.kt \
  app/src/main/java/io/fluxreader/app/reader/SpeedDialoguePlayback.kt \
  app/src/main/java/io/fluxreader/app/reader/SpeedDialogueHyphenRun.kt \
  app/src/main/java/io/fluxreader/app/reader/SpeedReaderCursor.kt \
  app/src/main/java/io/fluxreader/app/data/Models.kt \
  app/src/main/java/io/fluxreader/app/data/ReadingPositionPolicy.kt \
  scripts/dialogue_test_main.kt -include-runtime -d "$TEMP_DIR/dialogue-tests.jar"
java -Dfile.encoding=UTF-8 -jar "$TEMP_DIR/dialogue-tests.jar"
