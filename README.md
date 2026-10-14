# Flux Reader 0.2.0-alpha.83 — clean rewrite

## alpha.53 — аннотации как рабочий инструмент

- поиск по фрагментам, заметкам и названиям книг;
- область «Эта книга / Все книги» и сортировка по дате, книге или главе;
- экспорт текущей выборки в Markdown или TXT;
- заметка прикрепляется прямо к выделению без обязательной отдельной NOTE-записи;
- границы существующего highlight/quote можно открыть на тексте, поправить ручками и сохранить;
- глобальный переход из списка аннотаций открывает нужную книгу на точном `chapter + block + char`.

## alpha.52.1 — unit-test compile hotfix

- New alpha.52 policy tests now use the project's existing JUnit 4 API instead of missing `kotlin.test` bindings.
- ReaderAnnotationHitPolicyTest and ReaderSelectionEdgePolicyTest compile in the Android debug unit-test classpath.
- Deprecated `Icons.Filled.Notes` usages were replaced with `Icons.AutoMirrored.Filled.Notes`.
- Added regression guards that reject `kotlin.test` imports in these Android unit tests and reject the deprecated Notes icon import.



## alpha.52

- edge auto-scroll для выделения и selection handles;
- перенос выделения через границу страниц в Screen Paging;
- тап по сохранённой аннотации открывает компактный popup возле текста;
- copy / note / recolor / delete для существующих highlights и заметок.



## alpha.50.1 — Compose compile hotfix

- Fixed `ReaderScreen.kt` note-save callback reading `MaterialTheme.colorScheme` from a non-composable lambda.
- The annotation note color is now resolved once in composable scope and passed into the callback as a plain ARGB value.
- Added a static regression guard for this callback boundary.

## alpha.50 — predictable Speed taps + persistent text tools

- Speed Reader: single tap = play/pause in normal and fullscreen, double tap = toggle fullscreen; Back exits fullscreen.
- media preview owns taps until dismissed; loading/error/chapter intro no longer react to playback taps.
- Reader long-press + drag selects a word range inside a text block.
- persistent highlights, quotes, notes and bookmarks are stored in `reader-annotations.json` without modifying book files.
- Reader has a dedicated “Заметки и выделения” sheet with jump/edit/delete actions.
- text actions include Speed Reader from selection, copy, highlight colors, quote, note, bookmark and book search.


## alpha.49 — live import progress + cancel + library continuity

- Large book, folder and ZIP imports now expose a live in-library progress card instead of looking frozen until the final snackbar.
- ZIP import reports archive copy progress first, then the current supported book and `current / total` inside the archive.
- Folder import reports the currently scanned entry and running object count.
- Import can be cancelled from the progress card; cancellation propagates to coroutine-aware folder/ZIP loops and archive temp files are cleaned in `finally`.
- The add FAB is hidden while import is active, preventing accidental competing import launches.
- The Continue card now has the same direct Speed Reader button as normal book cards.
- Removed a duplicated Reader bottom-bar visibility branch left by the fullscreen refactor.


## alpha.48 — Speed → Reader continuity + themed library cards + compact actions

- Leaving Speed Reader back to the normal Reader now maps the exact speed chapter offset back to a Reader block + character anchor instead of reopening the chapter start.
- Reader progress is synchronized on Speed exit, including chapter changes made inside Speed Reader.
- Grid / Compact / List book cards now use the current theme's `surfaceVariant` tint instead of a neutral/background-only presentation.
- The book-card `⋮` action was removed: its place is now a direct Speed Reader play button. Tap the card to read, tap ▶ for Speed Reader, long-press for file actions.
- Book long-press actions are now a compact popup without the book title, supporting subtitles, duplicated Open/Speed actions, or a Close button.
- Folder long-press actions use the same compact visual language.


## alpha.47 — long-press text actions + stable fullscreen anchor

- Single tap no longer selects words or opens the Speed action; normal taps remain reader navigation/chrome gestures.
- Long press on a word opens a compact contextual menu near the pressed word.
- Text actions now include: **Скорочтение отсюда**, **Копировать слово**, **Копировать предложение**, **Копировать абзац**, **Найти в книге**.
- Reader text rendered through `BasicText` now receives an explicit theme text color, fixing the near-black text regression on dark themes.
- EPUB links use a child-owned tap detector that consumes only actual link taps; non-link taps are left for the reader gesture layer.
- Normal ↔ immersive transitions now keep a separate stable exact anchor and never promote a one-page preview to the saved reading position.
- The pending fullscreen anchor is passed directly into preview pagination, then released only after complete pagination is committed.
- Speed Reader fullscreen entry moved from the top app bar into the playback control cluster as a dedicated outlined fullscreen button.


## alpha.46 — fullscreen anchor + contextual speed start

- Normal ↔ immersive keeps the current block/character anchor instead of jumping to the chapter start.
- Reflow no longer replaces the current page with a blocking “Подготавливаю страницы…” screen.
- Removed the old fullscreen line-snap that could shift the scroll offset.
- Fullscreen uses tighter real guards so more text fits without wasting vertical space.
- The normal Reader speed action is now an icon-only circular button with an error/red outline.
- “Скорочтение с выбранного слова” moved out of the overflow menu: tap a word in normal reading to highlight it and open the contextual “Скорочтение отсюда” action.
- One explicit fullscreen icon remains as an entry point to the same single immersive state.

> Alpha 24: Speed Reader rebuilt around the Reedy interaction model: compact speed/profile controls, separate Display / Behavior / Advanced settings, tap-to-pause text preview, image pauses, chapter intros, 3000 WPM range and volume-key control. Reader typography now preserves heading roles, symmetric page margins and adds a JetBrains reading profile.

> Build hotfix: Tesseract4Android 4.9.0 is resolved from JitPack; `settings.gradle.kts` contains a repository filter limited to its group.

Линия 0.2 написана заново. UI/reader implementation из 0.1.x не используется как база; совместимость сохраняется на уровне библиотеки, прогресса и части настроек.

## База 0.2

- библиотека и импорт EPUB / FB2 / FB2.ZIP / TXT / PDF;
- миграция старого `library.json` и `reader_settings`;
- EPUB по spine с загрузкой только текущей главы;
- EPUB 3 nav / EPUB 2 NCX оглавление;
- FB2 streaming parse + chapter-cache;
- TXT reader;
- PDF display через системный `android.graphics.pdf.PdfRenderer`;
- PDF text layer через sandboxed AndroidX PDF;
- zoom PDF 100–250% и быстрый переход по страницам;
- независимые позиции Reader / Speed Reader / PDF;
- точное восстановление Reader: глава + блок + символьный offset (с legacy pixel fallback);
- полноширинные изображения с downsample + memory cache;
- визуально настраиваемый ленивый Speed Reader;
- cancellable jobs + timeout → управляемое error-state.


## Новое в alpha.45 — единый immersive-режим и импорт ZIP

- Обычный Reader больше не имеет двух конкурирующих fullscreen-состояний: обычный UI и единый immersive-режим переключаются тапом по центру.
- Immersive скрывает системные панели и лишний chrome, сохраняя минимальный cutout/progress HUD; Back или повторный тап возвращают обычный интерфейс.
- В нижней панели обычного Reader появилась отдельная аккуратная кнопка **«Скорочтение»**; дублирующий пункт быстрого запуска из меню удалён.
- Speed Reader оставляет только один fullscreen без дополнительного состояния «показать/скрыть fullscreen controls»; тап в immersive возвращает обычный Speed UI.
- Добавлен импорт **ZIP-архивов**: EPUB/FB2/FB2.ZIP/TXT/PDF внутри архива импортируются пакетно с сохранением безопасной структуры папок.
- Архивный импорт защищён от `../`/absolute path traversal, слишком глубоких путей, чрезмерного числа файлов и zip-bomb-подобных объёмов.

## Новое в alpha.44 — раздельный Speed Reader chrome и контраст

- В fullscreen карточка контекста больше не лежит под панелью управления: при видимых controls viewport резервирует 92 dp и поднимает pause-preview с отдельным зазором.
- Нижний reserve участвует в расчёте безопасной позиции ORP/фокусного слова, поэтому панель не вытесняет текст поверх карточки.
- `PlaybackControls` получил самостоятельную контрастную поверхность, рамку и elevation; центральная Play/Pause увеличена и визуально отделена от переходов по предложениям.
- Карточка контекста получила более плотный фон, рамку/elevation и более читаемый текст.
- Верхний прогресс/HUD стал немного контрастнее, без увеличения количества элементов.

## Новое в alpha.43 — формы библиотеки, IME и семантика Save / Cancel / Close

- Диалоги без несохранённых данных (`Добавить`, выбор папки назначения) используют `Закрыть`, а формы редактирования — `Отмена` + явное подтверждение.
- Создание/переименование папки автоматически фокусирует имя; при переименовании текущее имя выделяется целиком, IME `Done` подтверждает валидное значение.
- Редактор метаданных получил последовательные IME `Next`, однострочные поля, сохранение только при непустом названии и явное закрытие клавиатуры перед Save/Cancel/cover picker/delete.
- Поиск библиотеки и поиск по книге используют IME `Search`; поиск по книге автоматически получает фокус при открытии.
- Поле имени пользовательской палитры использует IME `Done`, без случайного перевода строки.
- Правила валидности имени папки и названия книги вынесены в `LibraryFormPolicy` и покрыты unit-tests.


## Новое в alpha.42 — единая логика Back / Close в модальных экранах

- В Reader TOC, поиске и настройках появился единый заголовок с явной кнопкой закрытия.
- Во вложенных настройках Reader системный Back возвращает на основной экран настроек, а не закрывает весь sheet.
- Во вложенных настройках Speed Reader Back возвращает в хаб настроек; Close закрывает настройки полностью.
- Удаление книги, папки и пакетное удаление используют единый destructive-confirm dialog с error-цветом подтверждения.
- Удалён отдельный дублирующий `SpeedSheetHeader`; Reader и Speed Reader используют общий modal primitive.


## Новое в alpha.41 — единые действия библиотеки + Back navigation audit

- Убраны отдельные кнопки Speed Reader с каждой карточки книги: карточка снова отвечает только за открытие, а `long press`/`⋮` открывают единое полное меню.
- Меню книги теперь начинается с `Открыть` и `Скорочтение`, затем идут редактирование и файловые операции; удаление оформлено как destructive action.
- Меню папки приведено к тому же порядку: `Открыть` → создание/переименование → copy/move → удаление.
- Карточка «Продолжить» поддерживает long press и использует то же меню книги.
- Удалён двойной `speedPause()` при возврате стрелкой из Speed Reader.
- Выбор маршрута для системного Back вынесен в `AppBackDestinationPolicy` и покрыт unit-test: Reader/PDF → Library, Speed → исходный режим, Settings → предыдущий экран.
- Удалена двойная проверка supporting-text в общем элементе действия библиотеки.
- `package-release.command` теперь создаёт ZIP с одной корневой папкой проекта, совпадающей с командами установки.

## Новое в alpha.40 — системный UI audit + selection UX + stale-layout guard

- long press книги снова открывает действия; мультивыбор включается отдельной кнопкой;
- массовые операции заблокированы при пустом выборе;
- удалены неиспользуемые callback-параметры карточек;
- общие switch/slider primitives используются и глобальными настройками, и Speed Reader;
- устаревший результат пагинации после rotation/fullscreen не может попасть в UI или LRU cache.

## Новое в alpha.39 — UI dedup + fullscreen stability + memory pressure

- fullscreen Reader больше не рисует отдельные стрелки поверх уже существующих edge-tap зон;
- fullscreen-панель Reader оставляет только оглавление, скорочтение, настройки и явный выход из fullscreen; возврат в библиотеку остаётся в обычной верхней панели/системной кнопке Back;
- Reader и Speed Reader используют один `FullscreenSystemBarsEffect`, а общий поиск Activity вынесен в одно место;
- обычные и fullscreen-кнопки Speed Reader используют один playback-компонент вместо двух почти одинаковых реализаций;
- удалена дублирующая вкладка общих настроек «Быстрые»: параметры остались только в своих разделах «Оформление», «Чтение», «Библиотека» и «Скорочтение»;
- удалено мёртвое состояние `preparingKey` из фоновой пагинации;
- `MainActivity.onTrimMemory()` передаёт `UI_HIDDEN/BACKGROUND` в ViewModel: отменяется лишний prefetch и поджимаются bitmap/chapter caches без сброса позиции книги.

## Новое в alpha.38 — instant anchor preview + background pagination

- экранный режим сначала готовит одну быструю страницу прямо от сохранённого block/char anchor;
- полная точная пагинация главы продолжает работу кооперативно и бесшовно заменяет preview;
- preview не попадает в pagination LRU, не показывает ложное `1/1` и не перескакивает через границу главы;
- при смене шрифта/геометрии preview стартует с текущей видимой позиции, а не с начала главы;
- фоновый prefetch соседних глав стартует с короткой задержкой, чтобы не конкурировать с первым кадром, и сохраняет next-first порядок;
- после полной разметки текущей главы следующая глава заранее получает `PreparedReaderPagination` под текущие экран/шрифт;
- Reader → Library теперь оставляет только 1/8 embedded-image cache и 1/4 chapter cache; persistent parsed-cache на диске сохраняет быстрый reopen.

## Новое в alpha.37 — hot-path Reader + bitmap lifecycle

- пагинация длинных абзацев больше не создаёт 32K-временную строку на каждую экранную страницу: окно начинается с 4–12K и расширяется только при необходимости;
- повторные Compose-recomposition не пересоздают без необходимости стили и уже подготовленные текстовые срезы экранной страницы;
- одинаковые изображения EPUB/FB2 дедуплицируются, а число параллельных декодирований ограничивается в зависимости от memory class устройства;
- memory-cache изображений учитывает ревизию исходной книги и не возвращает старую картинку после замены файла;
- PDF native-render сериализован, устаревшие результаты защищены generation-token и освобождаются после отмены;
- максимальный PDF bitmap автоматически снижается на low-memory устройствах, сохраняя прежний потолок 8 MP на устройствах с большим запасом памяти.

## Новое в alpha.24 — Reedy-поток + книжная типографика

- основной экран Speed Reader оставляет скорость и три профиля темпа; точные параметры вынесены в отдельные окна «Отображение», «Поведение» и «Расширенные»;
- RSVP поддерживает диапазон 100–3000 WPM, плавный разгон, интеллектуальные паузы и точную ORP-фиксацию;
- тап по области чтения переключает Play/Pause, а на паузе открывается текстовый контекст с переходом к выбранному слову;
- кнопки громкости в Speed Reader: короткое нажатие Play/Pause, удержание — переход по предложениям;
- изображения EPUB/FB2 больше не теряются при скорочтении: изображение показывается на настраиваемое время, затем RSVP плавно продолжает;
- при входе в новую главу показывается отдельная заставка главы, а заголовочный кластер не проговаривается повторно слово за словом;
- прогресс показывает главу/книгу и приблизительное время до конца текущей главы;
- EPUB `h1/title` и FB2 title-блоки получают отдельную роль TITLE; кэш FB2 пересобирается один раз из-за новой семантики заголовков; заголовки и названия глав центрируются и визуально отделяются от основного текста;
- текстовые и графические блоки используют одинаковые боковые поля;
- добавлен профиль «JetBrains»: JetBrains Nerd, симметричные поля, спокойное левое выравнивание и автоматические переносы; сам файл шрифта по-прежнему импортируется пользователем через SAF и не встраивается в проект.

## Новое в alpha.19 — лаконичный интерфейс

- главная шапка теперь называется «Библиотека» и показывает только количество книг; номер версии убран в «О программе»;
- переключатель темы убран из шапки — тема меняется в настройках, а верхняя панель стала заметно спокойнее;
- поиск получил короткий placeholder «Поиск», а сортировка и группировка скрываются за одной кнопкой фильтров;
- карточки книг больше не показывают отдельную кнопку редактирования: быстрые действия — чтение/Speed Reader и `⋮`, остальные операции находятся в меню;
- диалоги добавления, действий книг/папок и выбора назначения превращены из тяжёлых outlined-кнопок в компактные строки с иконками;
- действия и настройки получили короткие подписи: «Изменить», «Копировать», «Дубликат», «Экспорт», «Навигация», «Интервал», «Абзацы» и т. п.;
- вторичные действия обычного Reader собраны в `⋮`, поэтому верхняя панель оставляет на виду только оглавление, поиск и параметры чтения;
- нижний индикатор Reader сокращён до компактного `глава/всего • страница/всего`;
- Speed Reader лишился отдельной кнопки переключения умного темпа в top bar — режим темпа остаётся в его настройках;
- Material 3 shapes/типографика унифицированы на уровне темы; скругления и веса заголовков теперь согласованы между экранами;
- PDF использует компактные подписи страницы и одинаковые content descriptions;
- логика предварительной пагинации alpha.17 и файловые операции alpha.18 не менялись.

## Новое в alpha.18 — управление книгами и папками

- виртуальные папки библиотеки теперь сохраняются отдельно и могут существовать даже без книг;
- у книг появилось меню действий: метаданные, копирование в папку, перемещение, отдельный физический дубликат, экспорт и удаление;
- «Копировать в папку» не расходует место: одна запись книги получает дополнительное размещение;
- «Дублировать» создаёт отдельный внутренний файл, отдельную запись и сбрасывает прогресс копии;
- у папок появились создание подпапки, переименование, копирование дерева, перемещение и безопасное удаление;
- удаление папки не удаляет книги: содержимое и вложенные папки поднимаются на один уровень выше;
- перемещение/переименование папки атомарно переписывает все вложенные пути книг;
- коллизии имён папок проверяются без учёта регистра; копия папки получает безопасное уникальное имя;
- экспорт книги использует системный Android SAF `CreateDocument`, поэтому приложению не нужны широкие разрешения на внешнее хранилище;
- импорт каталогов теперь сохраняет и пустые подпапки, а корневая библиотека не считается пустой, если в ней есть только пользовательские папки;
- добавлены pure-Kotlin regression tests для пустых папок, remap путей и конфликтов имён.

## Новое в alpha.17 — предварительная пагинация экрана

- режим Screen Paging больше не использует `LazyColumn` как движок определения границ страницы;
- текущая глава предварительно измеряется `TextMeasurer` под фактический viewport, шрифт, line-height, поля и выравнивание;
- страницы заканчиваются только на реальных границах строк через `getLineBottom/getLineEnd`;
- позиция чтения хранится как `chapter + block + char offset`, поэтому смена шрифта/ориентации пересчитывает страницы без потери места;
- изображения получают отдельные подготовленные страницы, а длинные абзацы измеряются ограниченными окнами.

## Новое в alpha.16.2 — fullscreen viewport + единый импорт

- fullscreen разделён на отдельную HUD-зону камеры и отдельный viewport текста;
- прогресс слева и номер главы справа, центр верхней зоны свободен под вырез камеры;
- при входе и после остановки ручного скролла верх текста привязывается к измеренной полной строке;
- одна кнопка «Добавить» открывает выбор «файл» или «папка»;
- рекурсивный импорт подпапок и структура библиотеки из alpha.16 сохранены.

## Новое в alpha.16 — папки библиотеки + Reader typography

- системный выбор каталога импортирует EPUB / FB2 / FB2.ZIP / TXT / PDF рекурсивно вместе со всеми подпапками;
- структура каталогов сохраняется в библиотеке и переживает перезапуск приложения;
- режим «Папки» показывает иерархию с хлебными крошками, переходом вверх и вложенными каталогами;
- одинаковый файл в нескольких каталогах не копируется повторно: одна запись книги может принадлежать нескольким путям;
- поиск учитывает пути папок, а редактор метаданных показывает, в каких каталогах находится книга;
- повторный импорт папки добавляет новые книги и новые связи без размножения SHA-256 дубликатов;
- одновременный запуск двух импортов блокируется, а новые книги больше не попадают в «Продолжить», пока их действительно не открыли;
- Reader использует реальные line-top координаты Compose после layout вместо одной приблизительной высоты строки для page anchors;
- при fullscreen/reflow текущая экранная страница повторно привязывается к реальной верхней границе строки;
- для обычных абзацев включены paragraph line breaking + автоматические переносы; пресет «Книга» использует естественное левое выравнивание и книжный абзацный отступ;
- возврат из глобальных настроек ведёт обратно на тот экран, откуда настройки были открыты.
- EPUB/XHTML больше не сплющивает вложенные списки и контейнерный текст: аннотации, обычные блоки, `ul/ol/li` и вложенные пункты сохраняют структуру;
- пункты EPUB-списков рендерятся отдельными строками с уровнями `• / ◦ / ▪`, а ссылки остаются независимыми и кликабельными вместо склейки названий.

## Hotfix alpha.13.2 — unit-test compile

- исправлена некорректная многострочная обычная Kotlin-строка в `SpeedReaderCursorTest.kt`;
- тест навигации по абзацам теперь использует явные `\n\n`;
- static self-check проверяет test Kotlin-файлы на незакрытые обычные строковые литералы до запуска Gradle;
- runtime-поведение Smart Speed 3.0 и полноэкранного режима не менялось.

## Новое в alpha.13 — Smart Speed 3.0 + fullscreen

- полноэкранный Speed Reader скрывает app bar и системные панели, оставляя минимальные controls;
- переходы назад/вперёд работают по предложениям; движок поддерживает и переходы по абзацам;
- после долгой паузы доступен auto-rewind к смысловой границе;
- на паузе можно показывать локальный контекст вокруг текущей фиксации;
- PHRASE/FOCUS адаптируют количество слов к сложности текущих токенов;
- добавлены профили чтения для художественного, учебного и технического текста.

## Новое в alpha.12 — Smart Speed 2.0 / micro-pauses

- умный Speed Reader получил отдельные микропаузы **после конца предложения**, после `, ; :` и тире; паузы задаются в миллисекундах, поэтому не исчезают на высоком WPM;
- пауза после абзаца теперь имеет собственную регулируемую длительность и может работать независимо от пунктуационных микропауз;
- адаптивный конец предложения учитывает его длину: длинные предложения получают небольшое дополнительное окно для интеграции смысла;
- замедление длинных/сложных слов вынесено в отдельную настройку вместо жёстко зашитого коэффициента;
- добавлен плавный разгон после `Play`: можно выбрать стартовый процент от целевого WPM и длительность выхода на полную скорость;
- разгон использует smoothstep-кривую без резкого скачка между первой и второй фиксацией;
- появились три готовых профиля ритма: **Плавно**, **Баланс**, **Понимание**, после выбора каждый параметр всё равно можно менять вручную;
- режим **Ровно** остаётся действительно ровным и не добавляет семантических пауз; `Умно` использует пользовательские параметры, `Глубоко` усиливает смысловые/complexity задержки;
- smart-chunking, ORP, abbreviations и разбор границ абзацев из alpha.11 сохранены;
- pure-Kotlin тесты покрывают микропаузу после предложения, адаптацию к длине предложения и постепенный выход на целевой WPM.

## Новое в alpha.11 — Smart Speed + adaptive themes + icon pass

- Speed Reader получил три режима темпа: **Ровно**, **Умно** и **Глубоко**;
- умный темп меняет время фиксации по длине и сложности слова, цифрам/URL/аббревиатурам, пунктуации и границам абзацев;
- PHRASE/FOCUS умеют заканчивать фиксацию на смысловой границе вместо механической нарезки по N словам;
- исправлена сама smart-chunking логика: смысловой break теперь действительно завершает текущий chunk;
- распространённые сокращения (`см.`, `т.д.`, `стр.` и т.п.) не принимаются за конец предложения;
- ORP-фокус учитывает открывающие кавычки/скобки и ставится на букву, а не на знак пунктуации;
- добавлены **Rosé Pine Moon** и **CMX Purple**;
- у всех встроенных палитр теперь есть светлая и тёмная схема; Rosé Pine Moon в светлом режиме использует официальный companion **Rosé Pine Dawn**;
- режим оформления вынесен отдельно от палитры: **Системная / Светлая / Тёмная**; системный вариант автоматически следует теме Android;
- переключатель режима темы доступен и прямо на главной странице библиотеки;
- Speed Reader может отдельно следовать текущей теме приложения или использовать собственные цветовые пресеты;
- добавлены Speed Reader пресеты Rosé Moon, CMX Purple и Dawn;
- интерфейс получил больше Material icons: разделы настроек, умный темп, поиск, сортировки, группы, системный/light/dark режимы и навигация Speed Reader.

## Новое в alpha.10 — Library/Home 2.0 + Themes 2.0

- полностью обновлена главная библиотека: карточка «Продолжить», поиск, список и адаптивная сетка обложек;
- добавлены сортировки по недавним, названию, автору, серии, дате добавления и прогрессу;
- книги можно объединять в пользовательские группы, а библиотеку — показывать полками по группам, сериям или авторам;
- серия книги и её номер теперь отдельные метаданные и участвуют в сортировке/группировке;
- редактор метаданных позволяет вручную менять название, автора, серию, номер, группы, издательство, год, язык и описание;
- пакетный импорт принимает сразу несколько книг; SHA-256 защита отсеивает дубликаты;
- Smart Import извлекает EPUB/FB2 title/author/series/index/publisher/language/year/description и использует имя файла как fallback;
- EPUB 2/3 и FB2 обложки извлекаются автоматически; свою обложку можно выбрать из изображения или удалить;
- большие обложки декодируются с downsample; пользовательские изображения валидируются до активации;
- Reader получил дополнительные анимации перелистывания: None / Slide / Fade / Zoom с регулируемой длительностью;
- добавлены популярные темы OLED, Dark, Paper, Sepia, Nord, Dracula, Solarized Dark, Catppuccin и Gruvbox;
- тема «Моя» имеет собственное имя и точную RGB-настройку background / surface / text / accent;
- Speed Reader получил дополнительные визуальные пресеты OLED / Paper / Cyber / Nord / Dracula / Sepia и настраиваемую focus-band под текущим словом/фразой;
- настройки библиотеки, группировки, темы, page-turn animation и новых параметров Speed Reader сохраняются между запусками.

## Новое в alpha.9

- полностью переработан layout Speed Reader для длинных слов и фраз: RSVP больше не раскладывает части слова поверх друг друга;
- PHRASE / FOCUS по умолчанию держатся в одну строку и автоматически уменьшают размер до доступной ширины; при желании можно разрешить до 2–3 строк;
- автоподгонка использует реальный Compose `TextMeasurer`, а не оценку по числу символов;
- направляющая больше не проходит сквозь текст: вертикальная линия имеет центральный разрыв, дополнительные метки находятся по краям разрыва;
- добавлены отдельные настройки минимального размера, насыщенности, межбуквенного и межстрочного интервала, ширины текстовой зоны и вертикального положения;
- режим PHRASE показывает всю фразу полноконтрастно, FOCUS приглушает контекст, RSVP сохраняет ORP-фиксацию;
- фокус можно подсвечивать по одной ORP-букве или целым словом;
- отдельно настраиваются видимость прогресса, высота/толщина направляющей и метки фиксации;
- Speed Reader получил собственный выбор System / Sans / Serif / Mono / JetBrains Nerd;
- в обычный Reader также добавлен режим `JetBrains Nerd`; пользователь может импортировать собственный `.ttf/.otf` через системный picker, файл копируется во внутреннее хранилище приложения;
- если JetBrains Nerd ещё не импортирован или файл недоступен, используется безопасный `Monospace` fallback;
- файл шрифта не входит в исходники/архив Flux Reader и не требует INTERNET permission;
- верхняя панель Speed Reader теперь использует выбранные фон/цвет текста, поэтому пресеты Paper/Cyber выглядят цельно.

## Новое в alpha.8

- FB2 `<body name="notes">` больше не отбрасывается: примечания и сноски входят в outline и доступны для перехода;
- fragment-only ссылки FB2 (`#note42`) получают реальный поиск по индексам якорей глав, с fallback-поиском для старых/нестандартных книг;
- FB2 cache v3 сохраняет anchors + inline links и инвалидируется также по `lastModified`;
- FB2 стихи сохраняются как отдельный `POEM` block со строками через перенос, а не как обычные абзацы;
- FB2 таблицы сохраняют текст даже в ячейках без вложенного `<p>`;
- EPUB таблицы превращаются в стабильные `TABLE` rows, включая ссылки и anchors внутри ячеек;
- EPUB контейнеры `poem/poetry/verse/stanza` сохраняют поэтическую роль текста;
- добавлены отдельные reader styles для `POEM` и `TABLE`;
- oversized FB2 `<binary>` обрывается до base64 decode, чтобы повреждённая книга не раздувала память;
- переходы по ссылкам теперь используют нейтральные сообщения и работают одинаково для EPUB/FB2.

## Новое в alpha.7

- EPUB-текст теперь сохраняет реальные inline-ссылки вместо потери `<a href>` при flattening;
- относительные ссылки нормализуются относительно текущего XHTML-файла, включая `../`, ссылки на другую главу и `#fragment`;
- `id`, `xml:id` и именованные якоря EPUB привязываются к конкретному reader-block;
- внутренние ссылки переходят прямо к нужной главе и блоку-якорю;
- сноски, оформленные как обычные EPUB anchors/links, работают тем же механизмом;
- перед переходом сохраняется точный viewport `chapter + block + pixel offset`; кнопка истории возвращает туда без потери позиции;
- история переходов по ссылкам ограничена 128 позициями и очищается при явном переходе из оглавления/поиска;
- внешние `http/https/mailto/...` ссылки передаются системному обработчику без добавления INTERNET permission;
- кликабельные ссылки визуально выделяются и больше не конфликтуют с tap-зонами перелистывания;
- parser smoke test проверяет реальный переход `chapter.xhtml -> other.xhtml#footnote` и привязку footnote anchor.

## Новое в alpha.6

- введён единый page controller для обычного Reader;
- нижние кнопки теперь означают **предыдущую / следующую страницу**, а не предыдущую / следующую главу;
- тап по левому/правому краю и горизонтальный свайп вызывают ровно тот же page controller, что и кнопки;
- режим «Страницы» отключает свободный вертикальный drag и двигается только по дискретным page anchors;
- длинный абзац умеет переходить на следующую страницу внутри того же блока через сохранённый pixel offset;
- история до 256 page anchors делает движение назад симметричным движению вперёд;
- быстрые повторные жесты отменяют незавершённый page transition, поэтому не должно быть случайных двойных перелистываний;
- изменение шрифта/полей/интервалов/режима навигации автоматически сбрасывает устаревшую историю page anchors;
- при переходе через границу главы сохраняется точная последняя страница, поэтому возврат назад возвращает туда же;
- режим «Прокрутка» сохраняет обычный вертикальный scroll, но кнопки / тапы / свайпы всё равно выполняют page navigation;
- оглавление и поиск очищают временную историю страниц и переходят к новой точке чтения без конфликтов;
- `selfcheck` запрещает возврат `scrollBy/animateScrollBy` как механики горизонтального перелистывания.

С alpha.5 также сохраняются настраиваемые tap-зоны, автоскрытие chrome, System/Serif/Sans/Mono, выравнивание, поля, абзацные интервалы и пресеты оформления.

## Из alpha.4

- OCR для PDF-сканов запускается только когда у страницы нет text layer;
- Tesseract `rus/ukr/eng` через `tesseract4android 4.9.0`;
- bundled ML Kit Latin fallback без runtime INTERNET permission;
- поиск по EPUB/FB2/TXT и PDF text layer;
- `scripts/build-debug.command --with-ocr` автоматически добавляет `tessdata_fast` rus+ukr+eng.

## OCR

Обычная сборка содержит ML Kit Latin fallback, но не тяжёлые Tesseract traineddata.

Полный офлайн rus + ukr + eng APK:

```bash
scripts/build-release.command --with-ocr
```

Отдельно скачать модели в исходники:

```bash
scripts/fetch-ocr-models.command
```

В приложении OCR можно выключить, выбрать `rus/ukr/eng` и отключить ML Kit fallback.

## Новое в alpha.21

- Reader подогревает соседние главы в фоне и отменяет prefetch при выходе/смене режима, чтобы переходы по книге были быстрее без блокировки интерфейса.
- Готовая экранная пагинация хранится в небольшом LRU-кэше текущей книги: возврат к уже рассчитанной главе/геометрии не запускает повторный полный расчёт.
- Физические страницы получили типографические правила: новая проза не начинает страницу одной одинокой строкой внизу, односложный хвост абзаца переносится вместе с предыдущей строкой, заголовок не остаётся внизу без места минимум для двух строк текста.
- Speed Reader рассчитывает безопасную позицию фиксации по реальной высоте текста и резерву нижнего сегмента/контекста. При паузе крупное слово не прыгает, а направляющие не заходят в читаемую область даже при крупном шрифте.
- Добавлены unit-тесты политик пагинации, LRU-кэша и геометрии фиксации.

## Новое в alpha.22

- Экранная пагинация теперь работает по целому числу строк, а не по произвольной высоте в пикселях: остаток ниже последней полной строки намеренно оставляется пустым.
- При тех же настройках, где сейчас помещаются 27 полных строк, страница сохраняет этот целочисленный бюджет; изменение размера шрифта, межстрочного интервала и вертикальных полей автоматически пересчитывает доступное количество строк.
- Пагинатор одновременно проверяет и лимит строк, и фактический `TextLayoutResult`: если последняя строка не помещается полностью по реальным метрикам шрифта, она переносится на следующую страницу.
- Межабзацные интервалы, заголовки и правила widow/orphan из alpha.21 остаются дополнительными ограничителями, поэтому на сложной странице строк может быть меньше, но частично обрезанной строки быть не должно.
- Добавлены unit-тесты целочисленной сетки страницы и отдельный pure-Kotlin helper `ReaderPageGridCalculator`.

## Новое в alpha.23

- Speed Reader держит экран включённым всё время, пока открыт режим скорочтения; системное значение `keepScreenOn` восстанавливается при выходе.
- RSVP-поток переработан в сторону Reedy: реальный `TextLayoutResult` используется для привязки центра подсвеченной ORP-буквы к точной центральной оси, поэтому длина слова больше не сдвигает точку фиксации.
- Текущее слово естественно уходит влево относительно ORP, а до шести следующих слов могут постоянно отображаться справа приглушённым цветом, не участвуя в позиционировании текущего слова.
- В обычном Reader появился запуск «Скорочтение с выбранного слова»: после выбора команды жесты перелистывания временно отключаются, а тап по слову запускает Speed Reader с точного символьного смещения этой главы. Работает и в scroll, и в screen-paging.
- Полоса Speed Reader умеет показывать прогресс текущей главы, всей книги или оба значения; для новой установки по умолчанию используется прогресс главы.
- Добавлены pure-Kotlin helpers/tests для привязки нажатого слова к `ChapterContent.plainText()` и периферийного контекста RSVP.

## Новое в alpha.28

- Постраничный режим больше не отбрасывает нижний остаток высоты из-за жёсткой сетки номинальных строк. Используется вся безопасная высота, а последняя строка принимается только если её реальный `TextLayoutResult.getLineBottom()` целиком помещается.
- Для длинных абзацев больше не переносится дополнительная строка только ради защиты от одиночного хвоста: это убирает заметную пустую полосу снизу. Короткие абзацы сохраняют аккуратное widow/orphan-поведение.
- `TITLE` теперь ExtraBold, `HEADING` — Bold с `FontSynthesis.Weight`, поэтому заголовки остаются жирными даже у пользовательской гарнитуры без отдельного bold-файла.
- EPUB восстанавливает заголовки, если издатель оформил `Глава N` и название обычными `<p>` через CSS.
- `install.command` теперь собирает и устанавливает non-debuggable local release APK через ADB. Это убирает debug-флаг, хотя Play Protect всё ещё может предупреждать о sideload неизвестного разработчика.

## Версия

- `versionName = 0.2.0-alpha.48`
- `versionCode = 20063`
- `VERSION.txt`
- `app/src/main/assets/build-info.json`
- версия в Library/Settings через `BuildConfig`

## Сборка — всегда с разархивированием

```bash
cd ~/Downloads
rm -rf FluxReader-0.2.0-alpha.48-clean
unzip -oq FluxReader-0.2.0-alpha.48-clean.zip -d .
cd FluxReader-0.2.0-alpha.48-clean
scripts/selfcheck.command
scripts/build-release.command
```

Полный OCR APK:

```bash
cd ~/Downloads
rm -rf FluxReader-0.2.0-alpha.48-clean
unzip -oq FluxReader-0.2.0-alpha.48-clean.zip -d .
cd FluxReader-0.2.0-alpha.48-clean
scripts/selfcheck.command
scripts/build-debug.command --with-ocr
```

Готовый APK:

```text
dist/FluxReader-0.2.0-alpha.48-release.apk
```


## 0.2.0-alpha.43.2 — unit-test compiler hotfix

- `ParsedBookDiskCacheTest` больше не использует deprecated Kotlin `createTempDir()`.
- Временные каталоги тестов создаются через `kotlin.io.path.createTempDirectory(...).toFile()`.
- Добавлен self-check guard против возврата `createTempDir()` в `app/src/test`, потому что проект компилируется с deprecations-as-errors.

## Установка — всегда с разархивированием

```bash
cd ~/Downloads
rm -rf FluxReader-0.2.0-alpha.48-clean
unzip -oq FluxReader-0.2.0-alpha.48-clean.zip -d .
cd FluxReader-0.2.0-alpha.48-clean
./scripts/selfcheck.command
./install.command
```

С OCR:

```bash
cd ~/Downloads/FluxReader-0.2.0-alpha.48-clean
./install.command --with-ocr
```

## ANR / freeze trace

```bash
scripts/capture-anr.command
```

## 0.2.0-alpha.15 — fullscreen-safe Reader

- Normal Reader has an explicit fullscreen mode.
- `safeDrawing`/display-cutout aware viewport prevents content from entering camera/system unsafe regions.
- Screen Paging anchors are snapped to line height so the next page never starts in the middle of a text line.
- Old persisted Screen Paging offsets are repaired on reopen.
- In fullscreen, the camera band uses left/right space for subtle progress/chapter markers; center stays clear.
- Wide screens use a capped reading column while side gutters remain edge-tap/swipe navigation space.

## 0.2.0-alpha.15.1 — Android compile hotfix

Removed invalid explicit Compose imports for `calculateTopPadding` and `calculateBottomPadding`. The methods continue to be invoked on `PaddingValues`; alpha.15 fullscreen/cutout and line-safe pagination behavior is otherwise unchanged.

## Что нового в alpha.29

- реалистичнее выглядящее перелистывание `Лист` для постраничного чтения;
- направленная анимация при переходе через границу главы;
- плавный вход/выход из полноэкранного режима, включённый по умолчанию;
- отключение и точная длительность полноэкранной анимации в расширенных настройках.


## Что нового в alpha.30

- верхние вкладки общих настроек показывают стрелку справа/слева, когда за краем есть скрытые разделы; по стрелке можно прокрутить ряд;
- вкладки отделены от содержимого настроек горизонтальным разделителем;
- «О программе» показывает версию/build, авторство `n.revis` и краткое описание поддерживаемых форматов и основных режимов.


## 0.2.0-alpha.31
Минималистичные overflow-стрелки настроек, Compose API cleanup и первый проход оптимизации памяти/bitmap cache.

## 0.2.0-alpha.32

- стрелки скрытых вкладок получают отдельные боковые зоны и больше не накладываются на названия разделов;
- долгий тап по книге в сетке, компактном и списочном виде открывает меню действий;
- долгий тап по папке открывает меню операций с папкой;
- старые кнопки `⋮` сохранены как явный альтернативный способ открыть то же меню;
- существующая векторная иконка Flux Reader увеличена внутри того же единого тёмного фона, без смены её стиля.


## 0.2.0-alpha.38 — instant anchor preview + background pagination

- Anchor-aware one-page preview before full screen pagination.
- Partial pagination is explicitly marked incomplete and never cached.
- Completion transition remaps by exact block/char anchor.
- Background adjacent-chapter prefetch yields initial-page priority.
- More aggressive Reader memory trimming on return to Library.

## 0.2.0-alpha.37 — Reader hot-path + bitmap lifecycle

- адаптивные окна предварительного измерения длинных абзацев уменьшают временные строки и GC-нагрузку при первой пагинации;
- подготовленные срезы текста и TextStyle memoized между нерелевантными recomposition;
- EPUB/FB2 image requests дедуплицируются и декодируются с bounded parallelism 1/2/3 в зависимости от memory class;
- PDF native render сериализован и cancellation-safe: устаревший bitmap освобождается сразу после завершения отменённого native-вызова;
- PDF bitmap budget адаптируется к памяти устройства: 3/5/8 MP.

## 0.2.0-alpha.36 — Persistent reader cache

- EPUB/FB2 получают единый persistent parsed-cache для уже готовых outline и chapter models; повторное открытие книги больше не требует повторного XML/JSON parsing, пока исходный файл не изменился.
- Кэш валидируется по content hash + размеру + времени изменения файла и имеет собственную версию codec; повреждённые/stale записи безопасно игнорируются.
- Формат кэша — компактный streaming binary с атомарной записью и ограничением общего объёма 160 МБ; старые книги удаляются LRU, активная книга не вычищается.
- Повторные одновременные запросы одной главы объединяются через bounded striped mutexes, поэтому Reader и prefetch не парсят один XHTML параллельно.
- EPUB XML теперь передаётся DOM parser напрямую как InputStream, без промежуточного `readBytes()` для XHTML/OPF/nav; это уменьшает пиковую память на больших главах.
- Любая ошибка записи cache остаётся best-effort и не может сорвать открытие книги.

## 0.2.0-alpha.35.1

- Build-cleanup hotfix for alpha.35.
- Library Undo action uses `Icons.AutoMirrored.Filled.Undo`, removing the Compose directional-icon deprecation warning.
- Native OCR/PDF dependencies are unchanged; already-stripped third-party `.so` libraries may still produce the informational `stripReleaseDebugSymbols` packaging warning.

## 0.2.0-alpha.35

- долгий тап по книге теперь запускает режим множественного выбора; обычный тап в этом режиме добавляет или снимает выделение;
- массовые действия работают в Grid, Compact, List, папках и сгруппированных разделах: выбрать все видимые, добавить в папку, переместить и удалить;
- пакетные копирование/перемещение обновляют библиотеку одной транзакцией состояния и одним сохранением вместо цикла по книгам;
- добавлена отмена последнего изменения расположения книг для одиночных и массовых copy/move операций;
- системная кнопка «Назад» сначала закрывает режим выбора, не выходя из приложения;
- в строковом виде устранено задвоение кнопки скорочтения.

### 0.2.0-alpha.35 — Performance & stability

- обновление позиции одной книги без полной сортировки библиотеки;
- асинхронные memory-bounded обложки;
- предварительно нормализованный поиск библиотеки;
- adaptive chapter prefetch по memory class;
- автоматическое ужатие cover cache при входе в reader/PDF/speed-reader.


## Alpha.59 — experimental Wasteland full UI

The Wasteland theme now uses a dedicated programmed terminal shell and Wasteland UI primitives across Library, Reader, PDF, Speed Reader and Settings. It is intentionally much closer to the rugged CRT/industrial reference while preserving the normal UI for all other themes.

## Alpha.60 — Wasteland startup geometry hotfix

- Full routed screens are now measured against the complete CRT viewport.
- The Wasteland interactive area respects Android safe drawing insets while the physical shell remains edge-to-edge.
- The Library hardware rail is anchored to the bottom again and imported books remain visible.
- Wasteland list/grid modes no longer reserve the legacy floating-action-button clearance.


## Alpha.61 — FolderLibrary compiler hotfix

- Fixed the `Unresolved reference: settings` Kotlin compilation failure introduced in alpha.60.
- Folder/group library rendering now reads the active theme from `LocalFluxThemeMode` inside its own composable scope.
- Unified folder GRID/COMPACT/LIST bottom padding so Wasteland keeps the compact 18 dp rail clearance while normal themes retain the legacy 104 dp clearance.
- Added a self-check guard that rejects any future out-of-scope `settings.theme` use inside `FolderLibrary`.
