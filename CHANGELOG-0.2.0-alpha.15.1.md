# Flux Reader 0.2.0-alpha.15.1

Compile hotfix over alpha.15.

- Removed invalid explicit Compose imports for `calculateTopPadding` and `calculateBottomPadding` from `ReaderScreen.kt`.
- Kept the alpha.15 fullscreen/cutout and line-safe pagination implementation unchanged.
- Added a static self-check guard so these two unsupported imports cannot re-enter the Android source tree.
- Version bumped to 0.2.0-alpha.15.1 / 20021.
