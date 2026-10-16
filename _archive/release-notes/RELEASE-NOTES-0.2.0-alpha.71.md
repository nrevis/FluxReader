# Flux Reader 0.2.0-alpha.71 — Reader viewport and audiobooks

- Wasteland/Cyberpunk immersive Reader no longer applies the same safe-drawing padding twice.
- Progress HUD is compact inside an already-safe device frame; the normal theme retains its cutout guard.
- Separate audiobooks library with SAF multi-document import for MP3/M4A/M4B/AAC/OGG/FLAC/WAV/OPUS/3GP.
- Android MediaPlayer playback, seek bar and ±15 seconds, resume position persisted via SharedPreferences.
- File access uses persistable SAF permissions; large audio files are NOT copied into the app.
- Audiobooks accessible from Library top bar, Add menu and separate page; no change to existing EPUB/FB2/PDF models.

Limitations: decoder support varies with Android/installed codecs; continuous background playback/notification controls and embedded M4B chapters are NOT implemented in this version. Run the full Gradle build on a Mac with Android SDK before installing.
