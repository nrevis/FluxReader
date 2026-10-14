# Flux Reader 0.2.0-alpha.75

Fixes Kotlin compilation failure `CONFLICTING_IMPORT` for `Icons` in `ReadingWorkspaceModeBar.kt`.
Run `./scripts/selfcheck.command --static` and `./gradlew :app:compileDebugKotlin :app:compileReleaseKotlin` on the Mac before the release build.

This is a narrowly scoped compile hotfix. Bundled local sherpa-onnx JNI/models, native background playback via MediaSession and Android Foreground Service remain follow-up milestones.
