# Flux Reader 0.2.0-alpha.15.1 — release notes

This is a narrow Android compile hotfix for alpha.15. The real SDK 37 build showed that the current Compose dependency exposes `calculateTopPadding()` and `calculateBottomPadding()` as `PaddingValues` member calls, not importable top-level symbols. Alpha.15.1 removes the two invalid imports while preserving the fullscreen safe-area, display-cutout handling and line-aligned page anchors.
