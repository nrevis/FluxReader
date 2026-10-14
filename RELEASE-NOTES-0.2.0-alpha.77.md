# Flux Reader 0.2.0-alpha.77 — interaction and layout stabilization

- Manual fullscreen works even when auto-hide is disabled; the preference now accurately describes automatic chrome hiding.
- Speed Reader fullscreen uses separate progress, word viewport and 52dp control row: context preview never shares the controls’ layout region.
- The context preview is limited to three lines in fullscreen; vertical fixation reserves its size.
- A compact 56dp header replaces the heavy experimental Speed Reader top bar.
- WPM/pacing tuning is collapsed by default; tap the WPM label to expand it.
- The optional right-hand preview word appears only when it fits completely.
- Folder tiles have fixed heights, centered two-line titles, and theme-contrast-safe colors.
- Speed fixation guide capped in height to reduce visual noise.
- Added a three-critic static review gate (`scripts/review_alpha77.py`) plus an explicit manual device QA checklist (`docs/ALPHA77-DEVICE-QA.md`).

**Validation boundary**: Static checks only. Full Android/Kotlin Gradle build and visual device interaction still require a local Android SDK and Pixel 9 Pro.
