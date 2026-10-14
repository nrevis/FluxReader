# Flux Reader 0.2.0-alpha.43.2

Small test-compiler hotfix for alpha.43.1.

The Android application sources now compile successfully in the user-side SDK 37 build; the remaining failure was `compileDebugUnitTestKotlin`, where Kotlin treats deprecated `createTempDir()` calls as errors. The parsed-cache tests now use the modern `kotlin.io.path.createTempDirectory` API and self-check prevents the deprecated helper from returning.
