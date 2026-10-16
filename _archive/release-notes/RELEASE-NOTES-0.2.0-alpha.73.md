# Flux Reader 0.2.0-alpha.74 — Reader / Library integration

- Reader fullscreen is now explicitly opt-in (off by default for both existing installs and new ones). Auto-hiding chrome is disabled when fullscreen is not allowed.
- Speed Reader gets a narration button that returns to the same point in the standard Reader and opens its TTS controls. No third-party speech API or bundled local model is claimed.
- Local audiobooks are listed next to ebooks in the normal library with a colored music icon and format badge (MP3/M4B/WAV/etc). Import through Add menu; no dedicated audiobook icon in the library top bar.
- Audio entries preserve virtual folder membership. Moving, renaming, or deleting a containing folder rebases audio membership; the original SAF document remains untouched.
- GRID and COMPACT folder views now show distinct folder tiles, LIST keeps rows; no full-row grid spans for folders.
- All legacy library books, reading positions, and installed TTS engines are preserved.

Static checks do not substitute for a complete Android/Gradle build on macOS. No Android APK is included.
