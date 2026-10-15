# Flux Reader 0.2.0-alpha.37

- Reworked screen-pagination probing for very large text blocks: page measurement now starts with a 4–12K adaptive window and expands only when the whole probe actually fits, retaining the previous 32K safety ceiling.
- Re-measures with a fresh adaptive window whenever a partially filled page is flushed, so the optimization cannot under-fill the following full page.
- Memoized prepared text slices and resolved text styles to reduce repeat substring/style allocation during unrelated Compose recompositions such as chrome/fullscreen changes.
- Embedded EPUB/FB2 image loads now include content hash + source length + source mtime in the memory-cache key, preventing stale images after an in-place book update.
- Added striped image-request deduplication plus a memory-class-aware decode semaphore (1/2/3 concurrent decodes) to avoid duplicate ZIP reads and decode bursts in illustrated books.
- Reader image producers explicitly clear their previous value when the image key changes, preventing a stale image flash while the next bitmap is loading.
- PDF render requests now use a generation token so a late result from an obsolete page/zoom request can never replace the current bitmap; a new zoom request can supersede an in-flight render instead of being dropped while `loading=true`.
- PDF native renders are serialized with a mutex. Cancellation is checked immediately after the blocking native render; stale bitmaps are recycled before ownership can be lost.
- Added memory-class-aware PDF bitmap pixel budgets: 3 MP on low-memory devices, 5 MP on mid-range devices, up to the existing 8 MP ceiling on larger-memory devices.
- ViewModel teardown now explicitly recycles the currently owned PDF bitmap and embedded-image cache bitmaps.
- Added pure Kotlin regression tests/guards for adaptive pagination windows and the new memory policies.
