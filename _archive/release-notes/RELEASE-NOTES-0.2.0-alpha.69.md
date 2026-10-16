# Flux Reader 0.2.0-alpha.69 — исправление сборки

Проблема alpha.68: `LibraryScreen.kt:995 Unresolved reference border`. Исправлен отсутствующий импорт `androidx.compose.foundation.border`. Все функции alpha.68 сохранены.

Проверка и установка на macOS:

```bash
cd ~/Downloads
rm -rf FluxReader-0.2.0-alpha.69-clean
unzip -q FluxReader-0.2.0-alpha.69-clean.zip
cd FluxReader-0.2.0-alpha.69-clean
chmod +x gradlew ./*.command scripts/*.command
./scripts/selfcheck.command --static
./gradlew :app:compileDebugKotlin :app:compileReleaseKotlin
./FluxReader-0.2.0-alpha.69-build-install.command
```

Распаковка только рабочей копии проекта не затрагивает книги на телефоне. APK собирается и устанавливается на подключённое Android-устройство; требуется установленный Android SDK и разрешённая отладка USB.
