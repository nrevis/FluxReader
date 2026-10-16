# Flux Reader 0.2.0-alpha.6

Reader pagination rewrite on top of the 0.2 clean line.

- Buttons, left/right edge taps, and horizontal swipes now use one page controller.
- In Pages mode, vertical free scrolling is disabled and navigation uses discrete measured page anchors.
- Long text blocks paginate inside the same block using pixel offsets instead of jumping to another chapter.
- A bounded history of 256 page anchors makes backward navigation symmetric.
- Crossing a chapter boundary stores the exact previous-page anchor so Back returns to the same place.
- Scroll mode keeps free vertical scrolling; page controls still move by one page.
- Rapid repeated gestures cancel the in-flight transition before starting a new one.
- Layout-affecting settings invalidate page history automatically.
- ReaderPageMath is pure Kotlin and covered by unit tests.
