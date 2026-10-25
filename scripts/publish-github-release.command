#!/bin/zsh
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ "${1:-}" != "--publish" ]]; then
  cat <<'HELP'
Публикация Flux Reader в GitHub Releases (только вручную).

  scripts/build-release.command
  # Сохранить изменения и отправить commit в GitHub.
  scripts/publish-github-release.command --publish

Создаёт draft с APK, исходниками и двумя SHA-256, проверяет все файлы,
затем публикует выпуск. Подробнее: docs/RELEASING.md.
HELP
  exit 0
fi

for tool in gh shasum python3 git; do
  command -v "$tool" >/dev/null || { echo "Не найден $tool"; exit 2; }
done
bash scripts/verify-version.command
[[ -z "$(git status --porcelain)" ]] || { echo 'Сначала сохраните изменения в Git.'; exit 3; }
VERSION="$(sed -n 's/^versionName=//p' VERSION.txt)"
TAG="v$VERSION"
COMMIT="$(git rev-parse HEAD)"
APK="$PWD/dist/FluxReader-$VERSION-release.apk"
[[ -s "$APK" ]] || { echo "APK отсутствует: $APK"; exit 4; }
ALPHA="${VERSION##*-alpha.}"
NOTES="$PWD/docs/releases/$VERSION.md"
if [[ "$ALPHA" != "$VERSION" ]]; then NOTES="$PWD/docs/releases/alpha$ALPHA.md"; fi
[[ -s "$NOTES" ]] || { echo "Описание выпуска отсутствует: $NOTES"; exit 5; }
gh auth status >/dev/null
if gh release view "$TAG" --repo nrevis/FluxReader >/dev/null 2>&1; then
  echo "Релиз $TAG уже существует: ничего не перезаписываем."
  exit 6
fi

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
SIGNER="$SDK/build-tools/37.0.0/apksigner"
[[ -x "$SIGNER" ]] || { echo 'Нужен apksigner из Android SDK.'; exit 7; }
CERT_INFO="$("$SIGNER" verify --print-certs "$APK")"
echo "$CERT_INFO" | grep 'certificate SHA-256 digest'
echo "$CERT_INFO" | grep -q 'certificate SHA-256 digest: 8cfb92b70fc3054b403993486438ae9a214ed749362e42ac3933c07be2f89ace' || {
  echo 'Сертификат отличается от предыдущих alpha APK.'; exit 8;
}

OUT="$PWD/dist/release"
bash scripts/package-release.command "$OUT"
SOURCE="$OUT/FluxReader-$VERSION-clean.zip"
CHECKSUM="$APK.sha256"
SOURCE_SUM="$SOURCE.sha256"
for file in "$APK" "$SOURCE"; do
  SHA="$(shasum -a 256 "$file" | awk '{print $1}')"
  printf '%s  %s\n' "$SHA" "$(basename "$file")" > "$file.sha256"
done

# A failed upload or verification leaves a draft, never a half-published update.
if [[ "$ALPHA" != "$VERSION" ]]; then
  gh release create "$TAG" "$APK" "$CHECKSUM" "$SOURCE" "$SOURCE_SUM" --repo nrevis/FluxReader \
    --target "$COMMIT" --title "Flux Reader $VERSION" --notes-file "$NOTES" --draft --prerelease
else
  gh release create "$TAG" "$APK" "$CHECKSUM" "$SOURCE" "$SOURCE_SUM" --repo nrevis/FluxReader \
    --target "$COMMIT" --title "Flux Reader $VERSION" --notes-file "$NOTES" --draft
fi

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
gh api "repos/nrevis/FluxReader/releases/tags/$TAG" > "$TMP/release.json"
python3 - "$TMP/release.json" "$APK" "$CHECKSUM" "$SOURCE" "$SOURCE_SUM" <<'PYVERIFY'
import hashlib,json,sys
from pathlib import Path
release=json.loads(Path(sys.argv[1]).read_text())
assets={a['name']:a for a in release['assets']}
paths=[Path(p) for p in sys.argv[2:]]
assert set(assets)=={p.name for p in paths}, 'Неверный набор файлов релиза'
for path in paths:
    asset=assets[path.name]
    assert asset['size']==path.stat().st_size, path.name
    h=hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda:stream.read(1024*1024),b''):h.update(chunk)
    digest=h.hexdigest()
    assert asset.get('digest')=='sha256:'+digest, 'SHA-256 не совпадает: '+path.name
print('Все четыре файла проверены по размеру и SHA-256.')
PYVERIFY
gh release edit "$TAG" --repo nrevis/FluxReader --draft=false
echo "Релиз $TAG опубликован: https://github.com/nrevis/FluxReader/releases/tag/$TAG"
