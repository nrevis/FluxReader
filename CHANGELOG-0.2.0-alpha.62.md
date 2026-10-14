# Flux Reader 0.2.0-alpha.62

## LibraryScreen Kotlin compile hotfix

- Fixed `LibraryScreen.kt:598 Unresolved reference: bottomContentPadding` introduced in alpha.61.
- The root `LibraryScreen` now owns `libraryBottomContentPadding` and `libraryCompactBottomContentPadding`, derived from `settings.theme`, where `settings` is in scope.
- Root GRID, COMPACT, and LIST layouts now consume those explicit root-level values.
- `FolderLibrary` keeps its independent CompositionLocal-based `bottomContentPadding`; no cross-scope variable is shared between the two composables.
- Added a regression guard that rejects a bare `bottomContentPadding` reference in the root LibraryScreen content block.
- Wasteland full-screen viewport, safe-area handling, bottom rail placement, persistence, library database, imported books, reading progress, and annotations are unchanged.
