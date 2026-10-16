# Flux Reader 0.2.0-alpha.88 — Kotlin compile fix

- Fixed unresolved `ReaderSettings.activeUiTheme` in `ExperimentalColors.kt` by importing the extension declared in `data/Models.kt`.
- Added `scripts/check_extension_imports.py` to catch missing imports for `ReaderSettings` extension properties across UI code; included a negative regression test and integrated it into `scripts/selfcheck.command`.
- Updated versionName to `0.2.0-alpha.88` and versionCode to `20105`, including Gradle metadata and installer name.
- No changes to reading UI, saved library data, palettes, or fullscreen behavior.

Static checks do not replace a full Android Gradle build. Run both `:app:compileDebugKotlin` and `:app:compileReleaseKotlin` on a machine with the Android SDK before treating this build as validated.
