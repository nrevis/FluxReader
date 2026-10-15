# Flux Reader 0.2.0-alpha.16.2

Fullscreen reader geometry + unified import UX.

- Rebuilt fullscreen Reader layout as two physical regions instead of an overlay: a dedicated camera/cutout HUD and a separate text viewport below it.
- Fullscreen page height is now measured from the actual text viewport, so page navigation no longer counts the camera/status HUD as readable space.
- Added a permanent 10 dp top guard inside the fullscreen text viewport so a glyph row can never be clipped exactly at viewport y=0.
- Entering fullscreen snaps the current long-text offset back to a measured full line even in free-scroll mode.
- When free-scroll stops in fullscreen, the top edge settles to the previous measured line, preventing a half-visible first row at rest.
- Fullscreen progress is placed at the left edge of the camera band and chapter/page information at the right edge; the center remains empty for a hole-punch/notch.
- Library now has one Add FAB. Tapping it opens a dialog with “Выбрать файл” and “Выбрать папку”.
- Existing recursive folder import, folder hierarchy, EPUB structure fixes, covers, Reader typography and Smart Speed behavior are preserved.
- Version bumped to 0.2.0-alpha.16.2 / 20024.
