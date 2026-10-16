# Flux Reader 0.2.0-alpha.46

- Fixed Reader position loss across normal ↔ immersive viewport changes with an explicit pending viewport anchor.
- Removed the fullscreen scroll line-snap that could alter the visible offset.
- Kept the previous prepared page visible while the new viewport pagination is measured; no blocking preparation screen on fullscreen transitions.
- Tightened fullscreen HUD/page guards and scroll bottom reserve for denser, more natural text use.
- Replaced the labeled Reader speed pill with an icon-only circular red-outline action.
- Moved “speed reading from selected word” out of the overflow menu into a contextual word-tap action with visible word highlighting.
- Added one explicit fullscreen icon that enters the same single immersive state.
- ZIP archive import from alpha.45 remains intact.
