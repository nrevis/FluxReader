# Flux Reader 0.2.0-alpha.38

## Reader perceived performance

- Added anchor-aware one-page preview for screen paging. Reopening in the middle of a chapter paints useful text at the saved block/character position before exact pagination of the whole chapter completes.
- Preview pagination is explicitly incomplete: it is never stored in the LRU, never reports a fake total page count, and cannot cross into an adjacent chapter.
- When exact pagination replaces the preview, Reader remaps the visible position through the block/character anchor.
- Layout changes (font, padding, fullscreen geometry) derive the preview anchor from the currently visible prepared page rather than jumping back to the chapter start.
- Adjacent-chapter content prefetch starts after a short 180 ms grace period so the current page wins CPU/IO; candidate order remains next-first.
- After exact current pagination is published, Reader also prewarms the next chapter's `PreparedReaderPagination` for the same viewport/style after a 120 ms idle grace period.
- Returning Reader/PDF/Speed Reader to Library now trims embedded-image cache to 1/8 and parsed chapter memory to 1/4 while retaining persistent parsed disk cache for quick reopening.

## Validation

- Added pure ReaderPreviewPolicy tests and an incomplete-pagination cache guard.
- Existing alpha.14-alpha.37 regression guards remain enabled.
