# Сборка Flux Reader

[← Обзор приложения](README.md)

## Требования

- JDK 17 или новее, совместимый с используемым Gradle и Android Gradle Plugin.
- Android SDK с Platform 37.0 и Build Tools 37.0.0.
- Доступ к репозиторию для клонирования и к серверам зависимостей для первой сборки.

Актуальные версии Gradle и зависимостей закреплены в исходниках проекта. Персональные настройки SDK и ключи подписи в репозиторий не входят.

## Локальная сборка на macOS

```sh
git clone https://github.com/nrevis/FluxReader.git
cd FluxReader
chmod +x gradlew scripts/*.command
scripts/build-release.command
```

Скрипт ищет SDK через `ANDROID_HOME`, `ANDROID_SDK_ROOT` или стандартный каталог Android SDK в macOS. Он запускает unit-тесты и создаёт release APK в `dist/`; имя берётся из `VERSION.txt`.

Для отдельного запуска проверок:

```sh
scripts/selfcheck.command --static
./gradlew :app:testDebugUnitTest :app:assembleRelease
```

## OCR

Обычная сборка использует ML Kit Latin как резервный OCR. Чтобы включить локальные модели Tesseract для русского, украинского и английского, выполните:

```sh
scripts/build-release.command --with-ocr
```

Загрузка языковых моделей требует интернета. Поддержка нужного языка зависит от моделей, включённых в конкретный APK.

## Подпись и выпуск обновлений

Текущие alpha-release APK используют локальный Android debug-keystore для совместимости с предыдущими установками. Новый APK можно установить поверх старого только при совпадении сертификата и увеличенном `versionCode`. На другой машине debug-keystore будет другим.

Сохраняйте исходный ключ в защищённом локальном хранилище; не добавляйте его, пароли и токены в Git. Переход на другой сертификат требует отдельного плана миграции установок.

Публикация вручную описана в [RELEASE-UPDATES.md](RELEASE-UPDATES.md). Для обычной установки приложения откройте [GitHub Releases](https://github.com/nrevis/FluxReader/releases).
