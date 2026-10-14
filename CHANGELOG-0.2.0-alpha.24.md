# Flux Reader 0.2.0-alpha.24

## Speed Reader — rebuild around the Reedy interaction model

- Main Speed Reader chrome is reduced to playback, WPM and three pacing profiles (`Ровно`, `Умно`, `Глубоко`).
- Detailed controls are split into independent `Отображение`, `Поведение` and `Расширенные` sheets.
- RSVP range is expanded to 100–3000 WPM.
- ORP remains pinned to the actual measured glyph centre; optional following words stay as dim peripheral context.
- Tap on the reading viewport toggles Play/Pause. On pause a readable text window appears; tapping a word seeks the RSVP cursor to that exact word.
- Hardware volume keys are supported while Speed Reader is active: short press toggles Play/Pause, hold navigates by sentence.
- Progress now includes chapter/book percentage and an approximate time remaining for the current chapter.
- Display remains awake for the entire Speed Reader session.

## Images and chapter semantics

- EPUB/FB2 speed documents now preserve image positions and heading roles instead of flattening everything to plain text.
- Encountered images pause RSVP, show the image for a configurable 0.8–8.0 seconds, then resume through the normal gradual acceleration path.
- Leading and trailing images are handled too, including images before the first word or after the last word of a chapter.
- New chapters receive a dedicated chapter/title intro for a configurable duration.
- Consecutive leading chapter headings are presented by the intro and skipped by the word stream so they are not spoken/displayed a second time token by token.

## Reader typography / Google-Books-style cleanup

- EPUB `h1` / `title` blocks are promoted to `TITLE`; lower headings remain `HEADING`.
- FB2 title blocks distinguish `Глава N` from the actual chapter title, allowing a smaller chapter label plus a larger centered title.
- FB2 parser cache version is bumped to 4 so existing cached chapters are rebuilt with the new semantic roles.
- `TITLE` and `HEADING` receive dedicated centered typography and line-breaking rules instead of inheriting body metrics.
- Reader text and images use the same symmetric horizontal page padding.
- Existing whole-line pagination remains conservative: if the final line cannot be rendered completely, it stays on the next page.

## JetBrains reading profile

- Added a `JetBrains` profile: JetBrains Nerd font mode, symmetric margins, compact book-oriented spacing, left alignment and automatic word hyphenation.
- The font file is still user-imported through Android SAF; no font binary is bundled.

## Validation

- Static self-check updated for alpha.24 and passes.
- Pure Kotlin smoke checks pass for 3000 WPM, tap/word offsets, semantic heading skipping and image cues.
- Android/Compose Gradle compilation cannot run in the isolated build container because Gradle 9.6.0 is not cached and external DNS is unavailable; the macOS self-check/build scripts remain the authoritative full compile step.
