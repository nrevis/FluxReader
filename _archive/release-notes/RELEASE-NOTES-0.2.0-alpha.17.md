# Flux Reader 0.2.0-alpha.17 — release notes

This build changes how normal book pages are produced rather than adding another clipping offset.

In **Screen Paging**, Flux Reader first measures the current chapter for the actual reader viewport and then renders one already-fitted page at a time. Page position is stored as a character anchor, so changing screen size or typography triggers a fresh pagination without relying on stale pixels.

Expected device result: no half-cut last/first text rows at page boundaries; fullscreen text remains below the camera HUD; rotation and typography changes keep the reader near the same textual position.

The source includes static regression guards. A full Android/Compose build still has to be run on the macOS SDK 37 environment because Android SDK 37 is not installed in the isolated build container used for this package.
