# Flux Reader 0.2.0-alpha.48

- Synchronize normal Reader position from the exact Speed Reader stop offset when returning from Speed Reader.
- Add inverse `ChapterSpeedOffset.toReaderAnchor()` mapping with tests.
- Theme Grid/Compact/List/Continue library cards with current `surfaceVariant`.
- Replace book-card overflow (`⋮`) buttons with direct Speed Reader play buttons.
- Keep long press as the single entry point to book file actions.
- Replace oversized book/folder action dialogs with compact popups; remove book title and secondary descriptions from book actions.
