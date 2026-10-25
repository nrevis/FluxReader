#!/bin/zsh
set -euo pipefail
cd "$(dirname "$0")/.."

VERSION="$(sed -n 's/^versionName=//p' VERSION.txt)"
bash scripts/verify-version.command
OUT_DIR="${1:-$PWD/dist/release}"
ZIP="$OUT_DIR/FluxReader-$VERSION-clean.zip"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
ROOT="$TMP/FluxReader-$VERSION-clean"

mkdir -p "$OUT_DIR" "$ROOT"

# Copy source tree, excluding generated/local state. Never exclude the Gradle wrapper JAR.
rsync -a ./ "$ROOT/" \
  --exclude '.git/' \
  --exclude '.gradle/' \
  --exclude '.kotlin/' \
  --exclude '__pycache__/' \
  --exclude '*.pyc' \
  --exclude 'build/' \
  --exclude 'app/build/' \
  --exclude 'dist/' \
  --exclude '_archive/' \
  --exclude 'CHANGELOG*' \
  --exclude 'changelog*' \
  --exclude 'local.properties' \
  --exclude '*.jks' \
  --exclude '*.keystore' \
  --exclude '.env' \
  --exclude '.env.*' \
  --exclude '*.log' \
  --exclude '.DS_Store'

[[ -s "$ROOT/gradle/wrapper/gradle-wrapper.jar" ]] || {
  echo "❌ gradle-wrapper.jar потерян при упаковке" >&2
  exit 40
}
jar tf "$ROOT/gradle/wrapper/gradle-wrapper.jar" | grep -q 'org/gradle/wrapper/GradleWrapperMain.class' || {
  echo "❌ gradle-wrapper.jar невалиден" >&2
  exit 41
}

(
  cd "$ROOT"
  PYTHONDONTWRITEBYTECODE=1 bash scripts/selfcheck.command --static
)

rm -f "$ZIP"
(
  cd "$TMP"
  zip -qry "$ZIP" "FluxReader-$VERSION-clean"
)

unzip -l "$ZIP" > "$TMP/zip-listing.txt"
grep -q "FluxReader-$VERSION-clean/gradle/wrapper/gradle-wrapper.jar" "$TMP/zip-listing.txt" || {
  echo "❌ wrapper JAR отсутствует в финальном ZIP" >&2
  exit 42
}

echo "✅ $ZIP"
