#!/bin/zsh
set -euo pipefail
cd "$(dirname "$0")/.."

VERSION="$(sed -n 's/^versionName=//p' VERSION.txt)"
CODE="$(sed -n 's/^versionCode=//p' VERSION.txt)"
STATIC_ONLY=0
for arg in "$@"; do
  case "$arg" in
    --static) STATIC_ONLY=1 ;;
    -h|--help)
      echo "Usage: scripts/selfcheck.command [--static]"
      echo "  --static  run source/parser checks without requiring Android SDK"
      exit 0 ;;
    *) echo "Неизвестный аргумент: $arg"; exit 64 ;;
  esac
done

echo "Flux Reader $VERSION — clean rewrite self-check"
echo
echo "▶ Kotlin import conflicts / duplicate imports"
python3 scripts/check_alpha76_workflow.py
python3 scripts/review_alpha77.py
python3 scripts/review_alpha83.py
python3 scripts/review_alpha90.py
python3 scripts/review_alpha91.py --self-test
python3 scripts/review_alpha92.py
python3 scripts/review_alpha93.py
python3 scripts/check_kotlin_imports.py
python3 scripts/check_extension_imports.py --self-test
python3 scripts/check_compose_symbols.py
python3 scripts/test_compose_symbols.py
python3 scripts/test_kotlin_import_checker.py

echo "▶ Version / clean-line integrity"
bash scripts/verify-version.command
python3 scripts/test_version_preflight.py
# Internet access is now strictly for the opt-in GitHub Releases updater.
# Narration and book reading remain offline-first.
if grep -Rqs 'android.permission.INTERNET' app/src/main/AndroidManifest.xml &&
   [[ ! -s app/src/main/java/io/fluxreader/app/update/GitHubUpdateManager.kt ]]; then
  echo "❌ Интернет-разрешение допускается только вместе с проверяемым модулем GitHub Updates."
  exit 10
fi
for stale in TextReaderScreen.kt ReaderViewModel.kt BookRepository.kt; do
  if find app/src/main/java -name "$stale" -print -quit | grep -q .; then
    echo "❌ В clean rewrite найден legacy-файл: $stale"
    exit 11
  fi
done
grep -q 'androidx.pdf:pdf-core:1.0.0-beta01' app/build.gradle.kts
grep -q 'androidx.pdf:pdf-document-service:1.0.0-beta01' app/build.gradle.kts
grep -q 'com.google.mlkit:text-recognition:16.0.1' app/build.gradle.kts
grep -q 'tesseract4android:4.9.0' app/build.gradle.kts
grep -q 'https://jitpack.io' settings.gradle.kts
grep -q 'includeGroup("cz.adaptech.tesseract4android")' settings.gradle.kts
grep -q 'class OcrEngine' app/src/main/java/io/fluxreader/app/reader/OcrEngine.kt
grep -q 'fun searchBook' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'class PdfTextEngine' app/src/main/java/io/fluxreader/app/reader/PdfTextEngine.kt
grep -q 'speedText' app/src/main/java/io/fluxreader/app/reader/BookContentRepository.kt
grep -q 'android.graphics.pdf.PdfRenderer' app/src/main/java/io/fluxreader/app/reader/PdfRendererEngine.kt
grep -q 'class AppViewModel' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'catch (cancelled: CancellationException)' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'object SpeedReaderCursor' app/src/main/java/io/fluxreader/app/reader/SpeedReaderCursor.kt
grep -q 'readerChapter' app/src/main/java/io/fluxreader/app/data/Models.kt
grep -q 'speedChapter' app/src/main/java/io/fluxreader/app/data/Models.kt
grep -q 'pdfPage' app/src/main/java/io/fluxreader/app/data/Models.kt
grep -q 'persistSoon' app/src/main/java/io/fluxreader/app/data/LibraryRepository.kt
grep -q 'readerBlockOffsetPx' app/src/main/java/io/fluxreader/app/data/Models.kt
grep -q 'readNavigationTitles' app/src/main/java/io/fluxreader/app/reader/EpubParser.kt
grep -q 'postScale' app/src/main/java/io/fluxreader/app/reader/PdfRendererEngine.kt
grep -q 'speedFontSizeSp' app/src/main/java/io/fluxreader/app/data/Models.kt
grep -q 'saveReaderPosition' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'enum class ReaderNavigationMode' app/src/main/java/io/fluxreader/app/data/Models.kt
grep -q 'edgeTapZonesEnabled' app/src/main/java/io/fluxreader/app/data/Models.kt
grep -q 'fun Modifier.readerGestures' app/src/main/java/io/fluxreader/app/ui/ReaderGestures.kt
grep -q 'object ReaderPageMath' app/src/main/java/io/fluxreader/app/reader/ReaderPageMath.kt
grep -q 'class ReaderPageMathTest' app/src/test/java/io/fluxreader/app/reader/ReaderPageMathTest.kt
grep -q 'data class ReaderLink' app/src/main/java/io/fluxreader/app/data/Models.kt
grep -q 'normalizeLinkTarget' app/src/main/java/io/fluxreader/app/reader/EpubParser.kt
grep -q 'fun openReaderLink' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'BasicText' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt
grep -q 'CACHE_VERSION = 4' app/src/main/java/io/fluxreader/app/reader/Fb2Parser.kt
grep -q 'MAX_BASE64_CHARS' app/src/main/java/io/fluxreader/app/reader/Fb2Parser.kt
grep -q 'currentBodyIsNotes' app/src/main/java/io/fluxreader/app/reader/Fb2Parser.kt
grep -q 'fragment in it.anchors' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'TextRole.POEM' app/src/main/java/io/fluxreader/app/reader/EpubParser.kt
grep -q 'TextRole.TABLE' app/src/main/java/io/fluxreader/app/reader/EpubParser.kt
grep -q 'consumedByChild' app/src/main/java/io/fluxreader/app/ui/ReaderGestures.kt
grep -q 'PreparedReaderPagination' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt
grep -q 'zoom <= 1.01f' app/src/main/java/io/fluxreader/app/ui/PdfReaderScreen.kt
grep -q 'ReaderPreferencesControls' app/src/main/java/io/fluxreader/app/ui/SettingsScreen.kt
grep -q 'isAppearanceLightStatusBars' app/src/main/java/io/fluxreader/app/ui/FluxTheme.kt
grep -q 'val primary = findReadableSpeedChapter' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'primary ?: fallback ?: error' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'ReaderFontMode.JETBRAINS_NERD' app/src/main/java/io/fluxreader/app/ui/ReaderFonts.kt
grep -q 'ActivityResultContracts.OpenDocument' app/src/main/java/io/fluxreader/app/ui/ReaderFonts.kt
grep -q 'AndroidTypeface.createFromFile' app/src/main/java/io/fluxreader/app/ui/ReaderFonts.kt
grep -q 'speedAutoFitText' app/src/main/java/io/fluxreader/app/data/Models.kt
grep -q 'speedFixedFontSize' app/src/main/java/io/fluxreader/app/data/Models.kt
grep -q 'speedHorizontalFocusFraction' app/src/main/java/io/fluxreader/app/data/Models.kt
grep -q 'speedFixedFontSizeV11' app/src/main/java/io/fluxreader/app/data/ReaderSettingsRepository.kt
grep -q 'speedHorizontalFocusFractionV11' app/src/main/java/io/fluxreader/app/data/ReaderSettingsRepository.kt
grep -q 'horizontalFocusPx' app/src/main/java/io/fluxreader/app/ui/SpeedViewportGeometry.kt
grep -q 'rememberTextMeasurer' app/src/main/java/io/fluxreader/app/ui/SpeedReaderScreen.kt
grep -q 'softWrap = false' app/src/main/java/io/fluxreader/app/ui/SpeedReaderScreen.kt
grep -q 'SpeedFocusGuide' app/src/main/java/io/fluxreader/app/ui/SpeedReaderScreen.kt
grep -q 'speedTextWidthFraction' app/src/main/java/io/fluxreader/app/data/ReaderSettingsRepository.kt
grep -q 'enum class LibraryViewMode' app/src/main/java/io/fluxreader/app/data/Models.kt
grep -q 'enum class LibrarySortMode' app/src/main/java/io/fluxreader/app/data/Models.kt
grep -q 'enum class LibraryGroupMode' app/src/main/java/io/fluxreader/app/data/Models.kt
grep -q 'enum class PageTurnAnimation' app/src/main/java/io/fluxreader/app/data/Models.kt
grep -q 'PageTurnAnimation.CURL' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt
grep -q 'rotationY = -pageAnimationDirection' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt
grep -q 'fullscreenTransitionEnabledV13' app/src/main/java/io/fluxreader/app/data/ReaderSettingsRepository.kt
grep -q 'fullscreenTransitionDurationMsV13' app/src/main/java/io/fluxreader/app/data/ReaderSettingsRepository.kt
grep -q 'fullscreenTransitionModifier' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt
grep -q 'Анимация полного экрана' app/src/main/java/io/fluxreader/app/ui/ReaderPreferences.kt
grep -q 'object SmartBookImport' app/src/main/java/io/fluxreader/app/data/SmartBookImport.kt
grep -q 'OpenMultipleDocuments' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt
grep -q 'LazyVerticalGrid' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt
grep -q 'editMetadata' app/src/main/java/io/fluxreader/app/data/LibraryRepository.kt
grep -q 'contentHash' app/src/main/java/io/fluxreader/app/data/LibraryRepository.kt
grep -q 'ReaderThemeMode.CATPPUCCIN' app/src/main/java/io/fluxreader/app/ui/FluxTheme.kt
grep -q 'PageTurnAnimation.SLIDE' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt
grep -q 'speedFocusBandEnabled' app/src/main/java/io/fluxreader/app/ui/SpeedReaderScreen.kt
grep -q 'enum class ThemeAppearance' app/src/main/java/io/fluxreader/app/data/Models.kt
grep -q 'ReaderThemeMode.ROSE_PINE_MOON' app/src/main/java/io/fluxreader/app/ui/FluxTheme.kt
grep -q 'ReaderThemeMode.CMX_PURPLE' app/src/main/java/io/fluxreader/app/ui/FluxTheme.kt
grep -q 'ReaderThemeMode.MONO -> monochromeDarkScheme(soft = false)' app/src/main/java/io/fluxreader/app/ui/FluxTheme.kt
grep -q 'ReaderThemeMode.MONO_SOFT -> monochromeLightScheme(soft = true)' app/src/main/java/io/fluxreader/app/ui/FluxTheme.kt
grep -q 'error = emphasis' app/src/main/java/io/fluxreader/app/ui/FluxTheme.kt
grep -q 'ReaderThemeMode.MONO -> "Ч/Б"' app/src/main/java/io/fluxreader/app/ui/ReaderPreferences.kt
grep -q 'fontWeight = if (active) FontWeight.Bold else FontWeight.Normal' app/src/main/java/io/fluxreader/app/ui/ReaderPreferences.kt
grep -q 'isSystemInDarkTheme' app/src/main/java/io/fluxreader/app/ui/FluxTheme.kt
grep -q 'enum class SpeedPacingMode' app/src/main/java/io/fluxreader/app/data/Models.kt
grep -q 'speedSmartChunking' app/src/main/java/io/fluxreader/app/data/ReaderSettingsRepository.kt
grep -q 'speedMicroPausesEnabled' app/src/main/java/io/fluxreader/app/data/Models.kt
grep -q 'speedSentencePauseMsV6' app/src/main/java/io/fluxreader/app/data/ReaderSettingsRepository.kt
grep -q 'fun rampedWpm' app/src/main/java/io/fluxreader/app/reader/SpeedReaderCursor.kt
grep -q 'sentenceWordCountEndingAt' app/src/main/java/io/fluxreader/app/reader/SpeedReaderCursor.kt
grep -q 'SpeedSettingsHub' app/src/main/java/io/fluxreader/app/ui/SpeedReaderScreen.kt
grep -q 'SpeedAdvancedSheet' app/src/main/java/io/fluxreader/app/ui/SpeedReaderScreen.kt
grep -q 'speedAdaptiveChunkSizeV7' app/src/main/java/io/fluxreader/app/data/ReaderSettingsRepository.kt
grep -q 'fun previousSentenceOffset' app/src/main/java/io/fluxreader/app/reader/SpeedReaderCursor.kt
grep -q 'fun nextSentenceOffset' app/src/main/java/io/fluxreader/app/reader/SpeedReaderCursor.kt
grep -q 'speedAutoRewindAfterPause' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'FullscreenSystemBarsEffect(isFullscreen)' app/src/main/java/io/fluxreader/app/ui/SpeedReaderScreen.kt
grep -q 'commonAbbreviation' app/src/main/java/io/fluxreader/app/reader/SpeedReaderCursor.kt
grep -q 'SpeedSettingsNavRow' app/src/main/java/io/fluxreader/app/ui/SpeedReaderScreen.kt
grep -q 'material-icons-extended' app/build.gradle.kts
grep -q 'ThemeAppearance.SYSTEM -> Icons.Default.BrightnessAuto' app/src/main/java/io/fluxreader/app/ui/ReaderPreferences.kt
grep -q 'LibraryCoverCache' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt
grep -q 'repairMissingCovers' app/src/main/java/io/fluxreader/app/data/LibraryRepository.kt
grep -q 'createPdfCover' app/src/main/java/io/fluxreader/app/data/LibraryRepository.kt
grep -q 'createGeneratedCover' app/src/main/java/io/fluxreader/app/data/LibraryRepository.kt
grep -q 'findEpubCover' app/src/main/java/io/fluxreader/app/data/SmartBookImport.kt
grep -q 'LibraryViewMode.COMPACT' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt
grep -q 'SettingsSection.APPEARANCE' app/src/main/java/io/fluxreader/app/ui/SettingsScreen.kt
grep -q 'scrollState.canScrollBackward' app/src/main/java/io/fluxreader/app/ui/SettingsScreen.kt
grep -q 'scrollState.canScrollForward' app/src/main/java/io/fluxreader/app/ui/SettingsScreen.kt
grep -q 'KeyboardArrowLeft' app/src/main/java/io/fluxreader/app/ui/SettingsScreen.kt
grep -q 'KeyboardArrowRight' app/src/main/java/io/fluxreader/app/ui/SettingsScreen.kt
grep -q 'HorizontalDivider()' app/src/main/java/io/fluxreader/app/ui/SettingsScreen.kt
grep -q 'Автор: n.revis' app/src/main/java/io/fluxreader/app/ui/SettingsScreen.kt
grep -q 'Версия \${BuildConfig.VERSION_NAME}' app/src/main/java/io/fluxreader/app/ui/SettingsScreen.kt
if find app/src/main -type f \( -iname '*jetbrains*.ttf' -o -iname '*jetbrains*.otf' -o -iname '*nerd*.ttf' -o -iname '*nerd*.otf' \) -print -quit | grep -q .; then
  echo "❌ Пользовательский JetBrains/Nerd font не должен быть встроен в исходники; используется импорт через SAF."
  exit 23
fi
if grep -nE '\?:[[:space:]]*if[[:space:]]*\(' app/src/main/java/io/fluxreader/app/AppViewModel.kt >/dev/null; then
  echo "❌ Риск nullable inference: Elvis непосредственно перед if в AppViewModel.kt"
  grep -nE '\?:[[:space:]]*if[[:space:]]*\(' app/src/main/java/io/fluxreader/app/AppViewModel.kt
  exit 16
fi
# current build must not carry older active runtime/build versions.
STALE_RUNTIME_RE='0\.2\.0-alpha\.4([^0-9.]|$)|0\.2\.0-alpha\.5\.1([^0-9.]|$)|0\.2\.0-alpha\.5\.2([^0-9.]|$)|0\.2\.0-alpha\.5\.3([^0-9.]|$)|0\.2\.0-alpha\.6([^0-9.]|$)|0\.2\.0-alpha\.7([^0-9.]|$)|0\.2\.0-alpha\.8([^0-9.]|$)|0\.2\.0-alpha\.9([^0-9.]|$)|0\.2\.0-alpha\.10([^0-9.]|$)|0\.2\.0-alpha\.11([^0-9.]|$)|0\.2\.0-alpha\.12([^0-9.]|$)|0\.2\.0-alpha\.13([^0-9.]|$)|0\.2\.0-alpha\.13\.1([^0-9.]|$)|0\.2\.0-alpha\.13\.2([^0-9.]|$)|0\.2\.0-alpha\.14([^0-9.]|$)|0\.2\.0-alpha\.15([^0-9.]|$)|0\.2\.0-alpha\.15\.1([^0-9.]|$)|0\.2\.0-alpha\.16([^0-9.]|$)|0\.2\.0-alpha\.16\.1([^0-9.]|$)|0\.2\.0-alpha\.16\.2([^0-9.]|$)|0\.2\.0-alpha\.17([^0-9.]|$)|0\.2\.0-alpha\.18([^0-9.]|$)|0\.2\.0-alpha\.19([^0-9.]|$)|0\.2\.0-alpha\.20([^0-9.]|$)|0\.2\.0-alpha\.21([^0-9.]|$)|0\.2\.0-alpha\.22([^0-9.]|$)|0\.2\.0-alpha\.23([^0-9.]|$)|0\.2\.0-alpha\.24([^0-9.]|$)|0\.2\.0-alpha\.24\.1([^0-9.]|$)|0\.2\.0-alpha\.25([^0-9.]|$)|0\.2\.0-alpha\.26([^0-9.]|$)|0\.2\.0-alpha\.26\.1([^0-9.]|$)|0\.2\.0-alpha\.27([^0-9.]|$)|0\.2\.0-alpha\.27\.1([^0-9.]|$)|0\.2\.0-alpha\.28([^0-9.]|$)|0\.2\.0-alpha\.29([^0-9.]|$)|0\.2\.0-alpha\.30([^0-9.]|$)|0\.2\.0-alpha\.30\.1([^0-9.]|$)|0\.2\.0-alpha\.31([^0-9.]|$)|0\.2\.0-alpha\.32([^0-9.]|$)|0\.2\.0-alpha\.33([^0-9.]|$)|0\.2\.0-alpha\.34([^0-9.]|$)|0\.2\.0-alpha\.35([^0-9.]|$)|0\.2\.0-alpha\.35\.1([^0-9.]|$)|0\.2\.0-alpha\.36([^0-9.]|$)|0\.2\.0-alpha\.37([^0-9.]|$)|0\.2\.0-alpha\.38([^0-9.]|$)|0\.2\.0-alpha\.39([^0-9.]|$)|20004|20006|20007|20008|20009|20010|20011|20012|20013|20014|20015|20017|20018|20019|20020|20021|20022|20023|20024|20025|20026|20027|20031|20033|20034|20036|20037|20039|20040|20041|20042|20043|20044|20045|20046|20047|20048|20049|20050|20051|20052'
for current_file in app/build.gradle.kts VERSION.txt app/src/main/assets/build-info.json scripts/build-debug.command scripts/install-debug.command scripts/bootstrap-build-apk.command; do
  if grep -Eq "$STALE_RUNTIME_RE" "$current_file"; then
    echo "❌ stale runtime marker: $current_file"
    exit 15
  fi
done

echo "✅ version=$VERSION code=$CODE / clean rewrite markers OK"

echo
echo "▶ OCR packs"
PACK_DIR="app/src/main/assets/tessdata"
PACKS=0
for lang in rus ukr eng; do
  if [[ -f "$PACK_DIR/$lang.traineddata" ]]; then
    size="$(wc -c < "$PACK_DIR/$lang.traineddata" | tr -d ' ')"
    if [[ "$size" -lt 100000 ]]; then
      echo "❌ $lang.traineddata слишком мал: $size bytes"
      exit 14
    fi
    echo "✅ $lang.traineddata — $size bytes"
    PACKS=$((PACKS + 1))
  fi
done
if (( PACKS == 0 )); then
  echo "ℹ️ Tesseract packs не вложены; ML Kit Latin fallback доступен. Для rus/ukr/eng: scripts/build-debug.command --with-ocr"
fi

echo
echo "▶ XML"
python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET
for path in Path('app/src/main').rglob('*.xml'):
    ET.parse(path)
print('✅ XML parse OK')
PY

echo
echo "▶ Shell syntax"
if command -v zsh >/dev/null 2>&1; then
  CHECK_SHELL=(zsh -n)
else
  CHECK_SHELL=(bash -n)
fi
for script in scripts/*.command; do
  "${CHECK_SHELL[@]}" "$script"
done
echo "✅ scripts syntax OK"

echo
echo "▶ Kotlin escape regression scan"
python3 - <<'PY'
from pathlib import Path
import re
bad=[]
# Catch the exact class of mistakes that broke 0.1.x: \s/\S/\d/\w in normal quoted Regex strings.
pat=re.compile(r'Regex\("(?:[^"\\]|\\.)*?(?<!\\)\\[sSdDwW]')
for p in Path('app/src/main/java').rglob('*.kt'):
    text=p.read_text(encoding='utf-8')
    for i,line in enumerate(text.splitlines(),1):
        if pat.search(line): bad.append(f'{p}:{i}: {line.strip()}')
if bad:
    print('\n'.join(bad))
    raise SystemExit('❌ suspicious Kotlin regex escape')
print('✅ no suspicious normal-string Regex escapes')
PY

echo
echo "▶ Kotlin test string-literal regression scan"
python3 - <<'PYKOTLIN'
from pathlib import Path

def unescaped_quote_count(line: str) -> int:
    count = 0
    escaped = False
    for ch in line:
        if escaped:
            escaped = False
            continue
        if ch == '\\':
            escaped = True
            continue
        if ch == '"':
            count += 1
    return count

bad = []
for path in Path('app/src/test/java').rglob('*.kt'):
    in_raw = False
    for lineno, line in enumerate(path.read_text(encoding='utf-8').splitlines(), 1):
        triple_count = line.count('"""')
        if triple_count:
            if triple_count % 2 == 1:
                in_raw = not in_raw
            continue
        if in_raw:
            continue
        if unescaped_quote_count(line) % 2 == 1:
            bad.append(f'{path}:{lineno}: {line.strip()}')
if bad:
    print('\n'.join(bad))
    raise SystemExit('❌ regular Kotlin test string crosses a physical newline or has an unmatched quote')
print('✅ test regular strings are line-safe')
PYKOTLIN

echo
echo "▶ Android/Compose compile regression guards"
if grep -Rqs 'XMLConstants.ACCESS_EXTERNAL_DTD\|XMLConstants.ACCESS_EXTERNAL_SCHEMA' app/src/main/java; then
  echo "❌ Android javax.xml.XMLConstants does not expose ACCESS_EXTERNAL_DTD/SCHEMA on this compile API"
  exit 24
fi
if grep -Rqs '^import androidx.compose.foundation.layout.weight$' app/src/main/java; then
  echo "❌ Do not explicitly import foundation.layout.weight; use RowScope/ColumnScope Modifier.weight"
  exit 25
fi
if grep -Rqs '^import androidx.compose.foundation.layout.calculateTopPadding$\|^import androidx.compose.foundation.layout.calculateBottomPadding$' app/src/main/java; then
  echo "❌ calculateTopPadding/calculateBottomPadding are PaddingValues members on this Compose line; do not import them"
  exit 26
fi
python3 - <<'PYPADDING'
from pathlib import Path

def padding_calls(text: str):
    marker = '.padding('
    pos = 0
    while True:
        start = text.find(marker, pos)
        if start < 0:
            return
        i = start + len(marker)
        depth = 1
        in_string = False
        escaped = False
        while i < len(text) and depth:
            ch = text[i]
            if in_string:
                if escaped:
                    escaped = False
                elif ch == '\\':
                    escaped = True
                elif ch == '"':
                    in_string = False
            else:
                if ch == '"':
                    in_string = True
                elif ch == '(':
                    depth += 1
                elif ch == ')':
                    depth -= 1
            i += 1
        yield start, text[start:i]
        pos = max(i, start + len(marker))

bad = []
for path in Path('app/src/main/java').rglob('*.kt'):
    text = path.read_text(encoding='utf-8')
    for start, call in padding_calls(text):
        if 'horizontal =' in call and ('top =' in call or 'bottom =' in call):
            line = text.count('\n', 0, start) + 1
            bad.append(f'{path}:{line}: mixed padding(horizontal=..., top/bottom=...) overload')
if bad:
    print('\n'.join(bad))
    raise SystemExit('❌ Compose padding overload regression: chain padding() calls or use start/end/top/bottom')
print('✅ Compose padding overload call-shapes OK')
PYPADDING
echo "✅ Android XML properties + Compose layout imports OK"

# alpha.16.1 Library card call-shape regression guard
if grep -nE 'CompactBookCard\(book, onOpen, onSpeed\)[[:space:]]*\{|BookGridCard\(book, onOpen, onSpeed\)[[:space:]]*\{' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt >/dev/null; then
  echo "❌ Book card trailing lambda binds after showFolderPath; pass onEdit as a named argument"
  grep -nE 'CompactBookCard\(book, onOpen, onSpeed\)[[:space:]]*\{|BookGridCard\(book, onOpen, onSpeed\)[[:space:]]*\{' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt
  exit 27
fi
grep -q 'CompactBookCard(' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt
grep -q 'BookGridCard(' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt
grep -q 'selectionMode = selectionMode' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt
echo "✅ Library card callback signatures OK"

echo
echo "▶ Reader navigation parity"
READER_SCREEN="app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt"
if grep -qE 'animateScrollBy|scrollBy\(amount\)|viewport \* 0\.88' "$READER_SCREEN"; then
  echo "❌ Reader gestures must not scroll a viewport; taps/swipes must use the same previous/next actions as buttons"
  exit 17
fi
grep -q 'navigateReaderPage(' "$READER_SCREEN" || {
  echo "❌ unified page controller is missing"
  exit 18
}
grep -q 'onPreviousPage = { previousPage() }' "$READER_SCREEN"
grep -q 'onNextPage = { nextPage() }' "$READER_SCREEN"
grep -q 'readerChapterAt' app/src/main/java/io/fluxreader/app/AppViewModel.kt
if grep -q 'viewModel.readerNext()\|viewModel.readerPrevious()' "$READER_SCREEN"; then
  echo "❌ Reader UI must not bypass the page controller"
  exit 19
fi
echo "✅ buttons / edge taps / horizontal swipes share the same page controller"

# alpha.14 library artwork / Settings / compact-home guards
grep -q 'LibraryViewMode.COMPACT' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt
grep -q 'private fun CompactBookCard' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt
grep -q 'SettingsSection.APPEARANCE' app/src/main/java/io/fluxreader/app/ui/SettingsScreen.kt
grep -q 'Text("Библиотека", fontWeight = FontWeight.SemiBold)' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt
grep -q 'compactFiltersExpanded' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt
grep -q 'DropdownMenu(' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt
grep -q 'private val FluxShapes = Shapes' app/src/main/java/io/fluxreader/app/ui/FluxTheme.kt
grep -Fq 'ifBlank { "Скорочтение" }' app/src/main/java/io/fluxreader/app/ui/SpeedReaderScreen.kt
if grep -q 'BuildConfig.VERSION_NAME' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt; then
  echo "❌ alpha.19: version badge must stay out of the library top bar"
  exit 31
fi
if grep -q 'ThemeAppearance.SYSTEM -> Icons.Default.BrightnessAuto' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt; then
  echo "❌ alpha.19: theme cycling must stay in Settings, not library chrome"
  exit 32
fi
grep -q 'repairMissingCovers' app/src/main/java/io/fluxreader/app/data/LibraryRepository.kt
grep -q 'createPdfCover' app/src/main/java/io/fluxreader/app/data/LibraryRepository.kt
grep -q 'createGeneratedCover' app/src/main/java/io/fluxreader/app/data/LibraryRepository.kt
grep -q 'findEpubCover' app/src/main/java/io/fluxreader/app/data/SmartBookImport.kt
grep -q 'epub2GuideCoverPageResolvesNestedImage' app/src/test/java/io/fluxreader/app/data/SmartBookImportTest.kt
echo "✅ alpha.14 cover repair / compact library / Settings guards OK"

# alpha.15 fullscreen / cutout / line-safe paging guards
grep -q 'WindowInsets.safeDrawing' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt
grep -q 'WindowInsets.displayCutout' app/src/main/java/io/fluxreader/app/ui/ImmersiveCameraClearance.kt
grep -q 'ReaderCutoutStatus' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt
grep -q 'FullscreenSystemBarsEffect(!chromeVisible)' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt
grep -q 'pageLineSnapPx' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt
grep -q 'snapStepPx' app/src/main/java/io/fluxreader/app/reader/ReaderPageMath.kt
grep -q 'next_snaps_inside_text_block_to_line_boundary' app/src/test/java/io/fluxreader/app/reader/ReaderPageMathTest.kt
grep -q 'ImmersiveCameraClearance.current()' app/src/main/java/io/fluxreader/app/ui/SpeedReaderScreen.kt
echo "✅ alpha.15 fullscreen safe-area / cutout / line-snap guards OK"

# alpha.16 folder hierarchy / typography guards
grep -q 'ActivityResultContracts.OpenDocumentTree' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt
grep -q 'DocumentsContract.buildChildDocumentsUriUsingTree' app/src/main/java/io/fluxreader/app/data/LibraryRepository.kt
grep -q 'folderPaths' app/src/main/java/io/fluxreader/app/data/Models.kt
grep -q 'LibraryGroupMode.FOLDER' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt
grep -q 'object LibraryFolderTree' app/src/main/java/io/fluxreader/app/data/LibraryFolderTree.kt
grep -q 'class LibraryFolderTreeTest' app/src/test/java/io/fluxreader/app/data/LibraryFolderTreeTest.kt
grep -q 'measuredLineTops' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt
grep -q 'result.getLineTop' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt
grep -q 'Hyphens.Auto' app/src/main/java/io/fluxreader/app/ui/ReaderPagination.kt
grep -q 'LineBreak.Paragraph' app/src/main/java/io/fluxreader/app/ui/ReaderPagination.kt
grep -q 'measured_line_tops_beat_estimated_line_height' app/src/test/java/io/fluxreader/app/reader/ReaderPageMathTest.kt
grep -q 'TextRole.LIST_ITEM' app/src/main/java/io/fluxreader/app/reader/EpubParser.kt
grep -q 'richTextDirect' app/src/main/java/io/fluxreader/app/reader/EpubParser.kt
grep -q 'preservesAnnotationParagraphAndNestedListsWithoutFlattening' app/src/test/java/io/fluxreader/app/reader/EpubParserTest.kt
echo "✅ alpha.16 folder hierarchy / measured typography / EPUB structure guards OK"

# alpha.16.2 fullscreen viewport / unified-import guards
READER_SCREEN="app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt"
LIBRARY_SCREEN="app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt"
# alpha.79 supersedes the old fullscreen Column + double camera/status rows:
# the camera-lane HUD must be overlaid over a full-height measured reader.
grep -q 'The upper camera lane is an overlay' "$READER_SCREEN"
grep -q 'modifier = pageAnimationModifier,' "$READER_SCREEN"
grep -q 'val visibleHudHeight = if (safeFrameAlreadyApplied) ImmersiveCameraClearance.current()' "$READER_SCREEN"
python3 scripts/critic_pixel_immersive.py
python3 scripts/check_reader_fullscreen_scope.py --self-test
python3 scripts/test_pixel_immersive_policy.py
# alpha.46 supersedes the old fullscreen line-snap; viewport separation remains guarded here.
grep -q 'top = if (isFullscreen) 2.dp else 0.dp' "$READER_SCREEN"
grep -q 'var importChooserVisible by rememberSaveable' "$LIBRARY_SCREEN"
grep -q 'LibraryActionButton(Icons.AutoMirrored.Filled.InsertDriveFile, "Книги"' "$LIBRARY_SCREEN"
grep -q 'LibraryActionButton(Icons.Default.FolderOpen, "Папка"' "$LIBRARY_SCREEN"
if grep -q 'SmallFloatingActionButton' "$LIBRARY_SCREEN"; then
  echo "❌ Library must use one unified Add FAB; folder/file choice belongs in the import dialog"
  exit 28
fi
if [[ "$(grep -c 'FloatingActionButton(onClick = { importChooserVisible = true })' "$LIBRARY_SCREEN")" -ne 1 ]]; then
  echo "❌ Unified library import FAB is missing or duplicated"
  exit 29
fi
echo "✅ alpha.16.2 dedicated fullscreen viewport / camera HUD / unified import guards OK"

# alpha.17 screen-prepagination / character-anchor guards
PAGINATION="app/src/main/java/io/fluxreader/app/ui/ReaderPagination.kt"
PAGINATION_MODEL="app/src/main/java/io/fluxreader/app/ui/PreparedReaderPagination.kt"
grep -q 'sealed interface PreparedReaderPageItem' "$PAGINATION_MODEL"
grep -q 'suspend fun prepareReaderChapterPagination' "$PAGINATION"
grep -q 'textMeasurer.measure' "$PAGINATION"
grep -q 'getLineBottom' "$PAGINATION"
grep -q 'getLineEnd' "$PAGINATION"
grep -q 'fun buildReaderTextStyle' "$PAGINATION"
grep -q 'ReaderPreparedPageContent' "$READER_SCREEN"
grep -q 'rememberTextMeasurer' "$READER_SCREEN"
grep -q 'PreparedReaderPageItem.Image' "$READER_SCREEN"
grep -q 'readerBlockCharOffset' app/src/main/java/io/fluxreader/app/data/Models.kt
grep -q 'readerBlockCharOffset' app/src/main/java/io/fluxreader/app/data/LibraryRepository.kt
grep -q 'initialBlockCharOffset' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'blockCharOffset = page.startCharOffset' "$READER_SCREEN"
if grep -q 'userScrollEnabled = settings.navigationMode == ReaderNavigationMode.SCROLL' "$READER_SCREEN"; then
  echo "❌ SCREEN_PAGING must not use LazyColumn as its page-boundary engine"
  exit 30
fi
echo "✅ alpha.17 measured screen pre-pagination / character anchors / image-page guards OK"


# alpha.18 managed-library file/folder operations guards
LIBRARY_REPO="app/src/main/java/io/fluxreader/app/data/LibraryRepository.kt"
LIBRARY_SCREEN="app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt"
grep -q 'private val foldersFile = File(context.filesDir, "library-folders.json")' "$LIBRARY_REPO"
grep -q 'val folders: StateFlow<List<String>>' "$LIBRARY_REPO"
grep -q 'fun createFolder(' "$LIBRARY_REPO"
grep -q 'fun renameFolder(' "$LIBRARY_REPO"
grep -q 'fun moveFolder(' "$LIBRARY_REPO"
grep -q 'fun copyFolder(' "$LIBRARY_REPO"
grep -q 'fun deleteFolder(' "$LIBRARY_REPO"
grep -q 'fun copyBookToFolder(' "$LIBRARY_REPO"
grep -q 'fun moveBookToFolder(' "$LIBRARY_REPO"
grep -q 'suspend fun duplicateBook(' "$LIBRARY_REPO"
grep -q 'suspend fun exportBook(' "$LIBRARY_REPO"
grep -q 'BookActionsDialog' "$LIBRARY_SCREEN"
grep -q 'FolderActionsDialog' "$LIBRARY_SCREEN"
grep -q 'FolderDestinationDialog' "$LIBRARY_SCREEN"
grep -q 'ActivityResultContracts.CreateDocument' "$LIBRARY_SCREEN"
grep -q 'explicitEmptyFoldersRemainVisible' app/src/test/java/io/fluxreader/app/data/LibraryFolderTreeTest.kt
grep -q 'prefixRemapPreservesNestedSuffix' app/src/test/java/io/fluxreader/app/data/LibraryFolderTreeTest.kt
grep -q 'uniqueSiblingNameAvoidsCaseInsensitiveConflicts' app/src/test/java/io/fluxreader/app/data/LibraryFolderTreeTest.kt
echo "✅ alpha.18 managed library file/folder operations guards OK"

# alpha.20 no-clipping reader / stable speed-fixation guards
MODELS="app/src/main/java/io/fluxreader/app/data/Models.kt"
SETTINGS_REPO="app/src/main/java/io/fluxreader/app/data/ReaderSettingsRepository.kt"
SPEED_SCREEN="app/src/main/java/io/fluxreader/app/ui/SpeedReaderScreen.kt"
SPEED_CURSOR="app/src/main/java/io/fluxreader/app/reader/SpeedReaderCursor.kt"
grep -q 'val hyphenationEnabled: Boolean = false' "$MODELS"
grep -q 'hyphenationEnabledV8' "$SETTINGS_REPO"
grep -q 'Hyphens.None' "$PAGINATION"
grep -q 'ReaderPageGridCalculator.bottomSafetyPx' "$PAGINATION"
grep -q 'enum class SpeedLowerSegmentMode' "$MODELS"
grep -q 'speedLowerSegmentModeV8' "$SETTINGS_REPO"
grep -q 'PersistentSpeedSegment' "$SPEED_SCREEN"
grep -q 'lowerContentReservePx' "$SPEED_SCREEN"
grep -q 'weakTrailingWords' "$SPEED_CURSOR"
grep -q 'smartPhraseDoesNotLeaveShortConnectorDangling' app/src/test/java/io/fluxreader/app/reader/SpeedReaderCursorTest.kt
echo "✅ alpha.20/28 no-hyphen default / measured bottom reserve / speed guide / persistent segment guards OK"

# alpha.21 continuity / typographic pagination / speed geometry guards
CONTENT_REPO="app/src/main/java/io/fluxreader/app/reader/BookContentRepository.kt"
PAGINATION_POLICY="app/src/main/java/io/fluxreader/app/ui/ReaderPaginationPolicy.kt"
SPEED_GEOMETRY="app/src/main/java/io/fluxreader/app/ui/SpeedViewportGeometry.kt"
grep -q 'class PreparedReaderPaginationCache' "$PAGINATION_MODEL"
grep -q 'object ReaderPaginationPolicy' "$PAGINATION_POLICY"
grep -q 'ReaderPaginationPolicy.moveSingleOpeningLine' "$PAGINATION"
grep -q 'ReaderPaginationPolicy.keepHeadingWithBody' "$PAGINATION"
grep -q 'ReaderPaginationPolicy.pullSingleTrailingLine' "$PAGINATION"
grep -q 'paginationCache.put(prepared)' "$READER_SCREEN"
grep -q 'prefetchAdjacentChapters' "$CONTENT_REPO"
grep -q 'scheduleReaderPrefetch' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'effectiveVerticalPosition' "$SPEED_SCREEN"
grep -q 'pauseContextHeightPx' "$SPEED_SCREEN"
grep -q 'clampFixationFraction' "$SPEED_GEOMETRY"
grep -q 'paginationCacheIsLruBounded' app/src/test/java/io/fluxreader/app/ui/ReaderPaginationPolicyTest.kt
echo "✅ alpha.21 chapter prefetch / pagination LRU / widow-orphan / speed geometry guards OK"

# alpha.22/28 complete-line measured paging guards
PAGE_GRID="app/src/main/java/io/fluxreader/app/ui/ReaderPageGrid.kt"
PAGE_GRID_TEST="app/src/test/java/io/fluxreader/app/ui/ReaderPageGridTest.kt"
grep -q 'object ReaderPageGridCalculator' "$PAGE_GRID"
grep -q 'val nominalLineBudget: Int' "$PAGE_GRID"
grep -q 'val contentHeightPx: Int' "$PAGE_GRID"
grep -q 'measurementLineCap' "$PAGE_GRID"
grep -q 'ReaderPageGridCalculator.calculate' "$PAGINATION"
grep -q 'ReaderPageGridCalculator.measurementLineCap' "$PAGINATION"
grep -Fq 'ceil(layout.getLineBottom(line).toDouble()).toInt() <= availableHeightPx' "$PAGINATION"
if grep -q 'clampLastFittingLine\|remainingLines(lineBudget\|usedLineCount >= lineBudget' "$PAGINATION"; then
  echo "❌ Reader paging regressed to a rigid nominal line cap; measured line bottoms must choose the page boundary"
  exit 38
fi
grep -q 'keepsWholeSafeViewportInsteadOfDiscardingRemainder' "$PAGE_GRID_TEST"
grep -q 'measurementCapProvidesHeadroomWithoutBecomingTheBoundary' "$PAGE_GRID_TEST"
echo "✅ alpha.28 measured whole-line fill / no wasted nominal remainder guards OK"

# alpha.23 Reedy-style RSVP / tap-to-start / chapter-progress / screen-awake guards
CHAPTER_SPEED_OFFSET="app/src/main/java/io/fluxreader/app/reader/ChapterSpeedOffset.kt"
grep -q 'localView.keepScreenOn = true' "$SPEED_SCREEN"
grep -q 'currentLeftPx = focusTargetPx - focusCenterPx' "$SPEED_SCREEN"
grep -q 'followingTokens(chapterText, chunkEndOffset' "$SPEED_SCREEN"
grep -q 'speedReedyFlowEnabled' app/src/main/java/io/fluxreader/app/data/Models.kt
grep -q 'enum class SpeedProgressMode' app/src/main/java/io/fluxreader/app/data/Models.kt
grep -q 'speedProgressModeV9' app/src/main/java/io/fluxreader/app/data/ReaderSettingsRepository.kt
grep -q 'fun openSpeedAt' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'object ChapterSpeedOffset' "$CHAPTER_SPEED_OFFSET"
grep -qE 'Скорочтение с выбранного слова|Скорочтение отсюда' "$READER_SCREEN"
grep -q 'SpeedReaderCursor.wordStartOffset' "$READER_SCREEN"
grep -q 'tappedOffsetSnapsToWholeWordAndPeripheralContextStartsAfterCurrentWord' app/src/test/java/io/fluxreader/app/reader/SpeedReaderCursorTest.kt
grep -q 'mapsVisibleBlockCharacterIntoPlainText' app/src/test/java/io/fluxreader/app/reader/ChapterSpeedOffsetTest.kt
echo "✅ alpha.23+ Reedy-style RSVP / stable ORP / tapped-word start / chapter progress / keep-awake guards OK"

# alpha.24 Reedy rebuild / semantic media / chapter typography / grouped settings guards
SPEED_DOCUMENT="app/src/main/java/io/fluxreader/app/reader/SpeedReaderDocument.kt"
grep -q 'data class SpeedMediaCue' "$SPEED_DOCUMENT"
grep -q 'fun readableOffset' "$SPEED_DOCUMENT"
grep -q 'suspend fun speedDocument' "$CONTENT_REPO"
grep -q 'SpeedMediaFrame' "$SPEED_SCREEN"
grep -q 'SpeedChapterIntro' "$SPEED_SCREEN"
grep -q 'SpeedPausePreview' "$SPEED_SCREEN"
grep -q 'SpeedSettingsPanel { NONE, HUB, DISPLAY, BEHAVIOR, VOICE, EFFECTS, ADVANCED }' "$SPEED_SCREEN"
grep -q 'speedImagePauseMsV10' "$SETTINGS_REPO"
grep -q 'speedVolumeKeysEnabledV10' "$SETTINGS_REPO"
grep -Fq 'valueRange = 100f..3000f' "$SPEED_SCREEN"
grep -q 'KEYCODE_VOLUME_UP' app/src/main/java/io/fluxreader/app/MainActivity.kt
grep -q 'TextRole.TITLE, TextRole.HEADING -> if (settings.centerChapterHeadings) TextAlign.Center else TextAlign.Start' "$PAGINATION"
grep -Fq '"h1", "title" -> {' app/src/main/java/io/fluxreader/app/reader/EpubParser.kt
grep -Fq 'Regex("(?iu)^\\s*глава\\s+[0-9ivxlcdm]+\\b")' app/src/main/java/io/fluxreader/app/reader/Fb2Parser.kt
grep -q 'label = { Text("JetBrains") }' app/src/main/java/io/fluxreader/app/ui/ReaderPreferences.kt
grep -q 'hyphenationEnabled = true' app/src/main/java/io/fluxreader/app/ui/ReaderPreferences.kt
grep -q 'pauseTextWindowKeepsAbsoluteOffsetsForTapToSeek' app/src/test/java/io/fluxreader/app/reader/SpeedReaderCursorTest.kt
grep -q 'readableOffsetSkipsConsecutiveChapterHeadingsButNotBody' app/src/test/java/io/fluxreader/app/reader/SpeedReaderDocumentTest.kt
# alpha.24.1: BoxWithConstraints scope regression — capture constraints before entering Column/Image DSL scopes.
grep -q 'val viewportMaxHeight = maxHeight' "$SPEED_SCREEN"
if grep -q '\.height((maxHeight \* 0\.[46][24]f)' "$SPEED_SCREEN"; then
  echo "❌ SpeedMediaFrame uses BoxWithConstraints.maxHeight through a nested implicit DSL receiver"
  exit 33
fi
echo "✅ alpha.24.1 Reedy rebuild / media pauses / chapter typography / grouped settings / BoxWithConstraints scope guard OK"

# alpha.25 left-biased stable fixation / fixed RSVP font guards
grep -q 'val speedFixedFontSize: Boolean = true' "$MODELS"
grep -q 'val speedHorizontalFocusFraction: Float = 0.30f' "$MODELS"
grep -q 'speedFixedFontSizeV11' "$SETTINGS_REPO"
grep -q 'speedHorizontalFocusFractionV11' "$SETTINGS_REPO"
grep -q 'horizontalFocusPx(size.width, horizontalFocusPosition)' "$SPEED_SCREEN"
grep -q 'val currentLeftPx = focusTargetPx - focusCenterPx' "$SPEED_SCREEN"
grep -q 'settings.speedFixedFontSize -> baseSize' "$SPEED_SCREEN"
grep -q 'speedHorizontalFocusDefaultsToLeftThird' app/src/test/java/io/fluxreader/app/ui/ReaderPaginationPolicyTest.kt
echo "✅ alpha.25 left-biased fixation / fixed RSVP font / shared guide geometry guards OK"

# alpha.26 reader settings hierarchy / independent margins / TOC title / speed font-size guards
grep -q 'val topPaddingDp: Float = 20f' "$MODELS"
grep -q 'val bottomPaddingDp: Float = 20f' "$MODELS"
grep -q 'topPaddingDpV12' "$SETTINGS_REPO"
grep -q 'bottomPaddingDpV12' "$SETTINGS_REPO"
grep -q 'chapterTitleScaleV12' "$SETTINGS_REPO"
grep -q 'chapterHeadingScaleV12' "$SETTINGS_REPO"
grep -q 'ReaderSettingsPanel { MAIN, ADVANCED }' "$READER_SCREEN"
grep -q 'includeNavigationAdvanced = false' "$READER_SCREEN"
grep -q 'includeAdvancedText = false' "$READER_SCREEN"
grep -q 'overlay = ReaderOverlay.TOC' "$READER_SCREEN"
grep -q 'PreferenceSlider("Поле сверху"' app/src/main/java/io/fluxreader/app/ui/ReaderPreferences.kt
grep -q 'PreferenceSlider("Поле снизу"' app/src/main/java/io/fluxreader/app/ui/ReaderPreferences.kt
grep -q 'topPaddingPx = topPaddingPx' "$PAGINATION"
grep -q 'bottomPaddingPx = bottomPaddingPx' "$PAGINATION"
grep -q 'topAndBottomPaddingAreIndependent' "$PAGE_GRID_TEST"
grep -q 'SettingsValueSlider("Размер текста", settings.speedFontSizeSp' "$SPEED_SCREEN"
if [[ "$(grep -c 'SettingsValueSlider("Размер текста", settings.speedFontSizeSp' "$SPEED_SCREEN")" -ne 1 ]]; then
  echo "❌ Speed Reader font size must have one primary control in the main settings hub"
  exit 34
fi
echo "✅ alpha.26 main/advanced settings split / independent margins / title TOC / speed font-size guards OK"
echo "✅ alpha.27 monochrome palettes / grayscale semantic colors / weight-only emphasis guards OK"
echo "✅ alpha.29 paper-turn / fullscreen transition guards OK"
echo "✅ alpha.30 settings tab overflow / about guards OK"

# alpha.31 minimal settings arrows / deprecation cleanup / memory pass 1
grep -q 'Icons.AutoMirrored.Filled.KeyboardArrowLeft' app/src/main/java/io/fluxreader/app/ui/SettingsScreen.kt
grep -q 'Icons.AutoMirrored.Filled.KeyboardArrowRight' app/src/main/java/io/fluxreader/app/ui/SettingsScreen.kt
if grep -q 'surfaceContainerHigh' app/src/main/java/io/fluxreader/app/ui/SettingsScreen.kt; then
  echo "❌ alpha.31: settings overflow arrows must stay borderless/minimal"
  exit 43
fi
if grep -Rqs 'foundation.text.ClickableText' app/src/main/java/io/fluxreader/app/ui; then
  echo "❌ alpha.31: deprecated ClickableText remains"
  exit 44
fi
grep -q 'foundation.text.BasicText' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt
grep -q 'foundation.text.BasicText' app/src/main/java/io/fluxreader/app/ui/SpeedReaderScreen.kt
grep -q 'Icons.AutoMirrored.Filled.InsertDriveFile' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt
grep -q 'Icons.AutoMirrored.Filled.DriveFileMove' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt
grep -q 'object ReaderMemoryBudget' app/src/main/java/io/fluxreader/app/reader/ReaderMemoryBudget.kt
grep -q 'ReaderMemoryBudget.embeddedImageCacheKb' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'retainImageCacheForBook' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'trimReaderCachesForLibrary' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'chapterCacheMaxKb = ReaderMemoryBudget.chapterCacheKb' app/src/main/java/io/fluxreader/app/reader/BookContentRepository.kt
grep -q 'trimSessionCaches' app/src/main/java/io/fluxreader/app/reader/BookContentRepository.kt
grep -q 'ReaderMemoryBudget.PDF_TEXT_CACHE_CHARS' app/src/main/java/io/fluxreader/app/reader/PdfTextEngine.kt
grep -q 'ReaderMemoryBudget.OCR_TEXT_CACHE_CHARS' app/src/main/java/io/fluxreader/app/reader/OcrEngine.kt
grep -q 'ReaderMemoryBudget.PDF_MAX_BITMAP_PIXELS' app/src/main/java/io/fluxreader/app/reader/PdfRendererEngine.kt
grep -q 'DisposableEffect(state.bitmap)' app/src/main/java/io/fluxreader/app/ui/PdfReaderScreen.kt
grep -q 'maxSidePx: Int = 480' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt
[[ -f scripts/profile-memory.command ]]
grep -q 'dumpsys meminfo' scripts/profile-memory.command
echo "✅ alpha.31 minimal arrows / modern text taps / bounded reader memory guards OK"

# alpha.32 settings rails / long-press library management / enlarged existing icon
grep -q 'val leadingRail = if (scrollState.canScrollBackward) 34.dp else 0.dp' app/src/main/java/io/fluxreader/app/ui/SettingsScreen.kt
grep -q 'val trailingRail = if (scrollState.canScrollForward) 34.dp else 0.dp' app/src/main/java/io/fluxreader/app/ui/SettingsScreen.kt
grep -q '.padding(start = leadingRail, end = trailingRail)' app/src/main/java/io/fluxreader/app/ui/SettingsScreen.kt
if [[ "$(grep -c 'combinedClickable(' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt)" -lt 4 ]]; then
  echo "❌ alpha.32: long-press actions must cover folders and all book card modes"
  exit 45
fi
grep -q 'onLongClick = onManage' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt
grep -q 'M18,18h31' app/src/main/res/drawable/ic_flux_reader.xml
grep -q 'android:fillColor="#111318" android:pathData="M0,0h108v108h-108z"' app/src/main/res/drawable/ic_flux_reader.xml
echo "✅ alpha.32 settings rails / long-press file management / enlarged existing icon guards OK"

# alpha.33 multi-select / batched library operations / undo guards
grep -q 'var selectedBookIds by remember' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt
grep -q 'Icons.Default.SelectAll' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt
grep -q 'var selectionMode by rememberSaveable' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt
grep -q 'fun copyBooksToFolder' app/src/main/java/io/fluxreader/app/data/LibraryRepository.kt
grep -q 'fun moveBooksToFolder' app/src/main/java/io/fluxreader/app/data/LibraryRepository.kt
grep -q 'fun deleteBooks' app/src/main/java/io/fluxreader/app/data/LibraryRepository.kt
grep -q 'fun undoLibraryOperation' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'canUndoLibraryOperation' app/src/main/java/io/fluxreader/app/ui/FluxReaderApp.kt
python3 - <<'PYALPHA33'
from pathlib import Path
text = Path('app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt').read_text()
start = text.index('private fun BookListCard(')
end = text.index('@Composable\nprivate fun SelectionBadge', start)
chunk = text[start:end]
needle = 'IconButton(onClick = { onSpeed(book.id) }) { Icon(Icons.Default.PlayArrow, contentDescription = "Скорочтение") }'
if chunk.count(needle) > 1:
    raise SystemExit('❌ alpha.33: list card must not duplicate the speed-reader action')
PYALPHA33
echo "✅ alpha.33 multi-select / batch operations / undo / duplicate-control guards OK"

# alpha.34 reader viewport / precise progress / cutout-lane guards
grep -q 'fun bottomSafetyPx' app/src/main/java/io/fluxreader/app/ui/ReaderPageGrid.kt
grep -Fq 'lineHeight * 0.28f' app/src/main/java/io/fluxreader/app/ui/ReaderPageGrid.kt
grep -q 'minimumSafetyPx = with(density) { 6.dp.roundToPx() }' "$PAGINATION"
grep -q 'val topGuard = if (isFullscreen) 2.dp else 3.dp' "$READER_SCREEN"
grep -q 'val bottomGuard = if (isFullscreen) 3.dp else 5.dp' "$READER_SCREEN"
grep -q 'Spacer(Modifier.width(96.dp))' "$READER_SCREEN"
grep -q 'object ReaderProgressMath' app/src/main/java/io/fluxreader/app/reader/ReaderProgressMath.kt
grep -q 'ReaderProgressMath.chapterFraction' app/src/main/java/io/fluxreader/app/AppViewModel.kt
if grep -q 'target.toFloat() / outline.chapters.lastIndex.toFloat()' app/src/main/java/io/fluxreader/app/AppViewModel.kt; then
  echo "❌ alpha.34: last chapter must not jump straight to 100%"
  exit 46
fi
grep -q 'class ReaderProgressMathTest' app/src/test/java/io/fluxreader/app/reader/ReaderProgressMathTest.kt
echo "✅ alpha.34 reader viewport / clipping reserve / precise progress / cutout-lane guards OK"

# alpha.35 large-library performance / async covers / adaptive prefetch guards
LIBRARY_COVER_CACHE="app/src/main/java/io/fluxreader/app/data/LibraryCoverCache.kt"
LIBRARY_SEARCH_INDEX="app/src/main/java/io/fluxreader/app/data/LibrarySearchIndex.kt"
grep -q 'object LibraryCoverCache' "$LIBRARY_COVER_CACHE"
grep -q 'withContext(Dispatchers.IO)' "$LIBRARY_COVER_CACHE"
grep -q 'ReaderMemoryBudget.libraryCoverCacheKb' "$LIBRARY_COVER_CACHE"
grep -q 'produceState(' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt
grep -q 'LibrarySearchIndex.build' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt
grep -q 'object LibrarySearchIndex' "$LIBRARY_SEARCH_INDEX"
grep -q 'fun libraryCoverCacheKb' app/src/main/java/io/fluxreader/app/reader/ReaderMemoryBudget.kt
grep -q 'fun chapterPrefetchRadius' app/src/main/java/io/fluxreader/app/reader/ReaderMemoryBudget.kt
grep -q 'radius = readerPrefetchRadius' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'val next = current.toMutableList()' app/src/main/java/io/fluxreader/app/data/LibraryRepository.kt
if grep -A18 -F 'private fun updatePosition(' app/src/main/java/io/fluxreader/app/data/LibraryRepository.kt | grep -q 'sortedByDescending'; then
  echo "❌ alpha.35: hot reader position update must not resort the whole library"
  exit 47
fi
if grep -q 'private fun decodeCoverPreview' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt; then
  echo "❌ alpha.35: cover decoding must not run synchronously from the Compose library screen"
  exit 48
fi
grep -q 'class LibrarySearchIndexTest' app/src/test/java/io/fluxreader/app/data/LibrarySearchIndexTest.kt
echo "✅ alpha.35 large-library update / async bounded covers / prebuilt search / adaptive prefetch guards OK"

# alpha.28 stronger headings / local release install guards
grep -q 'TextRole.TITLE -> typography.headlineMedium.copy' "$PAGINATION"
grep -q 'fontWeight = FontWeight.ExtraBold' "$PAGINATION"
grep -q 'fontSynthesis = FontSynthesis.Weight' "$PAGINATION"
grep -q 'normalizeChapterHeadingRoles' app/src/main/java/io/fluxreader/app/reader/EpubParser.kt
grep -q 'promotesCssOnlyChapterLabelAndUppercaseTitle' app/src/test/java/io/fluxreader/app/reader/EpubParserTest.kt
grep -q 'buildTypes {' app/build.gradle.kts
grep -q 'signingConfig = signingConfigs.getByName("debug")' app/build.gradle.kts
[[ -f scripts/build-release.command ]]
[[ -f scripts/install-release.command ]]
grep -q 'scripts/install-release.command' install.command
grep -q ':app:assembleRelease' scripts/build-release.command
echo "✅ alpha.28 bold chapter headings / non-debuggable local-release install guards OK"

echo
# alpha.36 persistent parsed EPUB/FB2 cache + streamed XML guards
PARSED_CACHE="app/src/main/java/io/fluxreader/app/reader/ParsedBookDiskCache.kt"
CONTENT_REPO="app/src/main/java/io/fluxreader/app/reader/BookContentRepository.kt"
EPUB_PARSER="app/src/main/java/io/fluxreader/app/reader/EpubParser.kt"
[[ -f "$PARSED_CACHE" ]]
grep -q 'class ParsedBookDiskCache' "$PARSED_CACHE"
grep -q 'DEFAULT_MAX_BYTES = 160L' "$PARSED_CACHE"
grep -q 'fun sourceToken' "$PARSED_CACHE"
grep -q 'atomicWrite' "$PARSED_CACHE"
grep -q 'private val parseLocks = Array(PARSE_LOCK_STRIPES)' "$CONTENT_REPO"
grep -q 'parsedDiskCache.readOutline' "$CONTENT_REPO"
grep -q 'parsedDiskCache.readChapter' "$CONTENT_REPO"
grep -q 'runCatching { parsedDiskCache.writeChapter' "$CONTENT_REPO"
grep -q 'private fun parseXml(input: InputStream)' "$EPUB_PARSER"
if grep -q 'parseXml(.*readBytes' "$EPUB_PARSER"; then
  echo "❌ alpha.36: EPUB XML must stream directly into the parser"
  exit 49
fi
grep -q 'class ParsedBookDiskCacheTest' app/src/test/java/io/fluxreader/app/reader/ParsedBookDiskCacheTest.kt
echo "✅ alpha.36 persistent parsed cache / deduplicated loads / streamed EPUB XML guards OK"

# alpha.37 reader hot-path / image decode / PDF bitmap lifecycle guards
PAGINATION_WINDOW="app/src/main/java/io/fluxreader/app/ui/ReaderPaginationWindowPolicy.kt"
PDF_RENDERER="app/src/main/java/io/fluxreader/app/reader/PdfRendererEngine.kt"
[[ -f "$PAGINATION_WINDOW" ]]
grep -q 'MIN_WINDOW_CHARS = 4_096' "$PAGINATION_WINDOW"
grep -q 'MAX_WINDOW_CHARS = 32_768' "$PAGINATION_WINDOW"
grep -q 'ReaderPaginationWindowPolicy.initialWindowChars' "$PAGINATION"
grep -q 'ReaderPaginationWindowPolicy.nextWindowChars' "$PAGINATION"
grep -q 'val slicedBlock = remember' "$READER_SCREEN"
grep -q 'val style = remember(block.role, settings' "$READER_SCREEN"
grep -q 'private val imageDecodeSemaphore = Semaphore' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'private val imageDecodeLocks = Array(32)' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'book.contentHash.*sourceFile.length().*sourceFile.lastModified()' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'requestGeneration == pdfRenderGeneration' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'pdfPendingWidthPx == widthPx' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'private val pdfRenderMutex = Mutex()' "$CONTENT_REPO"
grep -q 'currentCoroutineContext().ensureActive()' "$CONTENT_REPO"
grep -q 'rendered.bitmap.recycle()' "$CONTENT_REPO"
grep -q 'fun pdfMaxBitmapPixels' app/src/main/java/io/fluxreader/app/reader/ReaderMemoryBudget.kt
grep -q 'maxBitmapPixels: Long = ReaderMemoryBudget.PDF_MAX_BITMAP_PIXELS' "$PDF_RENDERER"
grep -q 'class ReaderPaginationWindowPolicyTest' app/src/test/java/io/fluxreader/app/ui/ReaderPaginationWindowPolicyTest.kt
echo "✅ alpha.37 adaptive pagination / bounded image decode / cancellation-safe PDF guards OK"

# alpha.38 instant anchor preview / background completion / aggressive route trim guards
grep -q 'prepareReaderAnchorPreviewPagination' app/src/main/java/io/fluxreader/app/ui/ReaderPagination.kt
grep -q 'val isComplete: Boolean = true' app/src/main/java/io/fluxreader/app/ui/PreparedReaderPagination.kt
grep -q 'if (value.isComplete) entries\[value.layoutKey\] = value' app/src/main/java/io/fluxreader/app/ui/PreparedReaderPagination.kt
grep -q 'preparedPagination?.isComplete != next.isComplete' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt
grep -q 'pagination.isComplete && state.chapterIndex > 0' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt
grep -q 'else if (pagination.isComplete)' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt
grep -q 'delay(180L)' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'readerChapterForPaginationPrewarm' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'kotlinx.coroutines.delay(120L)' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt
grep -q 'paginationCache.put(nextPrepared)' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt
grep -q 'imageCache.maxSize() / 8' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'chapterCacheMaxKb / 4' app/src/main/java/io/fluxreader/app/reader/BookContentRepository.kt
grep -q 'ReaderPreviewPolicyTest' app/src/test/java/io/fluxreader/app/ui/ReaderPreviewPolicyTest.kt
echo "✅ alpha.38 instant-anchor preview / background completion / route-memory trim guards OK"

# alpha.39 duplicate-control audit / shared fullscreen system UI / memory pressure guards
UI_WINDOW_EFFECTS="app/src/main/java/io/fluxreader/app/ui/UiWindowEffects.kt"
[[ -f "$UI_WINDOW_EFFECTS" ]]
grep -q 'internal fun FullscreenSystemBarsEffect' "$UI_WINDOW_EFFECTS"
grep -Eq 'FullscreenSystemBarsEffect\((isFullscreen|!chromeVisible)\)' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt
grep -q 'FullscreenSystemBarsEffect(isFullscreen)' app/src/main/java/io/fluxreader/app/ui/SpeedReaderScreen.kt
if [[ $(grep -R -l 'hide(WindowInsetsCompat.Type.systemBars())' app/src/main/java/io/fluxreader/app/ui | wc -l | tr -d ' ') != "1" ]]; then
  echo "❌ alpha.39: fullscreen system-bar implementation duplicated"
  exit 50
fi
if grep -q 'MinimalFullscreenControls' app/src/main/java/io/fluxreader/app/ui/SpeedReaderScreen.kt; then
  echo "❌ alpha.39: duplicate Speed Reader fullscreen playback controls returned"
  exit 51
fi
if grep -A70 -F 'private fun ReaderFullscreenControls(' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt | grep -q 'Icons.Default.Chevron'; then
  echo "❌ alpha.39: fullscreen edge buttons duplicate the existing tap zones"
  exit 52
fi
if grep -q 'SettingsSection.QUICK' app/src/main/java/io/fluxreader/app/ui/SettingsScreen.kt; then
  echo "❌ alpha.39: duplicate Quick settings section returned"
  exit 53
fi
grep -q 'viewModel.handleTrimMemory(level)' app/src/main/java/io/fluxreader/app/MainActivity.kt
grep -q 'ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'ComponentCallbacks2.TRIM_MEMORY_BACKGROUND' app/src/main/java/io/fluxreader/app/AppViewModel.kt
if grep -q 'TRIM_MEMORY_RUNNING_' app/src/main/java/io/fluxreader/app/AppViewModel.kt; then
  echo "❌ alpha.39: deprecated running-memory trim levels must not be used"
  exit 54
fi
if grep -q 'preparingKey' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt; then
  echo "❌ alpha.39: dead pagination preparation state returned"
  exit 55
fi
echo "✅ alpha.39 UI dedup / fullscreen consolidation / memory-pressure guards OK"

# alpha.40 UI action audit / explicit multi-select / stale pagination commit guards
LIBRARY_UI="app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt"
SETTINGS_PRIMITIVES="app/src/main/java/io/fluxreader/app/ui/SettingsPrimitives.kt"
READER_UI="app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt"
PREPARED_PAGINATION="app/src/main/java/io/fluxreader/app/ui/PreparedReaderPagination.kt"
grep -q 'var selectionMode by rememberSaveable' "$LIBRARY_UI"
grep -q 'contentDescription = "Действия с выделенными книгами"' "$LIBRARY_UI"
grep -q 'onLongClick = onStartSelection' "$LIBRARY_UI"
if grep -q 'onLongClick = onToggleSelection' "$LIBRARY_UI"; then
  echo "❌ alpha.40: long press снова конфликтует с контекстными действиями книги"
  exit 56
fi
if grep -q 'private fun BookGridCard' "$LIBRARY_UI" && grep -A10 -F 'private fun BookGridCard(' "$LIBRARY_UI" | grep -q 'onEdit:'; then
  echo "❌ alpha.40: dead card onEdit parameter returned"
  exit 57
fi
[[ -f "$SETTINGS_PRIMITIVES" ]]
grep -q 'internal fun SettingsToggleRow' "$SETTINGS_PRIMITIVES"
grep -q 'internal fun SettingsValueSlider' "$SETTINGS_PRIMITIVES"
if grep -Rqs -E 'private fun (SettingsSwitch|SpeedSettingSwitch|FloatSetting|VisualSlider)' app/src/main/java/io/fluxreader/app/ui; then
  echo "❌ alpha.40: duplicate settings primitives returned"
  exit 58
fi
grep -q 'rememberUpdatedState(layoutKey)' "$READER_UI"
grep -q 'ReaderPaginationCommitPolicy.shouldCommit' "$READER_UI"
grep -q 'internal object ReaderPaginationCommitPolicy' "$PREPARED_PAGINATION"
grep -q 'class ReaderPaginationCommitPolicyTest' app/src/test/java/io/fluxreader/app/ui/ReaderPaginationCommitPolicyTest.kt
echo "✅ alpha.40 explicit selection / long-press actions / shared settings / stale-layout guards OK"

# alpha.41 library action consolidation / centralized back destination guards
BACK_POLICY="app/src/main/java/io/fluxreader/app/AppBackDestinationPolicy.kt"
grep -q 'internal object AppBackDestinationPolicy' "$BACK_POLICY"
grep -q 'AppBackDestinationPolicy.destination' app/src/main/java/io/fluxreader/app/AppViewModel.kt
grep -q 'class AppBackDestinationPolicyTest' app/src/test/java/io/fluxreader/app/AppBackDestinationPolicyTest.kt
if grep -q 'viewModel.speedPause(); onBack()' app/src/main/java/io/fluxreader/app/ui/SpeedReaderScreen.kt; then
  echo "❌ alpha.41: Speed Reader back still pauses twice"
  exit 59
fi
grep -q 'BookActionsDialog' "$LIBRARY_UI"
# alpha.48 intentionally moved the primary Speed action back onto cards; long press owns file actions.
if grep -q 'if (supporting.isNotBlank()) {[[:space:]]*if (supporting.isNotBlank())' "$LIBRARY_UI"; then
  echo "❌ alpha.41: duplicate supporting-text guard returned"
  exit 63
fi
grep -q 'destructive = true' "$LIBRARY_UI"
grep -q 'LibrarySelectionActionsMenu(' "$LIBRARY_UI"
grep -q 'zip -qry "$ZIP" "FluxReader-$VERSION-clean"' scripts/package-release.command
echo "✅ alpha.41 unified library actions / back destination / duplicate cleanup / rooted-package guards OK"

# alpha.42 modal hierarchy / explicit close / destructive-confirm consistency
MODAL_PRIMITIVES="app/src/main/java/io/fluxreader/app/ui/ModalPrimitives.kt"
BOOK_SEARCH_UI="app/src/main/java/io/fluxreader/app/ui/BookSearchSheet.kt"
SPEED_UI="app/src/main/java/io/fluxreader/app/ui/SpeedReaderScreen.kt"
[[ -f "$MODAL_PRIMITIVES" ]]
grep -q 'internal fun ModalSheetHeader' "$MODAL_PRIMITIVES"
grep -q 'contentDescription = "Закрыть"' "$MODAL_PRIMITIVES"
grep -q 'internal fun DestructiveConfirmDialog' "$MODAL_PRIMITIVES"
grep -q 'containerColor = MaterialTheme.colorScheme.error' "$MODAL_PRIMITIVES"
grep -q 'ModalSheetHeader(' "$BOOK_SEARCH_UI"
grep -q 'properties = ModalBottomSheetProperties(shouldDismissOnBackPress = panel == ReaderSettingsPanel.MAIN)' "$READER_UI"
grep -q 'BackHandler(enabled = panel == ReaderSettingsPanel.ADVANCED)' "$READER_UI"
grep -q 'panel = ReaderSettingsPanel.MAIN' "$READER_UI"
[[ $(grep -c 'properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false)' "$SPEED_UI") -ge 4 ]] || {
  echo "❌ alpha.42: nested Speed Reader sheets must own Back instead of dismissing"
  exit 65
}
grep -q 'BackHandler(enabled = settingsPanel != SpeedSettingsPanel.NONE)' "$SPEED_UI"
grep -q 'readerSpeedReaderLibrarySequenceIsStable' app/src/test/java/io/fluxreader/app/AppBackDestinationPolicyTest.kt
grep -q 'SpeedSettingsPanel.VOICE,' "$SPEED_UI" && grep -q 'SpeedSettingsPanel.ADVANCED -> SpeedSettingsPanel.HUB' "$SPEED_UI"
if grep -q 'private fun SpeedSheetHeader' "$SPEED_UI"; then
  echo "❌ alpha.42: duplicate SpeedSheetHeader returned"
  exit 66
fi
[[ $(grep -c 'DestructiveConfirmDialog(' "$LIBRARY_UI") -eq 4 ]] || {
  echo "❌ alpha.42: deletion confirmations drifted apart"
  exit 67
}
echo "✅ alpha.42 modal Back/Close hierarchy / shared destructive confirmation guards OK"

# alpha.43 library form / IME / dialog action semantics guards
FORM_POLICY="app/src/main/java/io/fluxreader/app/ui/LibraryFormPolicy.kt"
grep -q 'internal object LibraryFormPolicy' "$FORM_POLICY"
grep -q 'class LibraryFormPolicyTest' app/src/test/java/io/fluxreader/app/ui/LibraryFormPolicyTest.kt
grep -q 'TextRange(0, initialName.length)' "$LIBRARY_UI"
grep -q 'imeAction = ImeAction.Done' "$LIBRARY_UI"
grep -q 'KeyboardActions(onDone = { submitForm() })' "$LIBRARY_UI"
grep -q 'LibraryFormPolicy.canSaveBookMetadata(title)' "$LIBRARY_UI"
grep -q 'keyboardActions = nextAction' "$LIBRARY_UI"
grep -q 'Text("Закрыть")' "$LIBRARY_UI"
grep -q 'keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)' "$LIBRARY_UI"
grep -q 'keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)' "$BOOK_SEARCH_UI"
grep -q 'focusRequester.requestFocus()' "$BOOK_SEARCH_UI"
grep -q 'keyboardController?.hide()' "$BOOK_SEARCH_UI"
grep -q 'keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)' app/src/main/java/io/fluxreader/app/ui/ReaderPreferences.kt
if grep -A45 -F 'private fun FolderDestinationDialog(' "$LIBRARY_UI" | grep -q 'Text("Отмена")'; then
  echo "❌ alpha.43: destination picker must Close, not Cancel"
  exit 68
fi
if grep -A18 -F 'if (importChooserVisible)' "$LIBRARY_UI" | grep -q 'Text("Отмена")'; then
  echo "❌ alpha.43: action-only import dialog must Close, not Cancel"
  exit 69
fi
echo "✅ alpha.43 form validation / IME / Save-Cancel-Close semantics guards OK"

# alpha.43.1 Kotlin build-fix guards
APP_VM="app/src/main/java/io/fluxreader/app/AppViewModel.kt"
if grep -q 'delay(' "$APP_VM" && ! grep -q '^import kotlinx.coroutines.delay$' "$APP_VM"; then
  echo "❌ alpha.43.1: AppViewModel uses delay() without importing kotlinx.coroutines.delay"
  exit 70
fi
ACTION_SIG="$(grep -A7 -F 'private fun LibraryActionButton(' "$LIBRARY_UI")"
DESTRUCTIVE_LINE="$(printf '%s\n' "$ACTION_SIG" | grep -n 'destructive: Boolean = false' | head -1 | cut -d: -f1)"
ONCLICK_LINE="$(printf '%s\n' "$ACTION_SIG" | grep -n 'onClick: () -> Unit' | head -1 | cut -d: -f1)"
if [[ -z "$DESTRUCTIVE_LINE" || -z "$ONCLICK_LINE" || "$ONCLICK_LINE" -le "$DESTRUCTIVE_LINE" ]]; then
  echo "❌ alpha.43.1: LibraryActionButton must keep onClick after destructive so trailing lambdas bind to the callback"
  exit 71
fi
echo "✅ alpha.43.1 Kotlin delay import / trailing-lambda action guards OK"

# alpha.43.2 Kotlin test deprecation guards
CACHE_TEST="app/src/test/java/io/fluxreader/app/reader/ParsedBookDiskCacheTest.kt"
if grep -R -n -E '\bcreateTempDir[[:space:]]*\(' app/src/test --include='*.kt' >/dev/null 2>&1; then
  echo "❌ alpha.43.2: deprecated Kotlin createTempDir() remains in unit tests; warnings-as-errors will fail compilation"
  exit 72
fi
grep -q '^import kotlin.io.path.createTempDirectory$' "$CACHE_TEST" || {
  echo "❌ alpha.43.2: ParsedBookDiskCacheTest must use kotlin.io.path.createTempDirectory"
  exit 73
}
grep -q 'createTempDirectory("flux-parsed-cache-").toFile()' "$CACHE_TEST" || {
  echo "❌ alpha.43.2: parsed-cache tests are not using the secure temp-directory API"
  exit 74
}
echo "✅ alpha.43.2 unit-test temp-directory deprecation guards OK"

# alpha.44 Speed Reader control contrast guards (fullscreen lane superseded by alpha.45)
grep -q 'border = BorderStroke(1.dp, textColor.copy(alpha = 0.17f))' "$SPEED_UI"
grep -q 'modifier = Modifier.size(56.dp)' "$SPEED_UI"
grep -q 'border = BorderStroke(1.dp, textColor.copy(alpha = 0.14f))' "$SPEED_UI"
echo "✅ alpha.44 Speed Reader control contrast guards OK"

# alpha.45 unified immersive-mode + ZIP archive import guards
READER_UI="app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt"
ARCHIVE_POLICY="app/src/main/java/io/fluxreader/app/data/ArchiveImportPolicy.kt"
LIBRARY_REPO="app/src/main/java/io/fluxreader/app/data/LibraryRepository.kt"
if grep -q 'var isFullscreen by rememberSaveable(state.bookId)' "$READER_UI"; then
  echo "❌ alpha.45: ordinary Reader must not keep a second explicit fullscreen state"
  exit 75
fi
grep -q 'FullscreenSystemBarsEffect(!chromeVisible)' "$READER_UI" || {
  echo "❌ alpha.45: ordinary Reader immersive mode must be the chrome-hidden state"
  exit 76
}
if grep -q 'ReaderFullscreenControls' "$READER_UI" || grep -q 'text = { Text("Во весь экран") }' "$READER_UI"; then
  echo "❌ alpha.45: duplicate Reader fullscreen controls/menu entry returned"
  exit 77
fi
grep -q 'contentDescription = "Скорочтение"' "$READER_UI" || {
  echo "❌ alpha.45+: normal Reader must expose the dedicated speed-reading action"
  exit 78
}
if grep -q 'fullscreenControlsVisible' "$SPEED_UI" || grep -q 'onExitFullscreen' "$SPEED_UI"; then
  echo "❌ alpha.45: Speed Reader must have one fullscreen state without nested chrome mode"
  exit 79
fi
grep -q 'immersiveMode = true' "$SPEED_UI"
grep -q 'onToggleFullscreen = { isFullscreen = !isFullscreen }' "$SPEED_UI"
grep -q 'object ArchiveImportPolicy' "$ARCHIVE_POLICY"
grep -q 'MAX_ARCHIVE_BYTES = 1L' "$ARCHIVE_POLICY"
grep -q 'MAX_BOOK_BYTES = 512L' "$ARCHIVE_POLICY"
grep -q 'segments.any { it == ".." }' "$ARCHIVE_POLICY"
grep -q 'ZipFile(tempArchive)' "$LIBRARY_REPO"
grep -q 'suspend fun importSource(' "$LIBRARY_REPO"
grep -q 'importArchiveInternal(uri, name, onProgress)' "$LIBRARY_REPO"
grep -q '"Архив ZIP"' "$LIBRARY_UI" || {
  echo "❌ alpha.45: library import chooser does not expose ZIP archive import"
  exit 80
}
echo "✅ alpha.45 unified immersive mode / archive import guards OK"

# alpha.46 fullscreen anchor / non-blocking reflow / contextual word action guards
if grep -q 'Подготавливаю страницы под экран' "$READER_UI"; then
  echo "❌ alpha.46: fullscreen reflow must not replace the current page with a blocking preparation screen"
  exit 81
fi
grep -q 'pendingViewportAnchor' "$READER_UI" || {
  echo "❌ alpha.46: viewport anchor preservation missing"
  exit 82
}
grep -q 'setChromeVisiblePreservingPosition' "$READER_UI" || {
  echo "❌ alpha.46: chrome/fullscreen transitions bypass anchor preservation"
  exit 83
}
grep -q 'usingFallbackLayout' "$READER_UI" || {
  echo "❌ alpha.46: old prepared page fallback missing during reflow"
  exit 84
}
if grep -q 'fullscreenEntrySnapPending' "$READER_UI"; then
  echo "❌ alpha.46: legacy fullscreen line-snap returned"
  exit 85
fi
if grep -q 'Скорочтение с выбранного слова' "$READER_UI"; then
  echo "❌ alpha.46: selected-word speed action must not live in the overflow menu"
  exit 86
fi
grep -q 'ReaderTextContextMenu(' "$READER_UI"
grep -q 'readerTextRangeSelection' "$READER_UI"
grep -q 'border = BorderStroke(1.dp, if (speedEnabled) MaterialTheme.colorScheme.error' "$READER_UI" || {
  echo "❌ alpha.46: compact red-outline speed button missing"
  exit 87
}
if grep -q 'Text("Скорочтение", style = MaterialTheme.typography.labelMedium' "$READER_UI"; then
  echo "❌ alpha.46: labeled speed pill returned to Reader bottom bar"
  exit 88
fi
grep -q 'onCenterTap = { setChromeVisiblePreservingPosition(!chromeVisible) }' "$READER_UI" || {
  echo "❌ alpha.82: tap-to-immersive entry action missing"
  exit 89
}
echo "✅ alpha.46 fullscreen anchor / non-blocking reflow / contextual word action guards OK"

# alpha.47 long-press text actions / stable fullscreen anchor / Speed fullscreen entry guards
grep -q 'stableViewportAnchor' "$READER_UI" || {
  echo "❌ alpha.47: stable viewport anchor missing"
  exit 90
}
grep -q 'viewportAnchorLocked' "$READER_UI" || {
  echo "❌ alpha.47: transition anchor lock missing"
  exit 90
}
grep -q 'viewportAnchor = pendingViewportAnchor' "$READER_UI" || {
  echo "❌ alpha.47: pending fullscreen anchor is not passed into pagination"
  exit 91
}
grep -q 'preparedPagination?.isComplete == true' "$READER_UI" || {
  echo "❌ alpha.47: fullscreen transition can capture an incomplete preview anchor"
  exit 92
}
grep -q 'ReaderTextActionPolicy.select' "$READER_UI" || {
  echo "❌ alpha.47: long-press text action policy missing"
  exit 93
}
grep -q 'Text("Копировать")' "$READER_UI"
grep -q 'Сохранить как цитату' "$READER_UI"
grep -q 'Добавить заметку' "$READER_UI"
grep -q 'Добавить закладку' "$READER_UI"
grep -q 'Найти в книге' "$READER_UI"
if grep -q 'onWordTap' "$READER_UI" || grep -q 'selectReaderWord' "$READER_UI"; then
  echo "❌ alpha.47: single-tap word selection returned"
  exit 94
fi
grep -q 'style = style.copy(color = readerTextColor)' "$READER_UI" || {
  echo "❌ alpha.47: BasicText explicit reader color guard missing"
  exit 95
}
grep -q 'readerTextLongPress' app/src/main/java/io/fluxreader/app/ui/ReaderGestures.kt || {
  echo "❌ alpha.47: non-consuming long-press gesture helper missing"
  exit 96
}
grep -q 'readerTextTapAction' app/src/main/java/io/fluxreader/app/ui/ReaderGestures.kt || {
  echo "❌ alpha.47: child-owned link tap helper missing"
  exit 97
}
if grep -q 'IconButton(onClick = { isFullscreen = true })' "$SPEED_UI"; then
  echo "❌ alpha.47: Speed fullscreen entry duplicated in the top app bar"
  exit 98
fi
grep -q 'onFullscreen = { isFullscreen = true }' "$SPEED_UI" || {
  echo "❌ alpha.47: Speed fullscreen entry is not attached to playback controls"
  exit 99
}
grep -q 'contentDescription = "Полноэкранное скорочтение"' "$SPEED_UI" || {
  echo "❌ alpha.47: Speed fullscreen control missing"
  exit 100
}
if rg -q '\bcreateTempDir\s*\(' app/src/test; then
  echo "❌ alpha.47: deprecated createTempDir returned to tests"
  exit 101
fi
echo "✅ alpha.47 long-press text actions / stable fullscreen anchor / Speed fullscreen entry guards OK"

# alpha.48 Speed→Reader continuity / themed library cards / compact action popup guards
grep -q 'fun toReaderAnchor(chapter: ChapterContent, chapterOffset: Int)' app/src/main/java/io/fluxreader/app/reader/ChapterSpeedOffset.kt || {
  echo "❌ alpha.48: reverse Speed→Reader anchor mapping missing"
  exit 102
}
grep -q 'restoreReaderFromSpeedPosition()' app/src/main/java/io/fluxreader/app/AppViewModel.kt || {
  echo "❌ alpha.48: Speed exit does not restore Reader position"
  exit 103
}
grep -q 'libraryRepository.updateReaderPosition(bookId, safeChapter, safeBlock, 0, progress, safeChar)' app/src/main/java/io/fluxreader/app/AppViewModel.kt || {
  echo "❌ alpha.48: Reader position is not persisted from Speed stop"
  exit 104
}
grep -q 'private fun SpeedCardButton(' "$LIBRARY_UI" || {
  echo "❌ alpha.48: direct Speed card button missing"
  exit 105
}
python3 - <<'PYALPHA48'
from pathlib import Path
text = Path('app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt').read_text()
for start_name, end_name in [
    ('private fun BookGridCard(', 'private fun CompactBookCard('),
    ('private fun CompactBookCard(', 'private fun BookListCard('),
    ('private fun BookListCard(', 'private fun SelectionBadge'),
]:
    start = text.index(start_name)
    end = text.index(end_name, start)
    chunk = text[start:end]
    if 'onSpeed:' not in chunk or 'SpeedCardButton(' not in chunk:
        raise SystemExit(f'❌ alpha.48: direct Speed action missing in {start_name}')
    if 'Icons.Default.MoreVert' in chunk:
        raise SystemExit(f'❌ alpha.48: overflow button returned in {start_name}')
book_start = text.index('private fun BookActionsDialog(')
book_end = text.index('private fun FolderActionsDialog(', book_start)
menu = text[book_start:book_end]
if 'Text(book.title' in menu or 'supporting' in menu or '"Открыть"' in menu or '"Скорочтение"' in menu:
    raise SystemExit('❌ alpha.48: book action popup is not compact')
if 'modifier = Modifier.width(268.dp)' not in menu:
    raise SystemExit('❌ alpha.48: compact popup width guard missing')
if text.count('CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy') < 4:
    raise SystemExit('❌ alpha.48: theme-tinted library cards missing')
PYALPHA48
grep -q 'mapsSpeedOffsetBackToReaderBlockAndCharacter' app/src/test/java/io/fluxreader/app/reader/ChapterSpeedOffsetTest.kt || {
  echo "❌ alpha.48: reverse offset test missing"
  exit 106
}
echo "✅ alpha.48 Speed→Reader continuity / themed cards / direct Speed buttons / compact actions guards OK"

# alpha.49 live import progress / cancellation / library continuity guards
grep -q 'data class SourceImportProgress' app/src/main/java/io/fluxreader/app/data/Models.kt || {
  echo "❌ alpha.49: import progress model missing"
  exit 107
}
grep -q 'val importState: StateFlow<ImportUiState>' app/src/main/java/io/fluxreader/app/AppViewModel.kt || {
  echo "❌ alpha.49: import state flow missing"
  exit 108
}
grep -q 'fun cancelImport()' app/src/main/java/io/fluxreader/app/AppViewModel.kt || {
  echo "❌ alpha.49: import cancellation missing"
  exit 109
}
grep -q 'ImportPhase.COPYING_ARCHIVE' app/src/main/java/io/fluxreader/app/data/LibraryRepository.kt || {
  echo "❌ alpha.49: ZIP copy progress missing"
  exit 110
}
grep -q 'ImportPhase.SCANNING_FOLDER' app/src/main/java/io/fluxreader/app/data/LibraryRepository.kt || {
  echo "❌ alpha.49: folder scan progress missing"
  exit 111
}
grep -q 'private fun ImportProgressCard(' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt || {
  echo "❌ alpha.49: library progress card missing"
  exit 112
}
grep -q 'onCancelImport = viewModel::cancelImport' app/src/main/java/io/fluxreader/app/ui/FluxReaderApp.kt || {
  echo "❌ alpha.49: cancel action not wired to UI"
  exit 113
}
python3 - <<'PYALPHA49'
from pathlib import Path
reader = Path('app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt').read_text()
if reader.count('if (chromeVisible && listState != null) {') != 1:
    raise SystemExit('❌ alpha.49: Reader bottom-bar visibility condition is duplicated')
lib = Path('app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt').read_text()
if 'ContinueCard(' not in lib or 'if (!selectionMode) SpeedCardButton(onClick = { onSpeed(book.id) })' not in lib:
    raise SystemExit('❌ alpha.49: Continue card Speed action missing')
fab_guard = 'if (!selectionMode && !importState.active)' in lib or 'settings.theme != ReaderThemeMode.WASTELAND_TERMINAL && !selectionMode && !importState.active' in lib
wasteland_add_guard = 'WastelandNavItem("Add", FluxIconRole.ADD, Icons.Default.Add, enabled = !importState.active)' in lib
if not fab_guard or not wasteland_add_guard:
    raise SystemExit('❌ alpha.49: add action remains active during import')
repo = Path('app/src/main/java/io/fluxreader/app/data/LibraryRepository.kt').read_text()
if 'onProgress: (SourceImportProgress) -> Unit = {}' not in repo:
    raise SystemExit('❌ alpha.49: repository progress callback missing')
if 'finally {' not in repo or 'tempArchive.delete()' not in repo:
    raise SystemExit('❌ alpha.49: ZIP cancellation cleanup guard missing')
PYALPHA49
echo "✅ alpha.49 live import progress / cancel / Continue Speed / Reader cleanup guards OK"


# alpha.50 predictable Speed taps / persistent Reader annotations / range selection guards
ANNOTATIONS_REPO="app/src/main/java/io/fluxreader/app/data/ReaderAnnotationsRepository.kt"
ANNOTATIONS_UI="app/src/main/java/io/fluxreader/app/ui/ReaderAnnotationsUi.kt"
SPEED_TAP_POLICY="app/src/main/java/io/fluxreader/app/ui/SpeedTapPolicy.kt"
grep -q 'object SpeedTapPolicy' "$SPEED_TAP_POLICY" || {
  echo "❌ alpha.50: Speed tap policy missing"
  exit 114
}
grep -q 'SpeedTapPolicy.singleTap' "$SPEED_UI" || {
  echo "❌ alpha.50: Speed single-tap contract not wired"
  exit 115
}
grep -q 'SpeedTapPolicy.doubleTap' "$SPEED_UI" || {
  echo "❌ alpha.50: Speed double-tap fullscreen contract not wired"
  exit 116
}
if grep -q 'immersiveMode -> onImmersiveTap()' "$SPEED_UI"; then
  echo "❌ alpha.50: fullscreen Speed single tap still exits immersive mode"
  exit 117
fi
grep -q 'fun Modifier.readerTextRangeSelection' app/src/main/java/io/fluxreader/app/ui/ReaderGestures.kt || {
  echo "❌ alpha.50: range-selection gesture helper missing"
  exit 118
}
grep -q 'fun selectRange' app/src/main/java/io/fluxreader/app/ui/ReaderTextActionPolicy.kt || {
  echo "❌ alpha.50: range-selection text policy missing"
  exit 119
}
grep -q 'class ReaderAnnotationsRepository' "$ANNOTATIONS_REPO" || {
  echo "❌ alpha.50: persistent annotations repository missing"
  exit 120
}
grep -q 'reader-annotations.json' "$ANNOTATIONS_REPO" || {
  echo "❌ alpha.50: annotations are not persisted separately"
  exit 121
}
grep -q 'enum class ReaderAnnotationKind { HIGHLIGHT, QUOTE, NOTE, BOOKMARK }' "$ANNOTATIONS_REPO" || {
  echo "❌ alpha.50: annotation kinds incomplete"
  exit 122
}
grep -q 'ReaderAnnotationsSheet(' "$READER_UI" || {
  echo "❌ alpha.50: annotations screen is not reachable from Reader"
  exit 123
}
grep -q 'onTextRangeSelection = ::updateReaderTextSelection' "$READER_UI" || {
  echo "❌ alpha.50: range selection is not wired into Reader"
  exit 124
}
grep -q 'annotations.forEach' "$READER_UI" || {
  echo "❌ alpha.50: persistent annotations are not rendered in Reader text"
  exit 125
}
grep -q 'class SpeedTapPolicyTest' app/src/test/java/io/fluxreader/app/ui/SpeedTapPolicyTest.kt || {
  echo "❌ alpha.50: Speed tap unit test missing"
  exit 126
}
grep -q 'rangeSelectionExpandsToWholeWords' app/src/test/java/io/fluxreader/app/ui/ReaderTextActionPolicyTest.kt || {
  echo "❌ alpha.50: range-selection unit test missing"
  exit 127
}
echo "✅ alpha.50 Speed tap contract / persistent annotations / range selection guards OK"


# alpha.50.1 Compose callback hotfix guard
python3 - <<'PYALPHA501'
from pathlib import Path
reader = Path('app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt').read_text()
try:
    callback_region = reader.split('pendingNoteSelection?.let { selection ->', 1)[1].split('editingAnnotation?.let { item ->', 1)[0]
except IndexError:
    raise SystemExit('❌ alpha.50.1: note callback structure changed unexpectedly')
if 'MaterialTheme.' in callback_region:
    raise SystemExit('❌ alpha.50.1: composable MaterialTheme access leaked into ReaderNoteDialog callback')
if 'colorArgb = noteAnnotationColorArgb' not in callback_region and 'colorArgb = readerHighlightColors.first().toArgb()' not in callback_region:
    raise SystemExit('❌ alpha.50.1: note callback must use a color precomputed in composable scope')
PYALPHA501
echo "✅ alpha.50.1 Compose note-callback hotfix guard OK"


# alpha.51 cross-paragraph selection / handles / annotation filters and recolor guards
grep -q 'fun selectDocumentRange' app/src/main/java/io/fluxreader/app/ui/ReaderTextActionPolicy.kt || {
  echo "❌ alpha.51: cross-paragraph selection policy missing"
  exit 128
}
grep -q 'fun addRange(' app/src/main/java/io/fluxreader/app/data/ReaderAnnotationsRepository.kt || {
  echo "❌ alpha.51: multi-block annotation persistence missing"
  exit 129
}
grep -q 'fun updateColor(' app/src/main/java/io/fluxreader/app/data/ReaderAnnotationsRepository.kt || {
  echo "❌ alpha.51: saved highlight recolor missing"
  exit 130
}
grep -q 'private fun ReaderSelectionHandle(' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt || {
  echo "❌ alpha.51: selection handles missing"
  exit 131
}
grep -q 'ReaderAnnotationFilter.entries' app/src/main/java/io/fluxreader/app/ui/ReaderAnnotationsUi.kt || {
  echo "❌ alpha.51: annotation filters missing"
  exit 132
}
grep -q 'onChangeColor = { item, color -> viewModel.updateReaderAnnotationColor' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt || {
  echo "❌ alpha.51: highlight recolor is not wired"
  exit 133
}
grep -q 'documentRangeSpansParagraphsInBothDirections' app/src/test/java/io/fluxreader/app/ui/ReaderTextActionPolicyTest.kt || {
  echo "❌ alpha.51: cross-paragraph range unit test missing"
  exit 134
}
python3 - <<'PYALPHA51'
from pathlib import Path
reader = Path('app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt').read_text()
repo = Path('app/src/main/java/io/fluxreader/app/data/ReaderAnnotationsRepository.kt').read_text()
ui = Path('app/src/main/java/io/fluxreader/app/ui/ReaderAnnotationsUi.kt').read_text()
if 'index in it.startBlockIndex..it.endBlockIndex' not in reader:
    raise SystemExit('❌ alpha.51: multi-block annotations are not rendered across paragraphs')
if 'startBlockIndex = selection.startBlockIndex' not in reader or 'endBlockIndex = selection.endBlockIndex' not in reader:
    raise SystemExit('❌ alpha.51: selection range is not persisted with both endpoints')
if 'pendingViewportAnchor = ReaderPageLocation(' not in reader:
    raise SystemExit('❌ alpha.51: annotation jump does not seed an exact viewport anchor')
if 'FilterChip(' not in ui or 'highlightColors.forEach' not in ui:
    raise SystemExit('❌ alpha.51: annotation sheet filters/recolor UI incomplete')
if '.put("endBlockIndex", item.endBlockIndex)' not in repo:
    raise SystemExit('❌ alpha.51: persisted annotation end block missing')
PYALPHA51
echo "✅ alpha.51 cross-paragraph selection / handles / filters / recolor guards OK"

# alpha.52 edge auto-scroll / cross-page selection continuation / saved-annotation popup guards
EDGE_POLICY="app/src/main/java/io/fluxreader/app/ui/ReaderSelectionEdgePolicy.kt"
ANNOTATION_HIT_POLICY="app/src/main/java/io/fluxreader/app/ui/ReaderAnnotationHitPolicy.kt"
[[ -f "$EDGE_POLICY" ]] || { echo "❌ alpha.52: ReaderSelectionEdgePolicy missing"; exit 135; }
[[ -f "$ANNOTATION_HIT_POLICY" ]] || { echo "❌ alpha.52: ReaderAnnotationHitPolicy missing"; exit 136; }
grep -q 'currentList.scrollBy(direction \* stepPx)' "$READER_SCREEN" || {
  echo "❌ alpha.52: scroll-mode selection edge auto-scroll missing"
  exit 137
}
grep -q 'ReaderSelectionEdgePolicy.adjacentPageIndex' "$READER_SCREEN" || {
  echo "❌ alpha.52: screen-paging selection continuation missing"
  exit 138
}
grep -q 'private fun ReaderAnnotationContextMenu(' "$READER_SCREEN" || {
  echo "❌ alpha.52: saved annotation popup missing"
  exit 139
}
grep -q 'ReaderAnnotationHitPolicy.contains' "$READER_SCREEN" || {
  echo "❌ alpha.52: saved annotations are not hit-tested from Reader text"
  exit 140
}
grep -q 'pendingDeleteAnnotation' "$READER_SCREEN" || {
  echo "❌ alpha.52: saved annotation delete confirmation missing"
  exit 141
}
grep -q 'class ReaderSelectionEdgePolicyTest' app/src/test/java/io/fluxreader/app/ui/ReaderSelectionEdgePolicyTest.kt || {
  echo "❌ alpha.52: edge policy test missing"
  exit 142
}
grep -q 'class ReaderAnnotationHitPolicyTest' app/src/test/java/io/fluxreader/app/ui/ReaderAnnotationHitPolicyTest.kt || {
  echo "❌ alpha.52: annotation hit policy test missing"
  exit 143
}
python3 - <<'PYALPHA52'
from pathlib import Path
reader = Path('app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt').read_text()
if reader.count('onSelectionEdgeDrag = ::handleSelectionEdgeDrag') != 1:
    raise SystemExit('❌ alpha.52: edge selection controller must be wired once to the shared Reader viewport')
if 'block.links.isNotEmpty() || annotations.isNotEmpty()' not in reader:
    raise SystemExit('❌ alpha.52: saved annotation taps are not child-owned')
if 'val target = if (direction < 0) slices.lastOrNull() else slices.firstOrNull()' not in reader:
    raise SystemExit('❌ alpha.52: cross-page selection should extend only to nearest adjacent-page text')
if 'DestructiveConfirmDialog(' not in reader.split('pendingDeleteAnnotation?.let { item ->', 1)[1]:
    raise SystemExit('❌ alpha.52: annotation deletion must remain confirmed')
PYALPHA52
echo "✅ alpha.52 edge auto-scroll / cross-page continuation / saved annotation popup guards OK"

# alpha.52.1 Android unit-test classpath / mirrored Notes guards
for test_file in \
  app/src/test/java/io/fluxreader/app/ui/ReaderAnnotationHitPolicyTest.kt \
  app/src/test/java/io/fluxreader/app/ui/ReaderSelectionEdgePolicyTest.kt; do
  if grep -q '^import kotlin\.test' "$test_file"; then
    echo "❌ alpha.52.1: Android unit tests must use the project JUnit 4 API, not kotlin.test"
    exit 144
  fi
  grep -q '^import org\.junit\.Test' "$test_file" || {
    echo "❌ alpha.52.1: JUnit Test import missing in $test_file"
    exit 145
  }
done
if grep -R -q 'import androidx.compose.material.icons.filled.Notes' \
  app/src/main/java/io/fluxreader/app/ui/ReaderAnnotationsUi.kt \
  app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt; then
  echo "❌ alpha.52.1: Notes icon must use AutoMirrored"
  exit 146
fi
grep -q 'Icons.AutoMirrored.Filled.Notes' app/src/main/java/io/fluxreader/app/ui/ReaderAnnotationsUi.kt
grep -q 'Icons.AutoMirrored.Filled.Notes' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt
echo "✅ alpha.52.1 JUnit classpath / AutoMirrored Notes guards OK"


# alpha.53 annotation workspace guards
ANNOTATIONS_UI="app/src/main/java/io/fluxreader/app/ui/ReaderAnnotationsUi.kt"
ANNOTATION_TOOLS="app/src/main/java/io/fluxreader/app/ui/ReaderAnnotationTools.kt"
ANNOTATION_REPO="app/src/main/java/io/fluxreader/app/data/ReaderAnnotationsRepository.kt"
READER_SCREEN="app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt"
APP_VM="app/src/main/java/io/fluxreader/app/AppViewModel.kt"
for file in "$ANNOTATIONS_UI" "$ANNOTATION_TOOLS" "$ANNOTATION_REPO" "$READER_SCREEN" "$APP_VM"; do
  test -f "$file" || { echo "❌ alpha.53: missing $file"; exit 1; }
done
grep -q 'ReaderAnnotationScope.ALL_BOOKS' "$ANNOTATIONS_UI" || { echo "❌ alpha.53: all-books annotation scope missing"; exit 1; }
grep -q 'По книге' "$ANNOTATIONS_UI" && grep -q 'По главе' "$ANNOTATIONS_UI" && grep -q 'По дате' "$ANNOTATIONS_UI" || { echo "❌ alpha.53: annotation sort modes missing"; exit 1; }
grep -q 'CreateDocument("text/markdown")' "$ANNOTATIONS_UI" && grep -q 'CreateDocument("text/plain")' "$ANNOTATIONS_UI" || { echo "❌ alpha.53: annotation export pickers missing"; exit 1; }
grep -q 'fun updateRange(' "$ANNOTATION_REPO" || { echo "❌ alpha.53: persisted annotation range update missing"; exit 1; }
grep -q 'fun openReaderAt(' "$APP_VM" || { echo "❌ alpha.53: global annotation precise-open missing"; exit 1; }
grep -q 'Сохранить новые границы' "$READER_SCREEN" || { echo "❌ alpha.53: existing annotation boundary editor missing"; exit 1; }
grep -q 'kind = ReaderAnnotationKind.HIGHLIGHT' "$READER_SCREEN" || { echo "❌ alpha.53: notes must attach to a highlighted range"; exit 1; }
grep -q 'ReaderAnnotationToolsTest' < <(find app/src/test -type f -maxdepth 8 -print) || { echo "❌ alpha.53: annotation tools tests missing"; exit 1; }
echo "✅ alpha.53 annotation workspace / export / range-edit guards OK"


# alpha.58 deep experimental-theme guards
THEME_CHROME="app/src/main/java/io/fluxreader/app/ui/ThemeChrome.kt"
OVERLAY="app/src/main/java/io/fluxreader/app/ui/ExperimentalUiOverlay.kt"
test -f "$THEME_CHROME" || { echo "❌ alpha.58: ThemeChrome.kt missing"; exit 158; }
test -f app/src/main/res/drawable-nodpi/wasteland_surface_texture.png || { echo "❌ alpha.58: wasteland texture missing"; exit 159; }
test -f app/src/main/res/drawable-nodpi/cyberpunk_surface_texture.png || { echo "❌ alpha.58: cyberpunk texture missing"; exit 160; }
[[ $(find app/src/main/res/drawable -name 'ic_wasteland_*.xml' | wc -l | tr -d ' ') -ge 15 ]] || { echo "❌ alpha.58: incomplete Wasteland icon family"; exit 161; }
[[ $(find app/src/main/res/drawable -name 'ic_cyber_*.xml' | wc -l | tr -d ' ') -ge 15 ]] || { echo "❌ alpha.58: incomplete Cyberpunk icon family"; exit 162; }
grep -q 'ExperimentalThemePreview(settings.experimentalFrameTheme)' app/src/main/java/io/fluxreader/app/ui/ReaderPreferences.kt || { echo "❌ alpha.58: experimental preview missing"; exit 163; }
grep -q 'wasteland_surface_texture' "$OVERLAY" && grep -q 'cyberpunk_surface_texture' "$OVERLAY" || { echo "❌ alpha.58: texture overlay routing missing"; exit 164; }
if grep -q 'pointerInput\|infiniteTransition\|rememberInfiniteTransition' "$OVERLAY"; then
  echo "❌ alpha.58: experimental overlay must stay static/non-interactive"
  exit 165
fi
echo "✅ alpha.58 textures / themed icon families / static overlay guards OK"

# alpha.59 full Wasteland interface overhaul guards
WASTELAND_UI="app/src/main/java/io/fluxreader/app/ui/WastelandUi.kt"
test -f "$WASTELAND_UI" || { echo "❌ alpha.59: WastelandUi.kt missing"; exit 166; }
grep -q 'fun WastelandDeviceFrame' "$WASTELAND_UI" || { echo "❌ alpha.59: physical device frame missing"; exit 167; }
grep -q 'fun WastelandBottomRail' "$WASTELAND_UI" || { echo "❌ alpha.59: hardware bottom rail missing"; exit 168; }
grep -q 'fun WastelandCrtGlass' "$WASTELAND_UI" || { echo "❌ alpha.59: CRT glass layer missing"; exit 169; }
grep -q 'WastelandDeviceFrame' app/src/main/java/io/fluxreader/app/ui/FluxReaderApp.kt || { echo "❌ alpha.59: app shell does not route through Wasteland device frame"; exit 170; }
grep -q 'ExperimentalHardwareRail' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt || { echo "❌ alpha.59: Library hardware rail missing"; exit 171; }
grep -q 'WastelandLibraryFilter' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt || { echo "❌ alpha.59: Library Wasteland filters missing"; exit 172; }
grep -q 'contentDescription = "Меню чтения"' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt || { echo "❌ alpha.82: compact Reader overflow missing"; exit 173; }
grep -q 'ExperimentalPanel' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt || { echo "❌ alpha.59: Reader Wasteland controls missing"; exit 174; }
grep -q 'ExperimentalTopBar' app/src/main/java/io/fluxreader/app/ui/PdfReaderScreen.kt || { echo "❌ alpha.59: PDF Wasteland header missing"; exit 175; }
grep -q 'Modifier.fillMaxWidth().height(56.dp)' app/src/main/java/io/fluxreader/app/ui/SpeedReaderScreen.kt || { echo "❌ alpha.77: compact Speed Reader header missing"; exit 176; }
grep -q 'ExperimentalPanel' app/src/main/java/io/fluxreader/app/ui/SpeedReaderScreen.kt || { echo "❌ alpha.59: Speed Reader Wasteland hardware controls missing"; exit 177; }
grep -q 'ExperimentalTopBar' app/src/main/java/io/fluxreader/app/ui/SettingsScreen.kt || { echo "❌ alpha.59: Settings Wasteland header missing"; exit 178; }
grep -q 'ExperimentalPanel' app/src/main/java/io/fluxreader/app/ui/SettingsPrimitives.kt || { echo "❌ alpha.59: Settings Wasteland panels missing"; exit 179; }
grep -q 'FontFamily.Monospace' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt || { echo "❌ alpha.59: Wasteland Reader terminal typography missing"; exit 180; }
if grep -q 'pointerInput\|rememberInfiniteTransition\|infiniteTransition' "$WASTELAND_UI"; then
  echo "❌ alpha.59: Wasteland physical shell must remain static and touch-transparent"
  exit 181
fi
echo "✅ alpha.59 full Wasteland shell / Library / Reader / PDF / Speed / Settings guards OK"

# alpha.61 Wasteland startup geometry + FolderLibrary compile guards
WASTELAND_UI="app/src/main/java/io/fluxreader/app/ui/WastelandUi.kt"
grep -q 'propagateMinConstraints = true' "$WASTELAND_UI" || { echo "❌ alpha.61: Wasteland route viewport can collapse again"; exit 182; }
grep -q 'windowInsetsPadding(WindowInsets.safeDrawing)' "$WASTELAND_UI" || { echo "❌ alpha.61: Wasteland controls do not respect system safe area"; exit 183; }
for SCREEN in LibraryScreen.kt SettingsScreen.kt PdfReaderScreen.kt ReaderScreen.kt SpeedReaderScreen.kt; do
  grep -A2 'Scaffold(' "app/src/main/java/io/fluxreader/app/ui/$SCREEN" | grep -q 'modifier = Modifier.fillMaxSize()' || { echo "❌ alpha.61: $SCREEN Scaffold is not full-screen"; exit 184; }
done
LIBRARY="app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt"
grep -q 'val wasteland = experimentalMode.isExperimentalChrome' "$LIBRARY" || { echo "❌ alpha.61: FolderLibrary does not resolve theme from CompositionLocal"; exit 185; }
grep -q 'val bottomContentPadding = if (wasteland) 18.dp else 104.dp' "$LIBRARY" || { echo "❌ alpha.61: FolderLibrary bottom padding policy missing"; exit 186; }
if sed -n '/private fun FolderLibrary(/,/^}/p' "$LIBRARY" | grep -q 'settings\.theme'; then
  echo "❌ alpha.61: FolderLibrary contains out-of-scope settings.theme reference"
  exit 187
fi
grep -q 'bottom = bottomContentPadding' "$LIBRARY" || { echo "❌ alpha.61: FolderLibrary grid/list does not use resolved bottom padding"; exit 188; }
echo "✅ alpha.61 Wasteland viewport / safe-insets / FolderLibrary compile guards OK"

echo

echo

echo "▶ Gradle Wrapper package integrity"
WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"
WRAPPER_PROPS="gradle/wrapper/gradle-wrapper.properties"
if [[ ! -s "$WRAPPER_JAR" ]]; then
  echo "❌ Отсутствует $WRAPPER_JAR — clean ZIP неполный и ./gradlew не запустится."
  exit 35
fi
if [[ ! -f "$WRAPPER_PROPS" ]]; then
  echo "❌ Отсутствует $WRAPPER_PROPS"
  exit 36
fi
if ! jar tf "$WRAPPER_JAR" 2>/dev/null | grep -q 'org/gradle/wrapper/GradleWrapperMain.class'; then
  echo "❌ $WRAPPER_JAR повреждён или не содержит GradleWrapperMain.class"
  exit 37
fi
echo "✅ Gradle Wrapper JAR + properties присутствуют и валидны"

echo
echo "▶ Java"
if ! command -v java >/dev/null 2>&1; then
  echo "❌ Java не найдена. Нужен JDK 17+."
  exit 20
fi
java -version 2>&1 | head -n 1

# alpha.35.1 Compose directional icon deprecation guard
if grep -q 'import androidx.compose.material.icons.filled.Undo' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt; then
  echo "❌ alpha.35.1: Undo must use AutoMirrored icon"
  exit 1
fi
grep -q 'Icons.AutoMirrored.Filled.Undo' app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt
echo "✅ alpha.35.1 AutoMirrored Undo guard OK"

# alpha.62 root LibraryScreen padding scope compile guard
ROOT_LIBRARY_HEAD="$(sed -n '148,1258p' "$LIBRARY")"
grep -q 'val libraryBottomContentPadding = if ((settings.useWastelandDesign || settings.useCyberpunkDesign)) 18.dp else 104.dp' <<< "$ROOT_LIBRARY_HEAD" || { echo "❌ alpha.62: root library bottom padding declaration missing"; exit 189; }
grep -q 'val libraryCompactBottomContentPadding = if ((settings.useWastelandDesign || settings.useCyberpunkDesign)) 18.dp else 96.dp' <<< "$ROOT_LIBRARY_HEAD" || { echo "❌ alpha.62: root compact padding declaration missing"; exit 190; }
grep -q 'bottom = libraryBottomContentPadding' <<< "$ROOT_LIBRARY_HEAD" || { echo "❌ alpha.62: root grid does not use root-scoped padding"; exit 191; }
grep -q 'bottom = libraryCompactBottomContentPadding' <<< "$ROOT_LIBRARY_HEAD" || { echo "❌ alpha.62: compact grid does not use root-scoped padding"; exit 192; }
if grep -Eq 'bottom[[:space:]]*=[[:space:]]*bottomContentPadding|PaddingValues\([^\n]*bottomContentPadding' <<< "$ROOT_LIBRARY_HEAD"; then
  echo "❌ alpha.62: bare FolderLibrary bottomContentPadding leaked into root LibraryScreen scope"
  exit 193
fi
echo "✅ alpha.62 LibraryScreen padding scope compile guards OK"

# alpha.63 Wasteland Library measured-layout + direct-action guards
LIBRARY_SCREEN="app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt"
grep -q 'val libraryContent: @Composable (PaddingValues) -> Unit' "$LIBRARY_SCREEN" || { echo "❌ alpha.63: reusable measured library content missing"; exit 194; }
grep -q 'Box(Modifier.weight(1f).fillMaxWidth())' "$LIBRARY_SCREEN" || { echo "❌ alpha.63: Wasteland book viewport is not the weighted region"; exit 195; }
grep -q 'WastelandNavItem("Search", FluxIconRole.SEARCH, Icons.Default.Search) { wastelandSearchRequested = true }' "$LIBRARY_SCREEN" || { echo "❌ alpha.63: visible SEARCH hardware button is not wired directly"; exit 196; }
grep -q 'WastelandNavItem("Add", FluxIconRole.ADD, Icons.Default.Add, enabled = !importState.active) { importChooserVisible = true }' "$LIBRARY_SCREEN" || { echo "❌ alpha.63: visible ADD hardware button is not wired directly"; exit 197; }
grep -q 'WastelandNavItem("Settings", FluxIconRole.SETTINGS, Icons.Default.Settings) { onSettings() }' "$LIBRARY_SCREEN" || { echo "❌ alpha.63: visible SETTINGS hardware button is not wired directly"; exit 198; }
grep -q 'libraryKeyboardController?.show()' "$LIBRARY_SCREEN" || { echo "❌ alpha.63: SEARCH does not request keyboard visibility"; exit 199; }
if sed -n '/if ((settings.useWastelandDesign || settings.useCyberpunkDesign)) {/,/} else {/p' "$LIBRARY_SCREEN" | grep -q 'Scaffold('; then
  echo "❌ alpha.63: Wasteland Library regressed to independently measured Scaffold bars"
  exit 200
fi
echo "✅ alpha.63 Wasteland Library fixed regions / direct hit targets guards OK"

# alpha.64 Wasteland wrap-content panel measurement guard
WASTELAND_PANEL_BLOCK="$(sed -n '/internal fun WastelandPanel(/,/^@Composable/p' "$WASTELAND_UI")"
grep -q 'modifier = Modifier.matchParentSize()' <<< "$WASTELAND_PANEL_BLOCK" || { echo "❌ alpha.64: Wasteland panel texture is not measurement-neutral"; exit 201; }
if grep -q 'modifier = Modifier.fillMaxSize()' <<< "$WASTELAND_PANEL_BLOCK"; then
  echo "❌ alpha.64: WastelandPanel decorative texture can stretch wrap-content panels again"
  exit 202
fi
if grep -Eq '^import[[:space:]]+androidx\.compose\.foundation\.layout\.matchParentSize([[:space:]]|$)' "$WASTELAND_UI"; then
  echo "❌ alpha.66: matchParentSize is a BoxScope member; the import breaks Kotlin compilation"
  exit 203
fi
echo "✅ alpha.66 Wasteland panel texture and BoxScope import guards OK"

# alpha.65: the experimental layout MUST be a persisted opt-in, not a palette side effect.
SETTINGS_MODEL="app/src/main/java/io/fluxreader/app/data/Models.kt"
SETTINGS_STORE="app/src/main/java/io/fluxreader/app/data/ReaderSettingsRepository.kt"
PREFERENCES="app/src/main/java/io/fluxreader/app/ui/ReaderPreferences.kt"
THEME="app/src/main/java/io/fluxreader/app/ui/FluxTheme.kt"
APP_ROOT="app/src/main/java/io/fluxreader/app/ui/FluxReaderApp.kt"
grep -q 'val experimentalDesignEnabled: Boolean = false' "$SETTINGS_MODEL" || { echo '❌ alpha.65: experiment defaults on'; exit 204; }
grep -q 'experimentalDesignEnabled && experimentalFrameTheme == ReaderThemeMode.WASTELAND_TERMINAL' "$SETTINGS_MODEL" || { echo '❌ alpha.65: Wasteland layout bypasses opt-in'; exit 205; }
grep -q 'experimentalDesignEnabled && experimentalFrameTheme == ReaderThemeMode.CYBERPUNK_NEON' "$SETTINGS_MODEL" || { echo '❌ alpha.65: Cyberpunk layout bypasses opt-in'; exit 206; }
grep -q 'putBoolean("experimentalDesignEnabledV1", next.experimentalDesignEnabled)' "$SETTINGS_STORE" || { echo '❌ alpha.65: flag not saved'; exit 207; }
grep -q 'prefs.getBoolean("experimentalDesignEnabledV1", false)' "$SETTINGS_STORE" || { echo '❌ alpha.65: migration not disabled by default'; exit 208; }
grep -q 'checked = settings.experimentalDesignEnabled' "$PREFERENCES" || { echo '❌ alpha.65: checkbox missing'; exit 209; }
grep -q 'current.copy(experimentalDesignEnabled = checked)' "$PREFERENCES" || { echo '❌ alpha.65: checkbox not wired'; exit 210; }
grep -q 'LocalFluxThemeMode provides settings.activeUiTheme' "$THEME" || { echo '❌ alpha.65: icon chrome does not use effective theme'; exit 211; }
grep -q 'val activeTheme = if (settings.experimentalDesignEnabled)' "$THEME" || { echo '❌ alpha.65: experimental color scheme not gated'; exit 216; }
grep -q 'settings.useWastelandDesign -> WastelandDeviceFrame' "$APP_ROOT" && grep -q 'settings.useCyberpunkDesign -> CyberpunkDeviceFrame' "$APP_ROOT" && test "$(grep -c 'ExperimentalUiOverlay(settings.activeUiTheme, settings, immersiveReader)' "$APP_ROOT")" -eq 2 || { echo '❌ alpha.83: glass must be inside both gated display viewports'; exit 212; }
grep -q 'settings.useWastelandDesign -> WastelandDeviceFrame' "$APP_ROOT" || { echo '❌ alpha.65: device shell bypasses gate'; exit 213; }
for SCREEN in LibraryScreen.kt ReaderScreen.kt PdfReaderScreen.kt SpeedReaderScreen.kt SettingsScreen.kt; do
  if grep -Eq 'settings\.theme[[:space:]]*(==|!=)[[:space:]]*ReaderThemeMode\.WASTELAND_TERMINAL' "app/src/main/java/io/fluxreader/app/ui/$SCREEN"; then
    echo "❌ alpha.65: $SCREEN has Wasteland UI not gated on checkbox"; exit 214
  fi
done
grep -q 'remember(baseVisibleBooks, wastelandFilter)' "app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt" || { echo '❌ alpha.92: library filter does not recompute on type selection'; exit 215; }
echo '✅ alpha.65 persisted experimental checkbox / theme/chrome/layout separation guards OK'

# alpha.67: two entirely separate shells, menu/cell widgets, opt-in activation.
CHROME="app/src/main/java/io/fluxreader/app/ui/ExperimentalChrome.kt"
LIBRARY_SCREEN="app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt"
THEMED_ICONS="app/src/main/java/io/fluxreader/app/ui/ThemeChrome.kt"
for needle in CyberpunkDeviceFrame ExperimentalPanel ExperimentalTopBar ExperimentalHardwareRail ExperimentalTab ExperimentalCoverFrame ExperimentalProgress; do
  grep -q "fun $needle" "$CHROME" || { echo "❌ alpha.67: $needle missing"; exit 220; }
done
for needle in FolderBreadcrumb FolderRow FolderActionsDialog LibraryActionButton; do
  grep -q "fun $needle" "$LIBRARY_SCREEN" || { echo "❌ alpha.67: themed $needle missing"; exit 221; }
done
for file in app/src/main/res/drawable/ic_wasteland_folder.xml app/src/main/res/drawable/ic_cyber_folder.xml; do
  test -s "$file" || { echo "❌ alpha.67: $file missing"; exit 222; }
done
grep -q 'FluxIconRole.FOLDER -> R.drawable.ic_cyber_folder' "$THEMED_ICONS" || { echo '❌ alpha.67: cyber folder icon not routed'; exit 223; }
grep -q 'FluxIconRole.FOLDER -> R.drawable.ic_wasteland_folder' "$THEMED_ICONS" || { echo '❌ alpha.67: wasteland folder icon not routed'; exit 224; }
grep -q 'settings.useCyberpunkDesign -> CyberpunkDeviceFrame' "$APP_ROOT" || { echo '❌ alpha.67: cyber shell bypasses opt-in'; exit 225; }
if grep -Eq '^import[[:space:]]+androidx\.compose\.foundation\.layout\.(matchParentSize|weight)([[:space:]]|$)' "$CHROME"; then
  echo '❌ alpha.67: invalid BoxScope or RowScope import'; exit 226
fi
if grep -Eq 'pointerInput|rememberInfiniteTransition|infiniteTransition' "$CHROME"; then
  echo '❌ alpha.67: ornamental shell must stay static/non-interactive'; exit 227
fi
echo '✅ alpha.67 Cyberpunk/Wasteland separate shells, icons, folders, actions and opt-in guards OK' 

# alpha.68: progress seeking and long-press multi-selection across all library views.
SEEK="$PWD/app/src/main/java/io/fluxreader/app/ui/ReaderProgressSeek.kt"
TEST="$PWD/app/src/test/java/io/fluxreader/app/ui/ReaderProgressSeekTest.kt"
test -s "$SEEK" && test -s "$TEST" || { echo '❌ alpha.68: seek mapping and tests missing'; exit 230; }
grep -Fq 'onSeekProgress = { fraction ->' "$PWD/app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt" || exit 231
grep -Fq 'onValueChangeFinished = {' "$PWD/app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt" || exit 232
if grep -Fq 'LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())' "$PWD/app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt"; then
 echo '❌ alpha.68: read-only progress remains in bottom bar'; exit 233
fi
LIBRARY="$PWD/app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt"
grep -Fq 'onLongClick = onStartSelection' "$LIBRARY" || exit 234
grep -Fq 'LibrarySelectionActionsMenu(' "$LIBRARY" || exit 235
grep -Fq 'onStartSelection = { onStartSelection(book) }' "$LIBRARY" || exit 236
if grep -Fq 'onLongClick = { if (selectionMode) onToggleSelection() else onManage() }' "$LIBRARY"; then
 echo '❌ alpha.68: long-press still opens old book dialog'; exit 237
fi
grep -Fq 'useWastelandDesign || settings.useCyberpunkDesign' "$LIBRARY" || exit 238
echo '✅ alpha.68 book scrub and long-press selection regression guards OK'

# alpha.69: regression guard for Compose Modifier.border extension imports.
# alpha.68 passed previous static checks but failed to compile because LibraryScreen
# used .border(...) without importing androidx.compose.foundation.border.
while IFS= read -r source; do
  if grep -Eq '\.border[[:space:]]*\(' "$source" && ! grep -Fxq 'import androidx.compose.foundation.border' "$source"; then
    echo "❌ alpha.69: Compose border extension used without import: $source"
    exit 239
  fi
done < <(find app/src/main/java -name '*.kt' -type f)
echo '✅ alpha.69 Compose Modifier.border import guard OK'

# alpha.70: narrator is additive and persists its preferences; never enabled by a UI theme alone.
NARRATOR="app/src/main/java/io/fluxreader/app/reader/ReaderNarrator.kt"
SEGMENTS="app/src/main/java/io/fluxreader/app/reader/NarrationSegments.kt"
NARRATION_SHEET="app/src/main/java/io/fluxreader/app/ui/NarrationSheet.kt"
test -s "$NARRATOR" && test -s "$SEGMENTS" && test -s "$NARRATION_SHEET" || { echo '❌ alpha.70: narration files missing'; exit 240; }
grep -Fq 'ReaderOverlay.NARRATION -> NarrationSheet(' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt || exit 241
grep -Fq 'narrationVoiceIdV1' app/src/main/java/io/fluxreader/app/data/ReaderSettingsRepository.kt || exit 242
grep -Fq 'onRangeStart' "$NARRATOR" || exit 243
grep -Fq 'TextToSpeech' "$NARRATOR" || exit 244
grep -Fq 'NarrationSegmentsTest' app/src/test/java/io/fluxreader/app/reader/NarrationSegmentsTest.kt || exit 245
grep -Fq 'android.intent.action.TTS_SERVICE' app/src/main/AndroidManifest.xml || exit 246
echo '✅ alpha.70 narration controller, language/voice, progress and Android TTS package visibility OK'

# alpha.71: audiobook screen, SAF documents, interactive player, and insets parity.
AUDIO="app/src/main/java/io/fluxreader/app/audio/AudiobookController.kt"
AUDIO_UI="app/src/main/java/io/fluxreader/app/ui/AudiobooksScreen.kt"
test -s "$AUDIO" && test -s "$AUDIO_UI" || { echo "❌ audio files absent"; exit 247; }
grep -Fq 'AppRoute.AUDIOBOOKS -> AudiobooksScreen(' app/src/main/java/io/fluxreader/app/ui/FluxReaderApp.kt || exit 248
grep -Fq 'ActivityResultContracts.OpenMultipleDocuments()' "$AUDIO_UI" || exit 249
grep -Fq 'takePersistableUriPermission' "$AUDIO" || exit 250
test -s app/src/main/java/io/fluxreader/app/audio/AudiobookFormatPolicy.kt || exit 254
grep -Fq 'safeFrameAlreadyApplied' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt || exit 251
grep -Fq 'onPlayAudio = viewModel::openAudiobook' app/src/main/java/io/fluxreader/app/ui/FluxReaderApp.kt || exit 252
if grep -Fq 'import androidx.compose.foundation.layout.weight' "$AUDIO_UI"; then
    echo '❌ scope-only Compose weight must not be imported'
    exit 253
fi
echo '✅ alpha.71 fullscreen insets + audiobook player guards OK'

# alpha.73 — opt-in fullscreen, shared audiobook rows, folder view layouts, narration bridge.
echo
 echo "▶ alpha.73 fullscreen / folder / audiobook integration"
MODELS="app/src/main/java/io/fluxreader/app/data/Models.kt"
REPO="app/src/main/java/io/fluxreader/app/data/ReaderSettingsRepository.kt"
LIB="app/src/main/java/io/fluxreader/app/ui/LibraryScreen.kt"
SPEED="app/src/main/java/io/fluxreader/app/ui/SpeedReaderScreen.kt"
AUDIO="app/src/main/java/io/fluxreader/app/audio/AudiobookController.kt"
grep -Fq 'val readerFullscreenEnabled: Boolean = false' "$MODELS"
grep -Fq 'readerFullscreenEnabledV1", false' "$REPO"
grep -Fq 'if (settings.readerFullscreenEnabled && chromeVisible' "$READER_UI"
grep -Fq 'onNarration = toggleVoice' "$SPEED"
grep -Fq 'FolderTile(folder, compact = compact' "$LIB"
grep -Fq 'AudioLibraryCard(audio, LibraryViewMode.LIST' "$LIB"
grep -Fq 'folderPath: String = ""' "$AUDIO"
grep -Fq 'fun rebaseFolder(' "$AUDIO"
if grep -Fq 'onAudiobooks' "$LIB"; then
    echo '❌ отдельная кнопка аудиокниг вернулась в библиотеку'
    exit 263
fi
if grep -Fq 'import androidx.compose.foundation.layout.matchParentSize' app/src/main/java/io/fluxreader/app/ui/WastelandUi.kt; then
    echo '❌ запрещён импорт BoxScope.matchParentSize'
    exit 264
fi
if grep -Fq '.border(' "$LIB" && ! grep -Fq 'import androidx.compose.foundation.border' "$LIB"; then
    echo '❌ потерян импорт Modifier.border'
    exit 265
fi
echo '✅ alpha.73 screen + media integration checks OK'

python3 scripts/review_alpha84.py

if (( STATIC_ONLY )); then
  echo
  echo "▶ alpha.74 reading workspace + offline narration cache"
if grep -Fq 'ReadingWorkspaceModeBar(' app/src/main/java/io/fluxreader/app/ui/FluxReaderApp.kt; then
  echo '❌ three-tab workspace must not return'; exit 281
fi
test -s app/src/main/java/io/fluxreader/app/ui/NarrationInlineBar.kt || exit 282
test -s app/src/main/java/io/fluxreader/app/reader/NarrationClipCache.kt || exit 283
test -s app/src/main/java/io/fluxreader/app/reader/NarrationPronunciationPolicy.kt || exit 284
test -s app/src/main/java/io/fluxreader/app/reader/NarrationCachePolicy.kt || exit 285
grep -Fq 'AppRoute.SPEED -> SpeedReaderScreen(' app/src/main/java/io/fluxreader/app/ui/FluxReaderApp.kt || exit 286
grep -Fq 'onSpeed = { reader.bookId?.let(viewModel::openSpeed) }' app/src/main/java/io/fluxreader/app/ui/FluxReaderApp.kt || exit 287
grep -Fq 'onTogglePlayback = togglePlayback' app/src/main/java/io/fluxreader/app/ui/SpeedReaderScreen.kt || exit 288
grep -Fq 'voiceEnabled = voiceMode' app/src/main/java/io/fluxreader/app/ui/SpeedReaderScreen.kt || exit 289
if grep -Fq 'NarrationInlineBar(' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt; then echo '❌ narration controls must be in Speed Reader only'; exit 290; fi
grep -Fq 'prefetchNext()' app/src/main/java/io/fluxreader/app/reader/ReaderNarrator.kt || exit 291
test -s app/src/main/java/io/fluxreader/app/update/GitHubUpdateManager.kt || exit 292
test -s app/src/main/java/io/fluxreader/app/reader/SpeedSpeechNarrator.kt || exit 293
test -s app/src/main/java/io/fluxreader/app/reader/SpeedSpeechText.kt || exit 294
grep -Fq 'onRangeStart(utteranceId:' app/src/main/java/io/fluxreader/app/reader/SpeedSpeechNarrator.kt || exit 295
grep -Fq 'onTogglePlay = togglePlayback' app/src/main/java/io/fluxreader/app/ui/SpeedReaderScreen.kt || exit 296
grep -Fq 'FullscreenSystemBarsEffect(!chromeVisible)' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt || exit 297
if grep -Fq 'if (settings.readerFullscreenEnabled) IconButton(' app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt; then
 echo '❌ manual fullscreen button must always be accessible'; exit 298
fi
echo '✅ alpha.76 compact Reader + integrated Speed/Speech + offline narration checks OK' 

python3 scripts/review_alpha82.py || exit 282
echo "✅ Static self-check завершён (Android SDK / Gradle пропущены)"
  exit 0
fi

echo
echo "▶ Android SDK"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
if [[ ! -d "$SDK" ]]; then
  echo "❌ Android SDK не найден: $SDK"
  echo "Запустите: scripts/bootstrap-build-apk.command"
  exit 21
fi
export ANDROID_HOME="$SDK"
export ANDROID_SDK_ROOT="$SDK"
if [[ ! -f "$SDK/platforms/android-37.0/android.jar" ]]; then
  echo "❌ Нет Android Platform 37.0: $SDK/platforms/android-37.0/android.jar"
  echo "Запустите: scripts/bootstrap-build-apk.command"
  exit 22
fi
printf 'sdk.dir=%s\n' "$SDK" > local.properties
echo "✅ SDK: $SDK"
echo "✅ local.properties обновлён автоматически"

echo
echo "▶ Gradle compile + unit tests"
[[ -x ./gradlew ]] || chmod +x ./gradlew
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleRelease --console=plain

echo

echo "✅ Self-check завершён"


# alpha.72 — neural work stays on-device; no ElevenLabs provider/key or network permission.
echo "▶ Offline narration / paired pagination font"
if grep -RinE 'ElevenLabs|elevenlabs|xi-api-key|api\.elevenlabs' app/src/main/java app/src/main/AndroidManifest.xml; then
    echo "❌ Unrequested ElevenLabs integration found"; exit 72
fi
# A network permission is required for separate GitHub Updates, not for narration.
# Keep external speech APIs forbidden while allowing the updater itself.
if grep -q 'android.permission.INTERNET' app/src/main/AndroidManifest.xml &&
   [[ ! -s app/src/main/java/io/fluxreader/app/update/GitHubUpdateManager.kt ]]; then
    echo "❌ Необъяснённое сетевое разрешение"; exit 73
fi
python3 - <<'PYCHECK'
from pathlib import Path
src=Path('app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt').read_text()
if src.count('FontFamily.Monospace') < 2:
    raise SystemExit('Expected matching experimental families in pagination AND page rendering')
if not all(k in src for k in ['settings.experimentalDesignEnabled, settings.theme','settings.fontMode,','viewportWidth.roundToPx()','ReaderPreparedPageContent']):
    raise SystemExit('Missing pagination cache dependencies')
print('  Offline-only narration; matching pagination font and theme-aware cache: OK')
PYCHECK
