#!/usr/bin/env python3
"""Alpha.86: cross-skin speed window-insets and speech-control regression guards."""
from pathlib import Path
import re
root = Path(__file__).resolve().parent.parent
ui = root / "app/src/main/java/io/fluxreader/app/ui"
speed = (ui / "SpeedReaderScreen.kt").read_text()
reader = (ui / "ReaderScreen.kt").read_text()
app = (ui / "FluxReaderApp.kt").read_text()
checks = {
    "standalone speed consumes the safe system area": "Modifier.windowInsetsPadding(WindowInsets.safeDrawing)" in speed,
    "standard-only inset guard": "!workspaceEmbedded && !wastelandTheme && !cyberTheme" in speed,
    "scaffold uses the guarded modifier": "Modifier.fillMaxSize().then(standaloneSafeArea)" in speed,
    "fullscreen has its own viewport": speed.index("if (isFullscreen) {") < speed.index("val standaloneSafeArea"),
    "experimental frames still own their safe area": "WastelandDeviceFrame(immersive = immersiveReader)" in app and "CyberpunkDeviceFrame(immersive = immersiveReader)" in app,
    "single Speed toolbar layout": speed.count("height(56.dp)") >= 1 and "TopAppBar(" not in speed,
    "Speed voice toggle exists": "onClick = toggleVoice" in speed or "onNarration = toggleVoice" in speed,
    "Speed can play speech": "speech.start(state.chapterText" in speed,
    "Reader bottom bar takes no voice callbacks": "onNarrationToggle" not in reader and "narrationPlaying:" not in reader,
    "Reader standard and experimental buttons contain no speech": '"Озвучить текст"' not in reader,
    "Reader overflow voice shortcut removed": 'text = { Text("Озвучивание") }' not in reader,
    "Reader still links to speed": "onSpeed = onSpeed" in reader and 'contentDescription = "Скорочтение"' in reader,
}
for name, ok in checks.items(): print(("PASS" if ok else "FAIL") + ": " + name)
if not all(checks.values()): raise SystemExit(1)
