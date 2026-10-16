# Flux Reader 0.2.0-alpha.81 — Version consistency and installer fix

- Fix release script still naming alpha.79 APK even when Gradle compiled alpha.80.
- Use VERSION.txt as one source for build, install and packaging scripts, and run cross-file verification before builds.
- Preflight checks Gradle versionName/versionCode, assets/build-info.json and the installer APK path.
- No changes to reader logic, book storage, Wasteland/Cyberpunk or TTS.
- Release code: 20098.
