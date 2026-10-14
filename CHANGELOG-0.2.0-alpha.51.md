# Flux Reader 0.2.0-alpha.51

- Reader text selection can span multiple text blocks/paragraphs while keeping exact block + character endpoints.
- Added draggable start/end selection handles for precise endpoint adjustment on visible text.
- Persistent highlights, quotes, notes and bookmarks now render across every covered paragraph.
- Reader annotations repository accepts multi-block ranges without breaking old single-block JSON entries.
- Added annotation filters: all, highlights, quotes, notes and bookmarks.
- Existing highlights can be recolored directly from the annotations sheet.
- Opening an annotation seeds an exact viewport anchor before navigating back into the Reader.
- Added pure cross-paragraph range tests and alpha.51 regression guards.
