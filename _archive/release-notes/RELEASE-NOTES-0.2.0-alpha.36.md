# Flux Reader 0.2.0-alpha.36

Performance/stability pass for reopening EPUB/FB2 books. Parsed chapter models are now persisted in a bounded, versioned cache and reused after application restarts while the source book remains unchanged. Reader/prefetch requests for the same chapter are deduplicated, and EPUB XML is streamed directly into the parser to reduce peak memory on large chapters.
