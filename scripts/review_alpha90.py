#!/usr/bin/env python3
"""Regression critic for stable experimental fullscreen composition and speed checkpoint.

Source checks do not replace Android screenshots, JUnit or physical device testing.
"""
from pathlib import Path
root=Path(__file__).resolve().parent.parent
ui=root/'app/src/main/java/io/fluxreader/app/ui'
base=root/'app/src/main/java/io/fluxreader/app'
read=lambda p:p.read_text()
w=read(ui/'WastelandUi.kt'); c=read(ui/'ExperimentalChrome.kt')
a=read(base/'AppViewModel.kt'); lib=read(base/'data/LibraryRepository.kt')
policy=read(base/'SpeedResumePolicy.kt'); speed=read(ui/'SpeedReaderScreen.kt')
checks={
 'No fullscreen frame early return in Wasteland': 'if (immersive) {\n        Box(' not in w,
 'No fullscreen frame early return in Cyberpunk': 'if (immersive) {\n        Box(' not in c,
 'Stable device content slot Wasteland': w.count('content = content,') >= 1 and 'if (!immersive) {' in w,
 'Stable device content slot Cyberpunk': c.count('content = content,') >= 1 and 'if (!immersive) {' in c,
 'No side bezel in immersive Wasteland': '.padding(horizontal = if (immersive) 0.dp else 10.dp' in w,
 'No side bezel in immersive Cyberpunk': '.padding(horizontal = if (immersive) 0.dp else 8.dp' in c,
 'Explicit selected-word entry preserved': 'startSpeed(\n            bookId = bookId,' in a and 'preferredOffset = chapterOffset.coerceAtLeast(0)' in a,
 'Saved speed checkpoint preferred': 'SpeedResumePolicy.choose(book.speedChapter, book.speedOffset, preferredChapter)' in a,
 'Saving speed checkpoint on pause': 'libraryRepository.flushPendingPositions()' in a,
 'Checkpoint flush off UI thread': 'persistJob = ioScope.launch { saveLibrary(snapshot) }' in lib,
 'Checkpoint policy does not depend on Android': 'import android' not in policy,
 'Speech cursor committed before exit': 'if (speech.status.value.playing) speech.pause()' in speed and 'viewModel.speedPause()\n            speech.close()' in speed,
}
for title,ok in checks.items():print(('PASS' if ok else 'FAIL'),title)
if not all(checks.values()):raise SystemExit(1)
print(f'alpha.90 fullscreen + checkpoint critic: {len(checks)}/{len(checks)}')
