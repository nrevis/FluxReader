#!/usr/bin/env python3
from pathlib import Path
p=Path(__file__).resolve().parent.parent / 'app/src/main/java/io/fluxreader/app'
app=(p/'ui/FluxReaderApp.kt').read_text()
speed=(p/'ui/SpeedReaderScreen.kt').read_text()
reader=(p/'ui/ReaderScreen.kt').read_text()
speech=(p/'reader/SpeedSpeechNarrator.kt').read_text()
assert 'ReadingWorkspaceModeBar(' not in app, 'Three-tab bar must be removed'
assert 'AppRoute.SPEED -> SpeedReaderScreen(' in app
assert 'AppRoute.READER -> ReaderScreen(' in app
assert 'onNarration = toggleVoice' in speed
assert 'onTogglePlay = togglePlayback' in speed
assert 'SpeedReaderCursor.next(state.chapterText, speechStatus.offset, 1)' in speed
assert 'onRangeStart(' in speech
assert 'fun setPosition(offset: Int)' in speech
assert 'FullscreenSystemBarsEffect(!chromeVisible)' in reader
assert 'showFullscreen = true' in speed
assert 'onToggleFullscreen = { isFullscreen = !isFullscreen }' in speed
assert 'FullScreenSystemBarsEffect(settings.readerFullscreenEnabled' not in reader
assert 'if (settings.readerFullscreenEnabled) IconButton(' not in reader
assert 'if (!voiceMode)' in speed and 'SpeedControls(settings, viewModel, textColor)' in speed
print('alpha.76 Read / Speed+Speech / manual fullscreen regression guards: OK')
