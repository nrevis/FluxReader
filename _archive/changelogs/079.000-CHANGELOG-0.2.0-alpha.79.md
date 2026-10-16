# Flux Reader 0.2.0-alpha.79 — immersive Pixel cutout layout

- Separate physical centered camera cutout clearance from Android status-bar safeDrawing in immersive mode.
- Both experimental chrome shells receive explicit Reader/Speed immersive state and release the hidden OS system-bar area.
- The immersive Reader chapter progress is positioned within the camera lane instead of taking a second measured row.
- Speed Reader uses the same cutout policy, without retaining the old safeDrawing padding.
- Original library/settings layout and experimental opt-in switch are preserved.
- New static geometry audit and Pixel 9 Pro screenshot overlay below.
