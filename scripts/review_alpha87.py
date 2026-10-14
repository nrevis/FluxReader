#!/usr/bin/env python3
"""Static design/behavior audit for alpha.87; not an emulator/screenshot test."""
from pathlib import Path
import argparse

root = Path(__file__).resolve().parent.parent
ui = root / 'app/src/main/java/io/fluxreader/app/ui'
data = root / 'app/src/main/java/io/fluxreader/app/data'
r = {p.stem:p.read_text() for p in ui.glob('*.kt')}
models = (data/'Models.kt').read_text()
repo = (data/'ReaderSettingsRepository.kt').read_text()

def checks(wasteland=None, cyber=None):
    w = wasteland if wasteland is not None else r['WastelandUi']
    c = cyber if cyber is not None else r['ExperimentalChrome']
    def stable_frame(src, token):
        part = src[src.index(f'internal fun {token}('):]
        # Switching an entirely different Box branch used to dispose the nested
        # screen, clearing rememberSaveable fullscreen state and causing oscillation.
        return ('if (!immersive) {' in part and
                'if (immersive) {' not in part.split('if (!immersive) {', 1)[0] and
                'return' not in part.split('if (!immersive) {', 1)[0] and
                'content = content' in part)
    return {
        'Wasteland fullscreen retains stable routed subtree': stable_frame(w, 'WastelandDeviceFrame'),
        'Cyberpunk fullscreen retains stable routed subtree': stable_frame(c, 'CyberpunkDeviceFrame'),
        'immersive device frame has full available horizontal width': (
            '.padding(horizontal = if (immersive) 0.dp else 10.dp' in w and
            '.padding(horizontal = if (immersive) 0.dp else 8.dp' in c),
        'CRT effect overlay receives full-screen state': r['FluxReaderApp'].count('ExperimentalUiOverlay(settings.activeUiTheme, settings, immersiveReader)') == 2,
        'Cyberpunk HUD side rails disabled in fullscreen': 'if (!immersive) CyberpunkHudOverlay()' in r['ExperimentalUiOverlay'],
        'experimental skin stays opt-in': 'experimentalDesignEnabled && experimentalFrameTheme' in models,
        'old skin choice auto-migrates from theme': 'theme == ReaderThemeMode.CYBERPUNK_NEON' in models and 'experimentalFrameStyle ?: ' in models,
        'palette and frame have independent persisted settings': all(s in repo for s in ('putString("experimentalFrameStyleV1"','putString("experimentalPaletteV1"','prefs.getString("experimentalFrameStyleV1"','prefs.getString("experimentalPaletteV1"')),
        'null experimental palette preserves original colors': 'if (settings.experimentalPalette == null)' in r['ExperimentalColors'],
        'palette is selected only in experiment': 'if (settings.experimentalDesignEnabled)' in r['FluxTheme'] and 'settings.experimentalPalette ?: settings.activeUiTheme' in r['FluxTheme'],
        'both shells use semantic adaptive text and accents': 'val pal = LocalExperimentalColors.current' in w and 'val pal = LocalExperimentalColors.current' in c and w.count('pal.accent') >= 8 and c.count('pal.accent') >= 8,
        'Speed Reader adopts background/text/accent from experimental palette': all(x in r['SpeedReaderScreen'] for x in ('-> pal.background','-> pal.text','-> pal.accent')),
        'CRT scans inherit accent without touching taps': 'ExperimentalScreenEffects(settings, palette.accent)' in r['ExperimentalUiOverlay'] and 'pointerInput' not in r['ExperimentalUiOverlay'],
        'palette choice offered in experimental settings only': 'if (settings.experimentalDesignEnabled) {' in r['ReaderPreferences'] and '"Цвета экспериментального дизайна"' in r['ReaderPreferences'],
        'icon geometry preserved and optional monochrome tint': 'val tint = if (colors.overridden) colors.accent else Color.Unspecified' in r['ThemeChrome'],
        'normal mode palette is untouched': 'else settings.activeUiTheme' in r['FluxTheme'] and 'settings.activeUiTheme' in r['FluxTheme'],
    }

p = argparse.ArgumentParser()
p.add_argument('--self-test', action='store_true')
args = p.parse_args()
results = checks()
for k,ok in results.items():
    print(('PASS' if ok else 'FAIL'),k)
if not all(results.values()):raise SystemExit(1)
if args.self_test:
    w = r['WastelandUi'].replace('if (!immersive) {', 'if (immersive) {',1)
    assert not checks(wasteland=w)['Wasteland fullscreen retains stable routed subtree']
    c = r['ExperimentalChrome'].replace('if (!immersive) {','if (immersive) {',1)
    assert not checks(cyber=c)['Cyberpunk fullscreen retains stable routed subtree']
    print('PASS negative tests: shell content cannot switch to a separate fullscreen branch')
print(f'PASS {len(results)}/{len(results)} structural design checks')
