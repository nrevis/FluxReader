# Flux Reader 0.2.0-alpha.75 — Kotlin compile hotfix

## Compiler failure from alpha.74
- Removed repeated `Icons`, `ArrowBack`, `Icon`, and `IconButton` imports from `ui/ReadingWorkspaceModeBar.kt`.
- Added `scripts/check_kotlin_imports.py` to reject duplicate and ambiguous class/object-style explicit Kotlin imports, plus known invalid Compose scope-only imports.
- Added seven self-test scenarios exercising the import guard (including intentional regression).
- Integrated guard in `scripts/selfcheck.command`, for both static and full runs.

## Compatibility
- Inherits the alpha.74 unified Read/Speed/Listen workspace and offline TTS clip cache without changing user data.
- No sherpa-onnx native JNI/model bundled in this hotfix; no MediaSession foreground service added in this hotfix.
- Requires Android SDK to validate full debug/release Kotlin compilation on user's Mac.
