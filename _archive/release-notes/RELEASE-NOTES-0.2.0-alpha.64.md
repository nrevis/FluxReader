# Flux Reader 0.2.0-alpha.64 — Wasteland panel sizing repair

This build addresses the screenshot where the LIBRARY panel stretched almost to the bottom of the display, hiding filters, the book grid and the hardware navigation rail.

The issue was in the shared Wasteland panel primitive, not in the book database or grid. Its decorative texture was measured with `fillMaxSize()`, so a panel that should wrap its content could request the entire parent height. The texture now uses `matchParentSize()`, which draws into the final measured panel without influencing layout.

The fix applies to every Wasteland panel: Library header, bottom rail, book cards, Settings sections, Reader controls, PDF controls and Speed Reader controls.
