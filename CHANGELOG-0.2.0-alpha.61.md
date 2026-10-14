# Flux Reader 0.2.0-alpha.61

## Kotlin compile hotfix

- Fixed `LibraryScreen.kt:1340 Unresolved reference: settings`.
- The regression came from the alpha.60 Wasteland bottom-padding hotfix referencing the outer `settings` variable from the private `FolderLibrary` composable, where that variable is not in scope.
- `FolderLibrary` now resolves Wasteland state from `LocalFluxThemeMode.current` and computes one local `bottomContentPadding` value.
- The same resolved padding is used by folder grid, compact, and list layouts.
- Added static guards preventing `settings.theme` from reappearing inside `FolderLibrary`.
- No persistence, book database, import, progress, annotations, or file-format logic changed.
