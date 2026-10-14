# Flux Reader 0.2.0-alpha.50.1

Compile hotfix for alpha.50.

The new note action in Reader attempted to read `MaterialTheme.colorScheme` from the ordinary `onSave` callback of `ReaderNoteDialog`. Compose rejects that at Kotlin compilation time. The color is now resolved in the surrounding composable and passed into the callback as a plain integer.

All alpha.50 functionality remains unchanged.
