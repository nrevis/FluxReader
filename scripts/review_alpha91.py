#!/usr/bin/env python3
"""Speed Reader resume critic: verify no idle TTS callback rewinds the chapter.

This is a source/lifecycle regression gate, NOT an Android runtime or UI test.
"""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parent.parent
UI = ROOT / 'app/src/main/java/io/fluxreader/app/ui/SpeedReaderScreen.kt'
TTS = ROOT / 'app/src/main/java/io/fluxreader/app/reader/SpeedSpeechNarrator.kt'
POLICY = ROOT / 'app/src/main/java/io/fluxreader/app/reader/SpeedSpeechCheckpointPolicy.kt'
VM = ROOT / 'app/src/main/java/io/fluxreader/app/AppViewModel.kt'
LIB = ROOT / 'app/src/main/java/io/fluxreader/app/data/LibraryRepository.kt'


def judge(ui: str, tts: str, policy: str, vm: str, library: str):
    chapter_effect = re.search(
        r'LaunchedEffect\(state\.bookId,\s*state\.chapterIndex\)\s*\{([^}]+)\}', ui
    )
    checkpoint_callback = re.search(
        r'speech\.onSettledPosition\s*=\s*\{\s*at\s*->(.*?)\n\s*\}',
        ui, re.S
    )
    narrator_pause = re.search(r'fun pause\(\)\s*\{(.*?)\n    \}', tts, re.S)
    return {
        'Chapter change never publishes idle TTS offset 0': bool(chapter_effect) and 'speech.setPosition(' in chapter_effect.group(1) and 'speech.pause(' not in chapter_effect.group(1),
        'Narrator callback gated on explicit voice mode': bool(checkpoint_callback) and 'SpeedSpeechCheckpointPolicy.acceptNarratorOffset(voiceMode,' in checkpoint_callback.group(1),
        'Narrator checks that the TTS session was active': bool(narrator_pause) and 'commitOnPause(wasSpeaking, wasPending)' in narrator_pause.group(1),
        'Idle TTS pause cannot fire settled-position callback': 'if (SpeedSpeechCheckpointPolicy.commitOnPause(wasSpeaking, wasPending))' in tts,
        'Pure policy excludes idle voice': 'wasSpeaking || wasPending' in policy and 'voiceMode && textLength > 0' in policy,
        'StartSpeed preserves persisted offset': 'val startOffset = if (resumeFromReader' in vm and 'else savedOrChapterStart' in vm,
        'Speed checkpoint is saved on pause': 'libraryRepository.flushPendingPositions()' in vm,
        'Speed checkpoint retains its text offset': 'ReadingPositionPolicy.speedCheckpoint(book, chapter, offset)' in library,
        'No TTS reset at chapter entry': 'LaunchedEffect(state.bookId, state.chapterIndex) { speech.pause()' not in ui,
        'Fresh speed session clears old auto-rewind timer': bool(re.search(r'private fun startSpeed\(.*?speedPausedAtRealtimeMs = 0L.*?_route.value = AppRoute.SPEED', vm, re.S)),
    }


def main():
    ui, tts, policy, vm, lib = [p.read_text() for p in (UI, TTS, POLICY, VM, LIB)]
    results = judge(ui, tts, policy, vm, lib)
    for label, ok in results.items():
        print(f"{'PASS' if ok else 'FAIL'} {label}")
    if not all(results.values()):
        sys.exit(1)
    if '--self-test' in sys.argv:
        # Re-introduce the two concrete alpha.90 defects in memory and check
        # that the critic rejects both, without changing the user's source.
        unsafe_ui = ui.replace(
            'speech.setPosition(state.chunk?.startOffset ?: 0)\n        voiceMode = false',
            'speech.pause()\n        voiceMode = false', 1
        )
        assert not judge(unsafe_ui, tts, policy, vm, lib)['Chapter change never publishes idle TTS offset 0']
        unsafe_tts = tts.replace('if (SpeedSpeechCheckpointPolicy.commitOnPause(wasSpeaking, wasPending)) {', 'if (true) {', 1)
        assert not judge(ui, unsafe_tts, policy, vm, lib)['Idle TTS pause cannot fire settled-position callback']
        print('PASS negative regressions: old chapter reset and unconditional TTS callback detected')
    print(f'alpha.91 Speed Reader resume critic: {len(results)}/{len(results)}')


if __name__ == '__main__':
    main()
