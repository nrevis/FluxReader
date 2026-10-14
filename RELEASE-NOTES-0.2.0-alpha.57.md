# Flux Reader 0.2.0-alpha.58

This build starts the experimental interface branch requested for Flux Reader without replacing the normal UI.

Choose a new look in **Настройки → Оформление → Палитра**:

- **Wasteland CRT** — an industrial retro-terminal interface with amber controls, phosphor-green text, squared metal-like geometry and subtle scanlines.
- **Cyberpunk** — a dark futuristic HUD with cyan/magenta accents, clipped corners and a fine neon grid.

The styles affect the whole application shell (library, reader controls, settings, dialogs and Material components). Reading logic, library data, gestures and Speed Reader are not forked, so switching back to OLED/Flux/Paper/etc. is immediate and safe.

The two concept boards that defined the visual direction are included in `docs/concepts/` for further iteration.
