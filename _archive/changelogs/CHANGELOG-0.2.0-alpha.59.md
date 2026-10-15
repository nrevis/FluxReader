# Flux Reader 0.2.0-alpha.59

## Experimental Wasteland full-interface overhaul

This build turns the Wasteland theme into a separate, fully programmed interface layer instead of a palette/texture variation.

- Added a static physical terminal shell around the app: rusty metal bezel, vent slots, screws, hazard marks and an amber power lamp.
- Added CRT glass/scanline treatment without animation or pointer interception.
- Added reusable Wasteland primitives: metal panels, section plates, hardware buttons, tabs, progress bars, cover frames and bottom hardware rails.
- Library now has a Wasteland-specific top panel, ALL/BOOKS/PDF/TXT filter row, hard-edged book cards, amber progress treatment and a four-button hardware rail.
- Reader now uses a terminal chapter header, monospaced Wasteland text treatment and physical previous/next/Aa/speed controls.
- PDF reader receives the same terminal header and Wasteland navigation/progress panel.
- Speed Reader now uses Wasteland colors even when independent speed colors were configured, plus terminal header, hardware playback controls and physical pacing tabs.
- Settings now use Wasteland section tabs, textured panels and terminal-style value/switch rows while keeping every existing preference functional.
- Existing non-Wasteland themes retain their previous interface paths.
- Decorative layers remain static and touch-transparent to avoid stealing Reader gestures or causing permanent recomposition/animation work.
