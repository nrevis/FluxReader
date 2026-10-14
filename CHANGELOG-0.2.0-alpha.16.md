# Flux Reader 0.2.0-alpha.16

Library folder hierarchy + reader typography / EPUB structure pass.

- Added recursive SAF folder import via Android's system folder picker for EPUB, FB2/FB2.ZIP, TXT and PDF.
- Preserved selected-folder/subfolder hierarchy inside the FluxReader library with breadcrumb navigation.
- A SHA-256 duplicate can belong to multiple imported folder paths without duplicating the physical book file.
- Added `LibraryGroupMode.FOLDER`; search includes folder paths and metadata editing shows the imported paths.
- Added pure `LibraryFolderTree` path normalization / hierarchy tests.
- Reader page anchors now use measured Compose line tops after layout, including reflow/fullscreen re-anchoring.
- Enabled paragraph line breaking and automatic hyphenation for prose; Book preset uses left alignment instead of forced justification.
- Fixed EPUB annotation/XHTML flattening: direct container text is preserved instead of being silently dropped.
- Added structural EPUB list parsing: `ul`/`ol`/nested `li` become separate reader blocks with list depth and markers.
- Nested EPUB links are no longer concatenated into a single underlined text run; each list item keeps its own link range and target.
- Block quotes now preserve nested paragraph structure instead of flattening all descendant paragraphs into one block.
- Added regression coverage modeled on an Annotation page with nested linked story titles.
- Version: 0.2.0-alpha.16 / versionCode 20022.
