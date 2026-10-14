#!/bin/zsh
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ "${1:-}" != "--publish" ]]; then
  cat <<'HELP'
Публикация Flux Reader в GitHub Releases (только вручную).

  scripts/build-release.command
  scripts/publish-github-release.command --publish

ВНИМАНИЕ: для обновлений поверх установленной версии все APK должны быть
подписаны ОДНИМ И ТЕМ ЖЕ сертификатом. Текущий alpha release подписывается
локальным Android debug keystore. Сохраняйте ключ на Mac и НЕ используйте
эпизодический CI/debug keystore с иным сертификатом.
HELP
  exit 0
fi

command -v gh >/dev/null || { echo 'Нужен GitHub CLI: brew install gh && gh auth login'; exit 2; }
command -v shasum >/dev/null || { echo 'Не найден shasum'; exit 2; }
bash scripts/verify-version.command
VERSION="$(sed -n 's/^versionName=//p' VERSION.txt)"
APK="$PWD/dist/FluxReader-$VERSION-release.apk"
[[ -s "$APK" ]] || { echo "APK отсутствует. Выполните scripts/build-release.command: $APK"; exit 3; }

gh auth status >/dev/null
TAG="v$VERSION"
if gh release view "$TAG" --repo nrevis/FluxReader >/dev/null 2>&1; then
  echo "Релиз $TAG уже существует: ничего не перезаписываем."
  exit 4
fi

SHA="$(shasum -a 256 "$APK" | awk '{print $1}')"
CHECKSUM="$APK.sha256"
printf '%s  %s\n' "$SHA" "$(basename "$APK")" > "$CHECKSUM"

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
if [[ -x "$SDK/build-tools/37.0.0/apksigner" ]]; then
  "$SDK/build-tools/37.0.0/apksigner" verify --print-certs "$APK" | grep -E 'Signer #1 certificate SHA-256 digest|Verified using'
fi

echo "Публикуем $TAG в https://github.com/nrevis/FluxReader/releases"
echo "SHA-256: $SHA"
NOTES="RELEASE-NOTES-$VERSION.md"
if [[ -f "$NOTES" ]]; then
  gh release create "$TAG" "$APK" "$CHECKSUM" --repo nrevis/FluxReader \
    --title "Flux Reader $VERSION" --notes-file "$NOTES" --prerelease
else
  gh release create "$TAG" "$APK" "$CHECKSUM" --repo nrevis/FluxReader \
    --title "Flux Reader $VERSION" --notes "Flux Reader $VERSION" --prerelease
fi

echo "Релиз $TAG опубликован."
