# Flux Reader 0.2.0-alpha.83 — Display Glass Pass

- Fixed the placement of experimental effects: drawn inside the Wasteland/Cyberpunk CRT/HUD viewport **after route content** rather than over the whole device frame.
- Effects now cover all app routes using the experimental shell: library/home, folders, Reader, Speed Reader, settings, audiobook screen.
- Increased visible scanline contrast and adjusted phosphor glow, fixed grain and radial vignette; kept effects static and without gesture handlers.
- Increased the default intensity for NEW installs to 65%. Existing saved intensity remains unchanged.
- Preserved master CRT and individual effect switches, including a genuinely blank effect layer at 0%.
- Added `scripts/review_alpha83.py` to release self-check.
- Did not modify app navigation, books, user library, or spoken content engines.

**Caveat:** on-device Android screenshots and final Gradle compilation are required to verify visibility on OLED Pixel 9 Pro.
