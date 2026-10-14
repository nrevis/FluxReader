# Flux Reader 0.2.0-alpha.6

## Reader navigation parity

- Horizontal swipes no longer scroll the current viewport by 88%.
- Swipe left and right edge tap use the same `readerNext()` action as the right navigation button.
- Swipe right and left edge tap use the same `readerPrevious()` action as the left navigation button.
- Center tap still toggles reader chrome.
- Vertical scrolling remains available as an independent fallback; it is not used to emulate horizontal page turns.
- Added a self-check guard so viewport-based horizontal navigation cannot silently return.
