#!/usr/bin/env python3
"""Build gate for the Pixel hole-punch/status/navigation inset contract.

This is a source-level audit; on-device screenshot/instrumentation is still needed
for verifying actual pixel alignment and font pagination.
"""
from pathlib import Path
import re
root=Path(__file__).resolve().parent.parent
ui=root/'app/src/main/java/io/fluxreader/app/ui'
get=lambda f:(ui/f).read_text()
app=get('FluxReaderApp.kt'); w=get('WastelandUi.kt'); c=get('ExperimentalChrome.kt')
r=get('ReaderScreen.kt'); s=get('SpeedReaderScreen.kt'); cam=get('ImmersiveCameraClearance.kt')
checks={
'Root owns route-specific immersive state': 'remember(route) { mutableStateOf(false) }' in app,
'Root passes immersive to Wasteland': 'WastelandDeviceFrame(immersive = immersiveReader)' in app,
'Root passes immersive to Cyberpunk': 'CyberpunkDeviceFrame(immersive = immersiveReader)' in app,
'Root passes callbacks to both Readers': app.count('onImmersiveChanged = reportImmersive') == 2,
'Wasteland does not reserve safeDrawing when bars hidden': 'ImmersiveLayoutPolicy.reserveSystemInsets(immersive)' in w,
'Cyberpunk does not reserve safeDrawing when bars hidden': 'ImmersiveLayoutPolicy.reserveSystemInsets(immersive)' in c,
'Reader reports immersive when chrome hidden': 'onImmersiveChanged(!chromeVisible)' in r,
'Speed reports immersive on manual full screen': 'onImmersiveChanged(isFullscreen)' in s,
'Camera pad is based on physical cutout': 'WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()' in cam,
'No status bar in immersive camera policy': 'safeDrawing' not in cam.replace('Never use safeDrawing here',''),
'Reader does not measure extra HUD row': 'The upper camera lane is an overlay' in r and 'modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)' in r,
'Reader page reserves only camera top': 'top = cameraTop' in r,
'Speed no longer uses safeDrawing for immersive content': '.padding(top = ImmersiveCameraClearance.current())' in s,
'Old forced 36dp status band gone': 'maxOf(statusTop, cutoutTop, 36.dp)' not in r,
'Manual full screen remains ungated': 'if (!settings.readerFullscreenEnabled && !visible) return' not in r,
}
for k,v in checks.items(): print(('PASS' if v else 'FAIL'),k)
if not all(checks.values()):raise SystemExit(1)
print('Pixel immersive layout critic: %d/%d structural checks passed' %(sum(checks.values()),len(checks)))
