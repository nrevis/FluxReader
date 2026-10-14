# Flux Reader 0.2.0-alpha.89 — Experimental design policy test fix

- Fixes `ExperimentalDesignPolicyTest` failure in alpha.88 by updating assertions to the alpha.87+ independent frame/palette policy.
- Keeps the experiment strictly opt-in, including with an explicit frame and custom experiment palette.
- Covers ordinary palette + enabled experimental shell, independent frame selection, preserving palette after disabling, and no automatic opt-in from palette selection.
- No changes to UI rendering, app data or runtime policy; no migration needed.
- Important: Gradle/JUnit tests and APK build must be run on a Mac with the Android SDK.
