# Flux Reader 0.2.0-alpha.24.1

## Hotfix

- Исправлена ошибка компиляции `DSL_SCOPE_VIOLATION` в `SpeedMediaFrame`.
- `BoxWithConstraints.maxHeight` теперь сохраняется в локальное `viewportMaxHeight` до входа во вложенный `Column` DSL и используется явно для высоты изображения/placeholder.
- Добавлен self-check regression guard, запрещающий возврат к неявному `maxHeight` во вложенном scope.
- Поведение скорочтения и форматирование alpha.24 не менялись.

## Build

- versionName: `0.2.0-alpha.24.1`
- versionCode: `20033`
