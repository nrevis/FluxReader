# Flux Reader 0.2.0-alpha.9 — release notes

This build targets the speed-reading rendering problems visible in the supplied screenshots: long RSVP words and multi-word PHRASE/FOCUS chunks could wrap or overlap around the fixation axis. The display now measures text before drawing and automatically reduces it when necessary. The fixation guide also leaves a gap around the text instead of crossing the glyphs.

Speed Reader now has substantially more typography and guide customization, including font family, weight, spacing, line height, width, vertical position, focus style and line-count controls.

JetBrains Nerd is supported as a user-imported `.ttf/.otf` font for both the normal Reader and Speed Reader. The project intentionally does not bundle or redistribute a font file; if none is imported, JetBrains mode safely falls back to Monospace.
