# Flux Reader 0.2.0-alpha.18

Managed Library / file & folder operations.

- Added persistent virtual folders independent from book records, so empty folders survive restarts.
- Added book actions: edit metadata/title, copy placement to another folder, move, physical duplicate, SAF export, and confirmed deletion.
- Copy-to-folder reuses the same managed book file; Duplicate creates a separate internal copy and resets its reading progress.
- Added folder actions: create subfolder, rename, copy subtree, move subtree, and safe delete.
- Safe folder delete preserves books and nested folders by lifting the subtree to the parent.
- Folder rename/move remaps all descendant book paths in one repository operation.
- Added case-insensitive folder-name collision guards and unique copy naming.
- Recursive folder import now persists empty directories seen by the SAF tree scanner.
- Added `library-folders.json` with backward derivation from existing `folderPaths`.
- Added SAF `CreateDocument` export without broad storage permission.
- Library UI now exposes `⋮` action menus in Grid, Compact, List, Grouped, and Folder modes.
- Added folder-tree tests for explicit empty folders, prefix remapping, and unique sibling names.
