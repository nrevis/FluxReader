# Flux Reader 0.2.0-alpha.13.2 — release notes

This is a build-only hotfix for alpha.13.1. On the real Android SDK 37 build, the main application Kotlin sources compiled successfully; Gradle then stopped in `compileDebugUnitTestKotlin` because one test used a regular quoted string across physical lines. The fixture now uses explicit newline escapes.

The static self-check also scans test Kotlin strings for this regression before Gradle. Smart Speed 3.0 and fullscreen reading behavior are unchanged.
