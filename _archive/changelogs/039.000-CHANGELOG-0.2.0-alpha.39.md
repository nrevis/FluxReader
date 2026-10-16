# Flux Reader 0.2.0-alpha.39

- Проведён аудит повторяющихся UI-действий и реализаций.
- Убраны видимые fullscreen-стрелки Reader, дублировавшие существующие edge tap-зоны перелистывания.
- Fullscreen-панель Reader упрощена до оглавления, Speed Reader, настроек и явного выхода из fullscreen; отдельная кнопка «Назад» внутри fullscreen убрана как двусмысленная рядом с Fullscreen Exit.
- Скрытие/возврат системных панелей Reader и Speed Reader объединены в `FullscreenSystemBarsEffect`; три копии поиска Activity сведены к одной функции.
- `PlaybackControls` и `MinimalFullscreenControls` Speed Reader объединены в один компонент с опциональной кнопкой Fullscreen Exit.
- Удалена вкладка «Быстрые» общих настроек, повторявшая параметры других специализированных разделов.
- Удалено неиспользуемое `preparingKey` из фоновой пагинации.
- Добавлена обработка `onTrimMemory`: при `UI_HIDDEN` и `BACKGROUND` останавливается фоновый prefetch и уменьшаются bitmap/chapter caches без потери reader state.
- Добавлены alpha.39 regression guards на возврат этих дублей.
