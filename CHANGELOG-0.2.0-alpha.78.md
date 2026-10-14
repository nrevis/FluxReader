# Flux Reader 0.2.0-alpha.78 — Kotlin build repair

- Fixed missing `androidx.compose.ui.text.style.TextAlign` in `LibraryScreen.kt` (folder cards, around line 1680).
- Added a conservative Compose symbol import scanner and negative tests to the static self-check.
- All library, reading, Wasteland/Cyberpunk and narration features are retained from alpha.77 without changes.
- Android/Gradle compilation is **not** verified in this environment; run the commands in the release instructions on macOS.
