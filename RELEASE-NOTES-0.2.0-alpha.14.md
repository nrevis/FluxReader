# Flux Reader 0.2.0-alpha.14 — release notes

This iteration focuses on three visible areas: book covers, settings discoverability and a denser library home screen.

Cover handling now tolerates common EPUBs that do not expose a direct `cover-image`: EPUB 2 guide pages and XHTML/SVG wrappers are followed to the underlying raster image, while a heuristic fallback checks plausible manifest images. Existing books are repaired automatically after upgrade. PDFs use page 1, and books with no usable artwork receive a locally generated title/author cover, so the library no longer depends on an embedded image being present.

Settings are split into focused sections instead of one long screen, with a Quick page for high-frequency controls. A new Compact library layout fits more books on screen while keeping covers, titles, authors and reading progress visible.

The alpha.13.2 behavior of Smart Speed 3.0 is retained. In full-screen speed reading, the minimal controls now hide automatically during playback and return on tap or pause.
