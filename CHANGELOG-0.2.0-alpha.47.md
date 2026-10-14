# Flux Reader 0.2.0-alpha.47

## Reader text interaction
- Replaced single-tap word selection with long-press contextual actions.
- Added copy word / sentence / paragraph, search-in-book, and Speed Reader from selected word.
- Added pure `ReaderTextActionPolicy` and unit tests.
- Fixed dark-theme `BasicText` color regression by explicitly applying reader foreground color.
- Link taps and long presses now coexist with parent edge/center/swipe gestures without consuming ordinary taps.

## Fullscreen position
- Added a stable exact viewport anchor independent from temporary preview pagination.
- Immersive transitions persist the exact block/character anchor before geometry changes.
- Preview pagination receives the pending anchor directly.
- The pending transition anchor is cleared only after complete pagination is accepted.
- Added anchor geometry regression test.

## Speed Reader
- Moved fullscreen entry out of the top app bar and into the playback controls.
- Kept one fullscreen state only.
