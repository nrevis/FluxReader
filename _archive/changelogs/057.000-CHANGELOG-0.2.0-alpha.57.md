# Flux Reader 0.2.0-alpha.58

## Experimental full-app skins

- Added **Wasteland CRT** as an experimental full-interface skin:
  - deep phosphor green / oxidized olive surfaces;
  - amber primary controls;
  - compact industrial corner geometry;
  - monospaced UI typography;
  - lightweight CRT scanlines and terminal corner markers.
- Added **Cyberpunk** as an experimental full-interface skin:
  - dark navy HUD surfaces;
  - cyan primary and magenta secondary accents;
  - cut-corner cards, chips, dialogs and controls through the global Material shape set;
  - thin non-interactive HUD grid and neon perimeter layer.
- Both skins are selectable from **Settings → Appearance → Palette** and are persisted in ReaderSettings.
- Experimental skins intentionally force a dark canvas so Android light mode cannot destroy their contrast.
- Existing themes and reader behavior remain available unchanged.

## Stability / performance rules

- The experimental overlay is static: no animation loop, no pointer input, no timers and no permanent recomposition source.
- The overlay does not participate in gestures and therefore cannot steal reader swipes, taps or long presses.
- No generated concept artwork is loaded at runtime; concept PNGs are documentation only under `docs/concepts/`.

## Version

- versionName: `0.2.0-alpha.58`
- versionCode: `20074`
