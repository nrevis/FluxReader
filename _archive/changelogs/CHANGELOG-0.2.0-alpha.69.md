# Flux Reader 0.2.0-alpha.69

- Исправлена компиляция `LibraryScreen.kt`: добавлен `import androidx.compose.foundation.border` для подсветки выбранных книг (`Modifier.border(...)`).
- Добавлена регрессионная проверка `scripts/selfcheck.command --static`, которая находит вызовы `.border(...)` без необходимого Compose-импорта.
- Сохранены интерактивная перемотка прогресса в Reader и множественное выделение книг из alpha.68.
- Обновлены `versionCode` (20086) и `versionName` (0.2.0-alpha.69); данные и формат библиотеки не менялись.
- Android SDK отсутствует в среде сборки автора архива: полная компиляция и запуск APK должны быть проверены на macOS с Android SDK.
