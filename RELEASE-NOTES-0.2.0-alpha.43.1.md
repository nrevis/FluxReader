# Flux Reader 0.2.0-alpha.43.1

Small build-fix release for alpha.43.

The real Android build exposed two compile-time regressions that the static self-check could not previously catch: a missing coroutine `delay` import and an unsafe `LibraryActionButton` signature where the click callback was followed by a Boolean parameter. Both are fixed and guarded against recurrence.
