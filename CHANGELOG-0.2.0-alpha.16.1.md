# Flux Reader 0.2.0-alpha.16.1

Compile hotfix over alpha.16.

- Fixed the two grouped-library calls to `CompactBookCard` and `BookGridCard` after alpha.16 added the optional `showFolderPath` parameter.
- `onEdit` is now passed explicitly as a named callback so Kotlin cannot bind a trailing lambda to the following Boolean parameter.
- Added a static self-check guard for this exact callback/signature regression.
- Preserved alpha.16 folder hierarchy, EPUB block/list parsing and reader typography changes unchanged.
- Version bumped to 0.2.0-alpha.16.1 / 20023.
