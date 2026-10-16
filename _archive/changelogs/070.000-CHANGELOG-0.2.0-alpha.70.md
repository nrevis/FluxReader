# Flux Reader 0.2.0-alpha.70 — Narration foundation

- Integrated native Android TextToSpeech narration into EPUB, FB2 and TXT Reader.
- Voice catalog (quality/network indication), automatic offline voice preference per detected chapter language (Russian/English), manual voice selection and voice preview.
- Play/pause/stop, sentence skip, pause/resume from reported UTF-16 range when supported by engine, sentence / paragraph micro-pauses.
- Speed/pitch persistence via ReaderSettingsRepository, opt-in auto-advance to next chapter, saved speech block position.
- No third-party API keys, no direct remote content upload from Flux Reader. Network voices are selectable explicitly; they may transmit text according to the external TTS engine's policy.
- Keeps experimental appearance gating and the existing Library, Reader, PDF and Speed Reader behavior unchanged outside the new narration controls.
- AndroidManifest.xml declares TTS_SERVICE package visibility for supported system engines.
- Unit tests for bounded sentence segments, stable offset mapping, image-only chapters.

## Limitations of this alpha

- Voice naturalness is determined by the Android TTS engine and installed voice data, not by this code. High-quality installed neural voices can be selected, but none are bundled.
- No independent cloud neural provider, offline neural model downloads, MediaSession/foreground audio service or read-along word highlighting yet.
- PDF narration (OCR/PDF text) is not part of this first release.
- This project ZIP has undergone static source checks and isolated Kotlin compilation for narration logic only; full Android Gradle build and on-device testing must be run on the user's Mac/Pixel 9 Pro.
