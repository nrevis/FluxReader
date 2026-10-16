# Flux Reader 0.2.0-alpha.47 — release notes

This build repairs the alpha.46 text-interaction regression and strengthens fullscreen position preservation.

### Text context menu
Long-press any word to open reader actions: Speed Reader from that word, copy word/sentence/paragraph, or search the word inside the book. A short tap no longer opens word actions.

### Reader rendering
The dark-theme text regression introduced by the alpha.46 `BasicText` path is fixed by applying the reader foreground color explicitly.

### Fullscreen
Normal ↔ immersive now uses a stable exact block/character anchor and passes that anchor directly into the new viewport pagination. Temporary preview pages are not allowed to become the saved reading position.

### Speed Reader
Fullscreen is now entered from the bottom playback control cluster with a dedicated outlined button; the duplicate top-bar entry is removed.
