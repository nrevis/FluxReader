# Flux Reader 0.2.0-alpha.24 — release notes

This release is a larger pass over both Speed Reader and ordinary book typography.

Speed Reader is now organized much closer to Reedy's interaction model: the normal screen keeps WPM and reading profiles visible while exact RSVP geometry, timings and colors live under `Расширенные`. Display and behavior controls each have their own sheet, so controls no longer compete for the same space. Tap-to-pause opens a real text context that can be used for seeking, hardware volume keys work during speed reading, the speed range reaches 3000 WPM, and progress includes an ETA for the current chapter.

EPUB/FB2 images now survive the speed-reading pipeline. When the cursor reaches an image, playback pauses, the image is shown for the configured duration, and reading resumes through gradual acceleration. Chapter titles are presented as a short dedicated intro and are not replayed immediately afterward as ordinary RSVP words.

The normal reader now preserves chapter structure more aggressively. FB2 chapter labels and chapter titles get separate semantic roles, EPUB h1/title elements are promoted to title styling, headings are centered with independent metrics, and images/text share symmetric side margins. The FB2 cache version is bumped so previously parsed books automatically receive the new formatting after reopening.

A new JetBrains reading profile configures the imported JetBrains Nerd font with equal page margins, compact spacing and automatic hyphenation while keeping left alignment to avoid the exaggerated word gaps that justified monospace text can create.
