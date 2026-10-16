# Flux Reader 0.2.0-alpha.26.1

Hotfix релизного пакета alpha.26. В предыдущем clean ZIP отсутствовал `gradle/wrapper/gradle-wrapper.jar`, поэтому `./gradlew` не мог найти `org.gradle.wrapper.GradleWrapperMain`.

В alpha.26.1 wrapper JAR включён в архив, self-check валидирует его до Gradle-сборки, а новый release-packager отдельно проверяет наличие wrapper JAR уже внутри готового ZIP.
