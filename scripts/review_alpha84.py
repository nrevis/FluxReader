#!/usr/bin/env python3
"""Regression guards for standard Reader insets and direct narration control."""
from pathlib import Path
p=Path(__file__).resolve().parents[1]/"app/src/main/java/io/fluxreader/app/ui/ReaderScreen.kt"
s=p.read_text()
checks={
    "standard chrome system padding": "Modifier.windowInsetsPadding(WindowInsets.safeDrawing)" in s,
    "only non-immersive standard layout padded": "if (chromeVisible && !experimentalChrome)" in s,
    "no experimental double padding": "val experimentalChrome = settings.useWastelandDesign || settings.useCyberpunkDesign" in s,
    "narration no longer exposed as a reader button": "onNarrationToggle" not in s,
    "narration menu removed": 'text = { Text("Озвучивание") }' not in s,
    "speed remains reader entry": "onSpeed = openSpeedFromReader" in s,
    "not dedicated header button": "topBar" in s and "bottomBar" in s,
}
for name, ok in checks.items():
    print(("OK" if ok else "FAIL")+": "+name)
if not all(checks.values()): raise SystemExit(1)
