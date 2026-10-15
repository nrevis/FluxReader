# Flux Reader 0.2.0-alpha.43.1

Hotfix for Kotlin compilation failures found by a real Android SDK 37 build of alpha.43.

- Added the missing `kotlinx.coroutines.delay` import used by reader prefetch scheduling.
- Made `LibraryActionButton` trailing-lambda-safe by keeping the function callback as the final parameter.
- Updated existing action calls that pass callbacks positionally so they remain unambiguous.
- Added static regression guards for coroutine `delay` imports and `LibraryActionButton` parameter ordering.
- Version bumped to `0.2.0-alpha.43.1` / `20057`.
