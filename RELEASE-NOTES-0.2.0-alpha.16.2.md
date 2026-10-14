# Flux Reader 0.2.0-alpha.16.2

This build addresses the persistent clipped first line seen in fullscreen screenshots. The camera/status area is no longer drawn over the reader: it owns a dedicated HUD band, while the book text gets a physically separate viewport below it. The Reader also snaps to real measured line tops when entering fullscreen and after a fullscreen free-scroll gesture settles.

The fullscreen HUD leaves the center empty for the camera cutout and places reading progress on the left and chapter position on the right.

Library import is simplified to one Add button. It opens a choice between files and a folder; folder selection keeps the recursive subfolder-aware import introduced in alpha.16.
