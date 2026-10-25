# alpha.82 — Pixel 9 Pro device QA (Android 17)

This checklist requires an actual build and installation. Static checks cannot certify gestures or visual layout.

1. Open an EPUB, FB2 or TXT book using standard theme; confirm the title and navigation occupy one 52 dp toolbar; check Back, chapter title and ⋮.
2. Confirm Search, Bookmarks/Notes, Text settings, Narration and Table of Contents are reachable through ⋮.
3. Tap page center: verify immersive hides top and bottom chrome and system bars, but does not obscure the camera cutout. Tap again: controls return. Verify reading location unchanged.
4. Repeat in Wasteland and Cyberpunk with the experimental-design checkbox enabled; then disable checkbox and verify standard geometry.
5. In Wasteland Settings > Appearance disable CRT and observe disappearance of all CRT scanlines/noise/glow/vignette. Toggle individual components and intensity. Restart app and confirm selections persist.
6. In Speed Reader > Settings > Voice and Narration inspect installed offline voices and pick a Russian one, adjust rate and pitch. Verify Play speaks while center word follows only when TTS exposes word range events. Turn voice off to restore WPM.
7. Open/close nested effects and voice sheets via Back and Close without accidentally exiting Speed Reader.
8. Reopen library: all book folders, cover cache and reading progress are retained.
9. Run release build and install on your Mac; inspect `adb logcat` for Compose crashes and TTS errors.

Release gate: no regressions in core library operations, fullscreen tap, or context text visibility.
