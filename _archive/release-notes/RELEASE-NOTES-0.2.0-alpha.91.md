# Flux Reader 0.2.0-alpha.91 — Speed Reader checkpoint regression fix

## Root cause

`SpeedReaderScreen` previously called `speech.pause()` every time `state.bookId` or
`state.chapterIndex` changed. That call emitted `onSettledPosition(0)` from a newly
created, idle TTS engine. The callback unconditionally called
`viewModel.speedSeekToOffset(0)` even when narration was disabled. This could
immediately overwrite a saved mid-chapter position with the first word.

## Fix

- Book/chapter changes reset the speech cursor silently with `setPosition`.
- Only explicitly enabled speech mode is permitted to save a narrator offset.
- Pausing an idle TTS engine does not emit a checkpoint.
- A newly opened Speed Reader session clears the previous session's pause timer to avoid automatic sentence rewind on first Play.
- Existing saved library data and the optional within-session rewind behavior are preserved.
- JUnit regression tests and `scripts/review_alpha91.py --self-test` added.

## Validation

The pure Kotlin policy and static critic can be checked without Android SDK.
A full Gradle build and a physical Pixel 9 Pro open/close/pause verification must
still be run on the user's Mac. Do not confuse static checks with device testing.
