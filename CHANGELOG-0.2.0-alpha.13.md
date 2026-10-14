# Flux Reader 0.2.0-alpha.13

Smart Speed 3.0 and immersive Speed Reader.

- Added an immersive full-screen Speed Reader that hides Android system bars and the normal app chrome.
- Full-screen mode keeps only four primary controls: previous sentence, Play/Pause, next sentence, and exit full screen.
- The regular Speed Reader previous/next controls now navigate semantically by sentence instead of stepping one fixation at a time.
- Added sentence and paragraph navigation primitives to the incremental cursor without tokenizing the whole book.
- Added optional adaptive fixation length for PHRASE/FOCUS: simple short text can use a denser chunk, while long/technical tokens reduce the chunk size.
- Added optional pause context: when playback is paused, nearby words are shown subtly below the fixation line.
- Added smart resume rewind: after a configurable long pause, Play returns to the beginning of the current/previous sentence before continuing.
- Added text profiles for fiction, study, and technical reading; profiles tune semantic pauses and long-word slowdown without changing the user's chosen WPM.
- Preserved Smart Speed 2.0 micro-pauses, sentence-length bonus, gradual acceleration, ORP behavior, themes, and custom typography.
- Fixed PHRASE/FOCUS text color inheritance when Speed Reader follows the app theme.
