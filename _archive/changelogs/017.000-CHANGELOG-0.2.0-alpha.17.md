# Flux Reader 0.2.0-alpha.17

Screen-prepagination rewrite for the normal Reader.

- Replaced pixel-derived `LazyColumn` page boundaries in **Screen Paging** with prepared physical pages measured for the current viewport.
- Added `ReaderPagination.kt`: the current chapter is measured with Compose `TextMeasurer`, using the same font family, font scale, font size, line height, alignment, hyphenation, indentation, margins and list width as the renderer.
- Page cuts are selected only at complete measured line boundaries with a small bottom safety reserve, so a page is never intentionally anchored inside a glyph row.
- Very large text blocks are measured through bounded 32 KiB windows instead of repeatedly shaping the whole remaining paragraph; the coroutine yields while preparing long chapters.
- Normal word boundaries are kept together at page edges when possible; unavoidable very long words still make forward progress.
- Added persistent **block + character offset** reading positions. New page turns no longer depend on a device-specific pixel offset.
- Existing alpha.16.x pixel positions are migrated approximately once and then replaced by the exact character anchor after the prepared page is shown.
- Font, margin, line-height, alignment, orientation, fullscreen and viewport changes repaginate the chapter while preserving the current character position.
- Previous/next chapter transitions use exact prepared-page anchors; returning from EPUB/FB2 links also restores the character position.
- Images in Screen Paging use dedicated pages with `ContentScale.Fit`, so image decoding/aspect ratio cannot push following text outside the viewport.
- Fullscreen camera HUD now reports page-aware progress on the left and the current prepared page on the right, leaving the center clear for the camera cutout.
- Continuous **Scroll** mode remains available and keeps its existing measured-line settling behavior.

Version: `0.2.0-alpha.17` / `versionCode 20025`.
