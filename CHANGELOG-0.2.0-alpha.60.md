# Flux Reader 0.2.0-alpha.60

## Wasteland startup layout recovery

- Fixed the Wasteland app shell collapsing routed `Scaffold` screens to bar-height on startup.
- `WastelandDeviceFrame` now propagates full minimum constraints to the routed screen.
- Interactive CRT content now respects `WindowInsets.safeDrawing`, preventing the Android status and navigation bars from covering Wasteland controls on edge-to-edge devices such as Pixel 9 Pro.
- Library, Reader, PDF Reader, Speed Reader and Settings now explicitly use full-screen `Scaffold` modifiers.
- Restored the Library viewport so previously imported books are visible and interactive immediately after launch.
- Restored the Wasteland hardware rail to the actual bottom of the usable screen.
- Reduced legacy FAB clearance in Wasteland grid/compact/list modes because Wasteland uses a real Scaffold bottom bar instead of a floating action button.
- Existing library data/import storage is untouched by this change.
