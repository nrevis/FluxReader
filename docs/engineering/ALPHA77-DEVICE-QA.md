# alpha.77: Release gate — independent critics

The source checker (`scripts/review_alpha77.py`) is deliberately separate from existing selfcheck guards. It catches known regressions but is not a substitute for actual Android rendering/gesture tests.

## Critic A — UX and interaction (device required)
- Standard and Wasteland/Cyberpunk Reader: open EPUB, tap manual fullscreen with auto-hide preference OFF; chrome disappears, content reflows, tapping center or Back returns without jumping position.
- Screen paging and continuous scroll: repeat while keeping bookmark/text selection and page progress stable.
- Speed Reader: toggle full screen from portrait normal mode, toggle voice, play/pause, switch back; controls remain responsive and never overlap context.
- Test with installed Android TTS with and without `onRangeStart`, including unsupported voices.

## Critic B — geometry/legibility (screenshots required)
- Wasteland: main RSVP word fully visible, no half next word, no clipped word at focus position.
- Fullscreen: context preview is legible but does not overlap 52dp bottom control rail; top/bottom nav insets remain safe around status bar and gesture handle.
- Folder Grid vs Compact: same card height per row, title green/cream contrast, names 1 and 2 lines, counts visible.
- Test at system font scales 1.0, 1.3, 1.6; rotate and check screen cutout.

## Critic C — build/data regression
- `python3 scripts/review_alpha77.py` and `python3 scripts/check_kotlin_imports.py` pass.
- `./gradlew :app:compileDebugKotlin :app:compileReleaseKotlin :app:testDebugUnitTest` passes on an Android SDK host.
- Existing books, folders, highlights and speed position persist after upgrading; experimental design only when opt-in is checked.

Block release if any critical item fails; attach screenshot and logcat to the next fix.
