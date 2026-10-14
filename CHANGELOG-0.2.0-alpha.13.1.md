# Flux Reader 0.2.0-alpha.13.1

Compile hotfix for alpha.13 after verification on the user's macOS Android SDK 37 toolchain.

- Fixed Android compile failure in `SmartBookImport.kt`: `javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD` and `ACCESS_EXTERNAL_SCHEMA` are not exposed by the Android compile API. The parser now uses the standard JAXP property URI strings inside the existing `runCatching` guards.
- Fixed Compose compile failures in `LibraryScreen.kt` and `ReaderPreferences.kt`: removed explicit `androidx.compose.foundation.layout.weight` imports so `Modifier.weight()` resolves from the active `RowScope` / `ColumnScope`.
- Added self-check regression guards for both failure classes.
- Smart Speed 3.0, immersive fullscreen controls, sentence/paragraph navigation, adaptive fixations, pause context and alpha.13 behavior are unchanged.
