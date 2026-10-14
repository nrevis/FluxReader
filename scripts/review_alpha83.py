#!/usr/bin/env python3
"""alpha.83 release critic: CRT glass compositing, gating, contrast and prefs.
This is a static verifier: it does not prove device screenshots look correct.
"""
from pathlib import Path
import re
root=Path(__file__).resolve().parent.parent
ui=root/'app/src/main/java/io/fluxreader/app/ui'
app=(ui/'FluxReaderApp.kt').read_text()
fx=(ui/'ExperimentalUiOverlay.kt').read_text()
models=(root/'app/src/main/java/io/fluxreader/app/data/Models.kt').read_text()
repo=(root/'app/src/main/java/io/fluxreader/app/data/ReaderSettingsRepository.kt').read_text()
checks={
 'only opt-in experimental shells': 'settings.useWastelandDesign -> WastelandDeviceFrame' in app and 'settings.useCyberpunkDesign -> CyberpunkDeviceFrame' in app,
 'glass is above EVERY route in both display viewports': bool(re.search(r'WastelandDeviceFrame\(immersive = immersiveReader\) \{\s*routeContent\(\)\s*//[^\n]*\n\s*//[^\n]*\n\s*ExperimentalUiOverlay\(', app)) and bool(re.search(r'CyberpunkDeviceFrame\(immersive = immersiveReader\) \{\s*routeContent\(\)\s*ExperimentalUiOverlay\(', app)),
 'no full-device overlay that tints metal frame': app.count('ExperimentalUiOverlay(settings.activeUiTheme, settings, immersiveReader)') == 2 and 'if (settings.experimentalDesignEnabled) ExperimentalUiOverlay' not in app,
 'readers and library share root overlay': 'routeContent()' in app and 'AppRoute.LIBRARY' in app and 'AppRoute.READER' in app and 'AppRoute.SPEED' in app and 'AppRoute.SETTINGS' in app,
 'master toggle and slider off work': 'if (!settings.experimentalCrtEnabled) return' in fx and 'if (strength <= 0f) return' in fx,
 'scanlines visible on old 30 percent preference': '0.35f * strength' in fx and '0.20f * strength' in fx,
 'all four subeffects obey their switches': all(('if (settings.experimental'+x+'Enabled)' in fx) for x in ('Scanlines','Glow','Noise','Vignette')),
 'no pointer capture': 'pointerInput' not in fx and '.clickable(' not in fx,
 'cached previous user preference preserved': 'experimentalEffectIntensityV1' in repo,
 'new installs have stronger visible defaults': 'experimentalEffectIntensity: Float = 0.65f' in models and 'experimentalEffectIntensityV1", 0.65f' in repo,
}
for name, ok in checks.items(): print(('PASS' if ok else 'FAIL'),name)
if not all(checks.values()): raise SystemExit(1)
print(f'PASS {len(checks)}/{len(checks)} alpha.83 display effect criteria')
