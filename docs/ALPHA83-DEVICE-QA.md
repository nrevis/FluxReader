# alpha.83 device QA — Pixel 9 Pro

1. In Settings → Appearance enable Experimental design, select Wasteland.
2. Set CRT on, scanlines on, glow on, vignette on; intensity to 100%: verify readily visible horizontal raster on Library, *including* green list / empty region, folder view and Reader.
3. Turn intensity down to 30%: lines should still be visible but softer.
4. Set intensity to 0%: CRT scanlines, glow, vignette and grain must disappear; decorative theme textures and metal bezel remain.
5. Turn off CRT: all four subeffects disappear, without changing theme or navigation.
6. Toggle scanlines, noise, glow and vignette independently; verify each responds.
7. Switch to Cyberpunk with Experimental enabled; verify effects over library, Reader, Speed Reader and Settings.
8. Disable Experimental design: default UI must not show the overlay even if theme remains Wasteland/Cyberpunk.
9. Tap menu buttons, scroll the library, swipe page, change slider, switch fullscreen; no effect layer may capture touch.
10. Check text legibility at 100% intensity, especially during long reading. Save screenshot of library and fullscreen reading for review.

Gradle debug and release compilation, plus real phone rendering, are mandatory before tagging as verified.
