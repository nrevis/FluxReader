# Flux Reader 0.2.0-alpha.86 — Standard Speed Reader UI stability

- The standard (non-experimental) Speed Reader's `Scaffold` now consumes the physical system-safe drawing region, just as the experimental device frames already do. The identical shared 56 dp header now begins **below** Android's status bar, and the control area avoids the gesture bar. In immersive Speed Reader, the safeDrawing padding is not applied, freeing the screen.
- Removed speech/play buttons from both standard and experimental **ordinary Reader** bottom bars and removed its general overflow speech shortcut. Speech toggle + playback remain accessible in **Speed Reader** only, including its immersive mode.
- Existing Speed Reader voice, pacing, word highlighting, bookmarks, reading position and reader content are unchanged.
- Introduced `scripts/review_alpha86.py` that statically checks shared geometry and Speed-only narration controls. Older alpha84 controls checks were updated to reflect the new requirement.
- Palette transfer into experimental UI is deferred to the next release; this release intentionally does not modify palette storage, selection or CRT overlay.
- Full Gradle Debug/Release builds and device layout still require Android SDK on the host Mac.
