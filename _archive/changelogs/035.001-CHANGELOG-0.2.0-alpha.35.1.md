# Flux Reader 0.2.0-alpha.35.1

- Replaced deprecated `Icons.Default.Undo` / `Icons.Filled.Undo` with `Icons.AutoMirrored.Filled.Undo`.
- Added a regression guard so directional Undo does not regress to the deprecated non-mirrored icon.
- No behavior change to OCR/PDF native dependencies. `stripReleaseDebugSymbols` may still report already-stripped third-party `.so` files; they remain packaged as supplied by their dependencies.
