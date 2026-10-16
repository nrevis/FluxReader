# Flux Reader 0.2.0-alpha.63 — Wasteland layout hotfix

This build fixes the Wasteland Library screenshot regression where the hardware navigation rail appeared near the top, the library viewport was overlapped/mostly invisible, and visible buttons had unreliable actions.

The Wasteland Library no longer uses Material Scaffold for its themed chrome. It now has a strict three-region layout: top header, weighted content viewport, bottom hardware rail. Visible hardware buttons own the click handlers directly.
