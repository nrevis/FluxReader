# Flux Reader 0.2.0-alpha.40

- Библиотека: долгое нажатие на книгу снова открывает контекстные действия, а не незаметно включает множественный выбор.
- Множественный выбор вынесен в отдельный явный режим верхней панели; можно войти в него без заранее выбранной книги и выйти через Back/крестик.
- Массовые Copy/Move/Delete недоступны, пока не выбрана хотя бы одна книга.
- Удалены мёртвые `onEdit`-параметры карточек библиотеки: редактирование идёт через единое меню действий.
- `SettingsSwitch`/`SpeedSettingSwitch` и `FloatSetting`/`VisualSlider` объединены в общие `SettingsToggleRow` и `SettingsValueSlider`.
- Reader: добавлен live layout-key guard. Результат пагинации, закончившийся после поворота, fullscreen-перехода или смены геометрии, не может попасть в UI/кэш.
- Добавлен unit-test `ReaderPaginationCommitPolicyTest` и alpha.40 regression guards.
