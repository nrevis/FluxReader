# Flux Reader 0.2.0-alpha.15 — release notes

This build targets the fullscreen/page-turn clipping visible on the supplied Pixel screenshots. The Reader now treats the display cutout and system gesture areas as part of the viewport contract instead of allowing text to render underneath them.

The second cause of the screenshots was pagination itself: a page turn could scroll a long paragraph to an arbitrary pixel offset, which could place the top of the next page halfway through a rendered line. Screen Paging now snaps these anchors to the measured line-height step and also repairs older persisted offsets on reopen.

Normal Reader now has an explicit fullscreen action. In fullscreen the center of the camera band stays clear, while subtle progress/chapter information occupies the left/right space. A tap reveals a small control cluster and optional side previous/next controls. On wide/landscape screens the reading column is capped while the outer gutters remain usable for navigation. Phone portrait density is intentionally unchanged.

Speed Reader fullscreen received the same safe-area protection for its progress indicator, viewport and controls.
