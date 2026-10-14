# Flux Reader 0.2.0-alpha.80

- Fix Kotlin compile error `Unresolved reference weight` in `ReaderScreen.kt`: remove `.weight(1f)` from `ReaderContent` inside a `Box`. `weight` is only available in a `RowScope`/`ColumnScope`; `ReaderContent` already fills its Box using `fillMaxSize()`.
- Add a targeted source-level regression gate `scripts/check_reader_fullscreen_scope.py` and negative tests that demonstrate detection of the original bug.
- Preserve the Pixel 9 Pro immersive layout and library data unchanged. No other Reader behavior was intentionally modified.
- No local Android SDK: the full Kotlin Android build has **not** been verified here.
