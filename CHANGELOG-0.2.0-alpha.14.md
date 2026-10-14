# Flux Reader 0.2.0-alpha.14

Library artwork, settings navigation and compact-home polish on top of the verified alpha.13.2 Android build.

- Reworked EPUB cover discovery for EPUB 3 `cover-image`, EPUB 2 `meta name=cover`, `guide` cover pages, XHTML/SVG image wrappers and real-world heuristic fallbacks.
- Existing library entries with missing artwork are repaired automatically after upgrade; Settings also exposes a forced cover rescan.
- PDF books use the first rendered page as their automatic cover.
- Books without usable embedded artwork receive a deterministic generated local cover instead of an empty placeholder.
- Imported/manual artwork is decoded with bounds checks and downsampling before being stored as JPEG to keep library memory bounded.
- Manual cover images are preserved during automatic rescans; “remove cover” now returns the book to automatic artwork.
- Added `COMPACT` as a third library view next to Grid and List, with denser cards, smaller covers and a thin progress indicator.
- Compact mode collapses sorting/group controls behind a Tune button to keep the home screen dense.
- Reorganized Settings into Quick, Appearance, Reading, Library, Speed Reading, OCR and About sections.
- Quick Settings now exposes theme appearance, library layout, reader font size, WPM, Continue block and cover repair without hunting through a long page.
- Library Settings includes display mode, grouping and a cover repair action.
- Full-screen Speed Reader controls now auto-hide while playing and reappear on tap or pause.
- Replaced several deprecated directional Material icons with AutoMirrored variants and removed deprecated direct system-bar color writes.
- Added EPUB 2 wrapper-cover regression coverage to unit tests and retained Smart Speed 3.0 tests.
