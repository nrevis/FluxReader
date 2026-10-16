# Flux Reader 0.2.0-alpha.43.2

Hotfix for the unit-test Kotlin compilation failure exposed by the real Android SDK 37 self-check of alpha.43.1.

- Replaced deprecated Kotlin `createTempDir()` calls in `ParsedBookDiskCacheTest` with `kotlin.io.path.createTempDirectory(...).toFile()`.
- Added the `kotlin.io.path.createTempDirectory` import.
- Added a static regression guard that fails packaging if deprecated `createTempDir()` returns anywhere under `app/src/test`.
- Kept Java `File.createTempFile(...)` uses unchanged because those are not the deprecated Kotlin helper reported by the compiler.
- Version bumped to `0.2.0-alpha.43.2` / `20058`.
