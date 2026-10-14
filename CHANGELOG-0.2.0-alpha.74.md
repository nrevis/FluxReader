# Flux Reader 0.2.0-alpha.74 — Unified Reading Workspace / Offline Voice Cache

- Persistent three-tab Reader workspace: Reading / Speed Reader / Listen, with shared header and Back button.
- Voice playback has an in-page compact controller; full TTS preferences remain in a sheet.
- Switching from Speed Reader restores the chapter/text anchor. No separate library visit is needed.
- Optional offline-only local WAV cache: separate Android TTS synthesizer pre-renders up to two forthcoming phrases; voice, language, rate, pitch, and exact spoken text are included in the SHA-256 cache key.
- Cache eviction at 64 MiB; prefetch queue capped at six; playback reuses locally synthesized WAV audio using MediaPlayer.
- Simple Russian speech normalization for a few abbreviations and the number sign; source text and reading positions are unchanged.
- Compatible with **installed sherpa-onnx Android TTS engines**. No sherpa JNI `.so` or Russian model are bundled in this ZIP: an installed offline voice remains required. This release does **not** claim built-in model inference, lock-screen media controls or background foreground-service playback.
- Does not require any cloud account, API key or INTERNET permission.

## Safety notes
Do not delete app data before upgrading: library data, reading positions, and settings remain on-device. For controlled tests use `./gradlew :app:compileDebugKotlin :app:compileReleaseKotlin` before installing. Builds cannot be validated in this environment without Android SDK.
