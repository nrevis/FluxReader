# Flux Reader 0.2.0-alpha.13.2

Compile hotfix over alpha.13.1.

- Fixed an invalid multiline regular Kotlin string in `SpeedReaderCursorTest.paragraphNavigationAndPauseContextStayLocal`.
- The paragraph-navigation test fixture now uses explicit `\n\n` separators and compiles under Kotlin/Android tooling.
- Added a static test-source guard that rejects unmatched regular Kotlin string quotes across physical lines while preserving valid triple-quoted raw strings.
- Retains alpha.13.1 Android XML-property and Compose `weight` compile fixes.
- No runtime Smart Speed 3.0, fullscreen, navigation or pacing behavior changed in this hotfix.
