# Flux Reader 0.2.0-alpha.50.1

Hotfix for the alpha.50 Android/Compose compilation failure.

- Precompute the Reader note annotation theme color in composable scope.
- `ReaderNoteDialog.onSave` now receives and uses a plain ARGB value instead of calling `MaterialTheme.colorScheme` inside a non-composable callback.
- Add a self-check regression guard that rejects `MaterialTheme` access from this callback region.
- No intended behavior changes to Speed Reader taps, text selection, annotations, import, or library UX from alpha.50.
