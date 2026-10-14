# Flux Reader 0.2.0-alpha.64

## Wasteland measurement fix

- Fixed the root cause of the giant Wasteland panels seen on Pixel 9 Pro: the decorative texture inside `WastelandPanel` used `fillMaxSize()`, which participated in parent measurement and could force wrap-content panels to consume the whole available height.
- The texture is now a measurement-neutral `matchParentSize()` child. Panel size is determined only by its real UI content.
- This fixes Wasteland top bars, bottom hardware rails, book cards, settings panels and reader control panels at the shared primitive level instead of adding more screen-specific padding workarounds.
- The strict Library layout from alpha.63 remains: top bar, weighted book viewport, bottom hardware rail.
- Added a regression guard that rejects `fillMaxSize()` in the `WastelandPanel` decorative layer and requires `matchParentSize()`.
