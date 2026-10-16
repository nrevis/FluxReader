# Flux Reader 0.2.0-alpha.85

- Standard and experimental Speed Reader now share the same 56 dp toolbar, viewport, player controls, speed settings and fullscreen geometry; only themed icons/colors differ.
- Readability fixes in experimental Appearance Settings: explicit text colors on headings and chips, single-line system/light/dark appearance chips, theme palette uses two even-width columns.
- `scripts/profile-ui-shells.command` records Android `gfxinfo` before/after equivalent user interactions to compare frame pacing with standardized conditions. This release does not claim a measured speed-up.
- Background library, imported content and narration behavior unchanged.
- Android build and Pixel hardware performance to be validated locally.
