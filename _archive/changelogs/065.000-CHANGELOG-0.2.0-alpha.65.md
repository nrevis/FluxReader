# Flux Reader 0.2.0-alpha.65 — Experimental UI opt-in

- New disabled-by-default **Экспериментальный дизайн** checkbox in Settings → Appearance.
- The selected color palette is independent from the experimental layout flag.
- Wasteland/Cyberpunk chrome, text styles, shapes, themed icons, textures and overlays are displayed **only** with the checkbox enabled and the corresponding theme selected.
- Wasteland-specific Library filters, content padding, device frame and custom layout all follow the same flag.
- Guard disabled experimental design for existing installs (migration default `false`).
- Persist opt-in as `experimentalDesignEnabledV1` without modifying user books.
