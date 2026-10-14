# Flux Reader 0.2.0-alpha.90 — Fullscreen & Speed checkpoint

- Wasteland and Cyberpunk fullscreen only hide frame decorations; the Reader/SpeedReader composition remains mounted, avoiding resets of its remembered fullscreen flag and cursor.
- Fullscreen content is truly edge-to-edge horizontally; the reader itself protects the camera cutout.
- Previously saved Speed Reader cursors are preferred over the normal reading chapter when opening Speed Reader. An explicit word selection still opens at the selected word.
- Pause and navigating back commits the exact active Speed Reader offset immediately to an asynchronous disk write rather than waiting for a debounced save.
- Added source critic and four JVM tests for resume policy.
- No change to book database schema, theme defaults or separate opt-in experimental design.

Android Gradle compile and on-device tests must still be performed on macOS.
