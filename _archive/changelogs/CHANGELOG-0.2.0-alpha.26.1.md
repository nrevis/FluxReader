# Flux Reader 0.2.0-alpha.26.1

## Build / packaging hotfix

- Исправлен clean ZIP: `gradle/wrapper/gradle-wrapper.jar` теперь обязательно входит в архив.
- `scripts/selfcheck.command` проверяет наличие и содержимое Gradle Wrapper до попытки компиляции.
- Добавлен `scripts/package-release.command`, который сохраняет wrapper JAR и проверяет финальный ZIP после упаковки.
- Ошибка `Could not find or load main class org.gradle.wrapper.GradleWrapperMain` больше не должна возникать из-за неполного релизного архива.

Функционально Reader/Speed Reader идентичны alpha.26; это только исправление сборочного пакета.
