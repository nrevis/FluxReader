# Flux Reader 0.2.0-alpha.93 — GitHub Updates

- Проверка GitHub Releases из настроек «О программе».
- Ежедневная необязательная автоматическая проверка и переключатель alpha-канала.
- Сведения о релизе, индикатор загрузки APK и подтверждение установки через Android.
- Проверка SHA-256, applicationId, versionCode и сертификата перед установкой.
- Команда `scripts/publish-github-release.command --publish` для ручной публикации APK и checksum.

Проверена готовая Android release-сборка: versionCode 20110, пакет io.fluxreader.app, подпись APK v2 и совпадение сертификата с alpha.92.

Репозиторий закрытый. Встроенная проверка GitHub Releases требует доступного приложению репозитория; без авторизации Android не сможет получать релизы из закрытого репозитория.
