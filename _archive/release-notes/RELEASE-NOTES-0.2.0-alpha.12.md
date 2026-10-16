# Flux Reader 0.2.0-alpha.12 — release notes

This build concentrates on reading rhythm rather than just raw WPM. Reedy publicly describes smart slowing around punctuation and difficult words plus gradual acceleration; other RSVP readers expose punctuation/paragraph pause tuning, and published work on RSVP/Spritz discusses extra display time for longer words and longer sentence-end pauses. Flux Reader now applies those ideas with its own configurable timing model instead of copying opaque timings from another app.

The default Balanced rhythm adds a short 170 ms sentence-end pause, 65 ms clause pause, a 260 ms paragraph pause, bounded extra time for long sentences, and a five-second smooth acceleration from 72% to the selected WPM. Gentle and Comprehension presets are included, and every value can be tuned independently.

Constant pacing is still available for users who want exact cadence without semantic delays.
