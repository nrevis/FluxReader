# Flux Reader alpha.80 — Kotlin compile fix

This release addresses the compiler error in alpha.79 (`ReaderScreen.kt:1177`). The fullscreen reader is hosted in a `Box`, where `Modifier.weight(1f)` is not available. The child now uses the same page-animation modifier as the normal reader, while the `ReaderContent` root measures itself with `fillMaxSize()`.

Static checks include the Pixel immersive policy and a new negative test against the exact failure. Full Android compilation must be completed on a machine with Android SDK. Install over the existing application to retain its data; do **not** uninstall or clear its storage.
