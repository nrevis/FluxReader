# Flux Reader 0.2.0-alpha.63

## Wasteland Library layout / interaction repair

- Replaced the Wasteland Library Material `Scaffold` path with an explicit full-height `Column` layout.
- The Wasteland header, book viewport, and hardware navigation rail now own separate measured regions and cannot overlap each other.
- The book viewport is the only weighted region, so grids/lists keep a real visible height on edge-to-edge devices such as Pixel 9 Pro.
- `LIBRARY`, `SEARCH`, `ADD`, and `SETTINGS` are now bound directly to the visible hardware buttons rather than relying on a separately measured Scaffold bottom bar.
- `LIBRARY` resets search/filter state; `SEARCH` focuses the real search field and requests the keyboard; `ADD` opens the import chooser; `SETTINGS` navigates to settings.
- Decorative Wasteland texture / CRT layers remain pointer-transparent and outside the interactive layout.
- Existing books, folders, metadata, and progress storage are unchanged.

Version: `0.2.0-alpha.63` / `versionCode 20080`.
