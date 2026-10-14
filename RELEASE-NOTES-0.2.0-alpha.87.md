# Flux Reader 0.2.0-alpha.87 — Edge-to-edge & Experimental Palettes

- Immersive Wasteland/Cyberpunk no longer draws left/right metallic rails or applies extra horizontal screen padding. The existing Reader handles the camera cutout.
- Experimental frame and color scheme are now independent: choose Wasteland or Cyberpunk, then Original / OLED / Nord / Dracula / Rosé Pine Moon / CMX Purple / ... in Settings → Appearance.
- Existing installs keep their alpha.86 colors until a new experimental palette is explicitly selected. Experimental mode remains opt-in.
- Colors propagate to screen background, panels, borders, active tabs, Speed Reader foreground/focus, and CRT overlay. Textures and layout are preserved for non-immersive modes.
- No change to library DB, pagination, speech synthesis or audio pipeline.
- Android Gradle build and real-device rendering require verification on the user's Mac.
