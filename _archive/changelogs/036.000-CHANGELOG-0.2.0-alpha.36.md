# Flux Reader 0.2.0-alpha.36

- Added a persistent parsed-book cache for EPUB and FB2 `BookOutline` / `ChapterContent` models.
- Cache entries are invalidated by a source token containing cache schema version, library content hash, source length and source mtime.
- Added a compact streaming binary codec that preserves text roles, anchors, links, list metadata and image blocks.
- Cache writes are atomic and best-effort; cache corruption or disk-write failure falls back to the original parser instead of breaking Reader.
- Added a 160 MiB disk budget with least-recently-used book-directory pruning; the active book is excluded from pruning.
- Added fixed-size striped parse mutexes so Reader and background prefetch cannot parse the same chapter concurrently.
- Memory cache keys now include the source token, preventing stale in-memory chapters if a book file changes during the process lifetime.
- EPUB DOM parsing now consumes ZIP entry streams directly instead of first allocating an extra decompressed `ByteArray` for XHTML/OPF/nav XML.
- Added pure Kotlin persistent-cache regression tests and static guards for streaming EPUB parsing / bounded cache wiring.
