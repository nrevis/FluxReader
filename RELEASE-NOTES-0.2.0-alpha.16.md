# Flux Reader 0.2.0-alpha.16

Alpha.16 focuses on two reader/library workflows.

You can now choose an entire folder through Android's system picker. FluxReader recursively imports supported books and keeps the selected folder/subfolder structure as a navigable hierarchy. Duplicate book content is stored once but can appear in more than one folder path.

The normal reader now uses measured text-line geometry for page anchors and a more book-like default typography path. EPUB XHTML structure is also preserved more faithfully: annotation text inside structural containers is no longer lost, and nested HTML lists are rendered as separate indented list items with independent clickable links instead of one concatenated underlined paragraph.

No denser library-card layout was introduced in this build.
