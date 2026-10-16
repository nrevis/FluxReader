# alpha.67 — экспериментальные Wasteland и Cyberpunk

После установки откройте Настройки → Оформление, включите «Экспериментальный дизайн», выберите Wasteland CRT или Cyberpunk. Выключение галочки возвращает обычный интерфейс без изменения книг.

Изменения: отдельный Cyberpunk HUD; собственные панели и кнопки; тематические папки (иконки, названия, меню, хлебные крошки); обновлённые Reader/PDF/Speed и настройки. Используется прежняя база и пакет `io.fluxreader.app`.

Проверка: `./scripts/selfcheck.command --static`, затем `./gradlew :app:compileDebugKotlin :app:compileReleaseKotlin :app:assembleRelease` на macOS.
