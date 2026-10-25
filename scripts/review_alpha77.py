#!/usr/bin/env python3
"""Independent release critics: behavior, layout, and compiler hygiene.

Source audits detect previous regressions; they do not replace Android instrumentation,
manual screenshot comparisons, or Gradle compilation on a real SDK.
"""
from pathlib import Path
import re
ROOT = Path(__file__).resolve().parent.parent
UI = ROOT / "app/src/main/java/io/fluxreader/app/ui"
reader = (UI / "ReaderScreen.kt").read_text()
speed = (UI / "SpeedReaderScreen.kt").read_text()
lib = (UI / "LibraryScreen.kt").read_text()
settings = (UI / "ReaderPreferences.kt").read_text()
checks = {
    "Критик взаимодействия": [
        ("full screen not gated by auto-hide switch", "if (!settings.readerFullscreenEnabled && !visible) return" not in reader),
        ("minimal reader overflow navigation", 'contentDescription = "Меню чтения"' in reader and 'contentDescription = "Полноэкранное чтение"' not in reader),
        ("fullscreen back restores chrome", 'BackHandler(enabled = !chromeVisible' in reader),
        ("speed manual exit", 'onToggleFullscreen = { isFullscreen = false }' in speed),
        ("voice and WPM separate", 'if (!voiceMode)' in speed and 'if (speedTuningExpanded) SpeedControls(' in speed),
        ("speed fullscreen has a voice/play/exit rail", all(s in speed for s in ["Modifier.fillMaxWidth().height(52.dp)", 'contentDescription = "Свернуть полный экран"', "onClick = toggleVoice", "onClick = togglePlayback"])),
    ],
    "Критик компоновки": [
        ("fullscreen controls measured outside viewport", 'modifier = Modifier.weight(1f).fillMaxWidth()' in speed),
        ("fullscreen context compact", 'compact = immersiveMode' in speed),
        ("context excerpt max 3 lines in full screen", 'maxLines = if (compact) 3 else 4' in speed),
        ("speed controls collapsed by default", 'var speedTuningExpanded by rememberSaveable { mutableStateOf(false) }' in speed),
        ("compact header grows for chapter text / font scaling", 'Modifier.fillMaxWidth().heightIn(min = 56.dp)' in speed and 'Text(section.ifBlank' in speed),
        ("next-word preview fits before rendering", 'nextLeftPx + previewWidthPx + gapPx < maxWidthPx' in speed),
        ("folder tiles fixed height", '.height(if (compact) 112.dp else 150.dp)' in lib),
        ("folder text contrasts with themed surface", 'if (themed) LocalFluxThemeMode.current.chromeText else MaterialTheme.colorScheme.onSurface' in lib),
    ],
    "Критик регрессий": [
        ("auto-hide still opt in", 'if (settings.readerFullscreenEnabled && chromeVisible' in reader),
        ("settings explains tap fullscreen", 'Переключение полного экрана доступно касанием области чтения.' in settings),
        ("no obsolete three-mode tabs", 'ReadingWorkspaceModeBar(' not in (UI/'FluxReaderApp.kt').read_text()),
        ("experimental design remains opt-in", 'settings.useWastelandDesign' in speed and 'settings.useCyberpunkDesign' in speed),
        ("shared folder navigation remains", 'FolderBreadcrumb(normalizedFolder, onFolder)' in lib),
    ],
}
failed = 0
for critic, entries in checks.items():
    bad = [name for name, ok in entries if not ok]
    print(f"{critic}: {len(entries) - len(bad)}/{len(entries)}")
    for name in bad:
        print("  FAIL:", name)
    failed += len(bad)
if failed:
    raise SystemExit(f"FAIL: {failed} release-blocking observations")
print("OK: 19 independent static release criteria")
