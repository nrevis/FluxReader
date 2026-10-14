# Flux Reader 0.2.0-alpha.7

This build extends the 0.2 clean reader with book-native navigation. EPUB hyperlinks and footnote anchors are no longer flattened away: the reader can open an internal target and return to the exact previous viewport.

## Verify on device

1. Open an EPUB with footnotes or internal chapter links.
2. Tap a linked note/chapter reference.
3. Confirm the target opens at the anchored paragraph rather than only at the chapter start.
4. Tap the history icon in the reader top bar and confirm the previous viewport is restored.
5. Confirm ordinary left/right tap zones and horizontal swipes still turn exactly one page.
6. Open an external link and confirm Android delegates it to the system handler without requesting INTERNET permission from Flux Reader.
