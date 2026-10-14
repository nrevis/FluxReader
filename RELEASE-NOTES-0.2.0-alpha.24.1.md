# Flux Reader 0.2.0-alpha.24.1 — release notes

Hotfix для сборки alpha.24. Исправлена ошибка Kotlin/Compose `DSL_SCOPE_VIOLATION` в `SpeedReaderScreen.kt` на строках с `maxHeight` внутри вложенного layout DSL. Предупреждение `stripDebugDebugSymbols` для OCR/native библиотек не является причиной падения и может оставаться: эти `.so` просто упаковываются без stripping.
