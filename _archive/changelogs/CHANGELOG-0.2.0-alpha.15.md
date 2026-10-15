# Flux Reader 0.2.0-alpha.15

Fullscreen reader / cutout / line-safe pagination fix pack.

- Added a dedicated fullscreen mode to the normal Reader with transient Android system bars.
- Reader fullscreen and auto-hidden chrome now lay out book content inside `WindowInsets.safeDrawing` instead of drawing text into the camera/status/navigation unsafe area.
- Added a 36 dp camera-safe minimum top band for immersive layouts where Android reports a zero top inset after bars are hidden.
- Reused the cutout/header band: subtle reading progress sits on the left and chapter position on the right, leaving the center clear for a hole-punch camera.
- Added minimal fullscreen Reader controls: back, contents, Speed Reader, reader settings and exit-fullscreen.
- Side controls expose previous/next page while the fullscreen controls are visible; the existing edge-tap/swipe controller remains the canonical page navigation path.
- Wide displays cap the readable column at 760 dp while keeping the side gutters active as navigation gesture areas. Phone portrait width/density is unchanged.
- Fixed clipped first lines after page turns: page anchors inside long text blocks are snapped to the current rendered line-height step instead of an arbitrary pixel in the middle of a glyph line.
- Previously saved pixel offsets are also line-snapped when reopening in Screen Paging mode, so old broken anchors self-heal without losing text.
- Page history is cleared when fullscreen/chrome geometry changes so anchors from the previous viewport are never reused against a different page height.
- Speed Reader fullscreen now also respects safe drawing insets for progress, text viewport and controls.
- Speed Reader no longer toggles `setDecorFitsSystemWindows(true)` on exit, preserving the app-wide edge-to-edge contract established by `enableEdgeToEdge()`.
- Added ReaderPageMath regression tests for line-safe next/previous anchors and restored-offset snapping.
