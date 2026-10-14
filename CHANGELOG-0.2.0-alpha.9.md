# Flux Reader 0.2.0-alpha.9

Speed Reader layout and typography overhaul based on the reported Pixel screenshots.

- Reworked RSVP rendering so long words cannot wrap the left/right ORP halves into each other.
- Added measured auto-fit using Compose `TextMeasurer`, with a configurable minimum font size.
- PHRASE and FOCUS default to one-line measured fitting; users can allow up to three lines.
- PHRASE keeps the whole phrase at full contrast; FOCUS dims surrounding words.
- Added character-focus vs whole-word-focus highlighting.
- Replaced the line-through-text guide with two separated guide segments and optional edge ticks.
- Added speed font family, weight, letter spacing, line height, content width and vertical-position controls.
- Added guide height/width/tick controls and optional progress bar.
- Added JetBrains Nerd font mode to both normal Reader and Speed Reader.
- Added local `.ttf/.otf` import through Android SAF; imported font is copied into private app storage.
- JetBrains mode falls back to Monospace until a user font is imported.
- No font binary is bundled with the source package.
- Speed top app bar now follows the selected speed-reader background/text colors.
