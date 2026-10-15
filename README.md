<div align="center">

# Flux Reader

**Книги, скорочтение и голос — в одном приложении для Android.**

EPUB · FB2 · FB2.ZIP · TXT · PDF · Аудиокниги

[**Скачать APK**](https://github.com/nrevis/FluxReader/releases/tag/v0.2.0-alpha.95) · [Исходники](https://github.com/nrevis/FluxReader) · [Все скриншоты](#gallery)

**Android 9+** · **0.2.0-alpha.95** · **Автор: n.revis**

</div>

---

Flux Reader объединяет домашнюю библиотеку, обычное чтение, показ текста в заданном темпе и озвучку. Можно читать страницу за страницей, следить за словами в Speed Reader или слушать книгу установленным голосом Android. Книги, позиции чтения и заметки хранятся на устройстве; чтение работает без интернета.

<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/library/books-list.jpg"><img src="docs/screenshots/library/books-list.jpg" width="250" alt="Книги: список с автором, серией и прогрессом"></a><br><sub>Библиотека</sub></td><td align="center" valign="top"><a href="docs/screenshots/reading/wasteland-immersive.jpg"><img src="docs/screenshots/reading/wasteland-immersive.jpg" width="250" alt="Wasteland: полноэкранное чтение"></a><br><sub>Чтение · Wasteland</sub></td><td align="center" valign="top"><a href="docs/screenshots/speed-reading/immersive-playback.jpg"><img src="docs/screenshots/speed-reading/immersive-playback.jpg" width="250" alt="Полноэкранное скорочтение во время воспроизведения"></a><br><sub>Speed Reader</sub></td>
</tr>
</table>


## Возможности с первого взгляда

| Библиотека | Чтение | Скорочтение | Звук и документы |
| --- | --- | --- | --- |
| Папки, группы, серии, авторы | Прокрутка и страницы | RSVP, фразы и фокус | Офлайн-озвучка Android TTS |
| Сетка, компактно и список | Полный экран и жесты | Настраиваемые WPM и паузы | Локальные аудиокниги |
| Поиск, фильтры, сортировка | Шрифты, поля и палитры | Контекст и точка фиксации | PDF и распознавание сканов |
| Импорт файлов, ZIP и каталогов | Закладки, цитаты и заметки | Сохранение позиции | Проверка обновлений через GitHub |

## Навигация

[Библиотека](#library) · [Импорт и организация](#import) · [Чтение](#reading) · [Настройка текста](#typography) · [Заметки](#notes) · [Скорочтение](#speed) · [Голос и аудиокниги](#audio) · [Оформление](#appearance) · [PDF и OCR](#pdf) · [Установка и обновления](#updates)

<a id="library"></a>

## 01 · Библиотека

Библиотеку можно показать крупными обложками, компактной сеткой или списком с автором, серией и прогрессом. Карточка **«Продолжить»** возвращает к последней книге, а путь над списком помогает перемещаться между папками.

- Фильтры **ALL / BOOKS / PDF / TXT / AUDIO** отделяют нужный тип содержимого.
- Поиск по библиотеке и сортировка по недавнему чтению, названию, автору, серии, дате добавления или прогрессу.
- Группировка по папкам, пользовательским группам, сериям и авторам; доступен и общий список без группировки.
- Папки с подпапками. Одна книга может находиться в нескольких папках без дублирования файла.
- Последняя открытая папка сохраняется при переходах между библиотекой, настройками и чтением, а также после перезапуска.
- Встроенные обложки EPUB/FB2, первая страница PDF или локально созданная обложка, если изображения нет. Обложки можно обновить.

<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/library/folders-compact.jpg"><img src="docs/screenshots/library/folders-compact.jpg" width="280" alt="Папки библиотеки: компактная сетка"></a><br><sub>Папки библиотеки: компактная сетка</sub></td><td align="center" valign="top"><a href="docs/screenshots/library/folders-list.jpg"><img src="docs/screenshots/library/folders-list.jpg" width="280" alt="Папки библиотеки: список"></a><br><sub>Папки библиотеки: список</sub></td>
</tr>
</table>


<details>
<summary><strong>Сравнить виды книг и открыть настройки библиотеки</strong></summary>

<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/library/folders-grid.jpg"><img src="docs/screenshots/library/folders-grid.jpg" width="280" alt="Папки библиотеки: крупная сетка"></a><br><sub>Папки библиотеки: крупная сетка</sub></td><td align="center" valign="top"><a href="docs/screenshots/library/books-compact.jpg"><img src="docs/screenshots/library/books-compact.jpg" width="280" alt="Книги: компактная сетка"></a><br><sub>Книги: компактная сетка</sub></td>
</tr>
</table>
<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/library/library-preferences.jpg"><img src="docs/screenshots/library/library-preferences.jpg" width="280" alt="Настройки вида, группировки и обложек"></a><br><sub>Настройки вида, группировки и обложек</sub></td>
</tr>
</table>


</details>

<a id="import"></a>

## 02 · Импорт и организация

Нажмите **«+»** в библиотеке, чтобы добавить книги, ZIP-архив, аудиофайлы или каталог вместе с подпапками. Здесь же создаётся новая папка.

| Что добавить | Поддержка |
| --- | --- |
| Электронные книги | EPUB, FB2, FB2.ZIP, TXT |
| Документы | PDF |
| ZIP-архив | Пакетный импорт поддерживаемых книг |
| Каталог | Импорт с подпапками и сохранением структуры |
| Аудиокниги | MP3, M4A, M4B, AAC, OGG/OGA, FLAC, WAV, OPUS, 3GP; воспроизведение зависит от кодеков Android |

При большом импорте показывается ход обработки; операцию можно отменить. Повторный импорт существующей книги может связать её с другой папкой вместо создания копии.

Для выбранных книг доступны добавление и перемещение в папку, создание папки из выделенного набора и удаление. **«Выделить все видимые»** помогает обработать текущую выборку сразу.

В редакторе метаданных можно изменить название, автора, серию и номер в ней, группы, издательство, год, язык и описание. Для книги доступны также действия с обложкой, копирование и перемещение через её меню.

<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/library/import-menu.jpg"><img src="docs/screenshots/library/import-menu.jpg" width="280" alt="Импорт книг, ZIP, аудио и папок"></a><br><sub>Импорт книг, ZIP, аудио и папок</sub></td><td align="center" valign="top"><a href="docs/screenshots/library/selection-actions.jpg"><img src="docs/screenshots/library/selection-actions.jpg" width="280" alt="Меню выбранных книг и пакетные действия"></a><br><sub>Меню выбранных книг и пакетные действия</sub></td>
</tr>
</table>


<details>
<summary><strong>Редактор метаданных и сортировка</strong></summary>

<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/library/book-metadata.jpg"><img src="docs/screenshots/library/book-metadata.jpg" width="280" alt="Редактирование метаданных книги"></a><br><sub>Редактирование метаданных книги</sub></td><td align="center" valign="top"><a href="docs/screenshots/library/sort-and-group.jpg"><img src="docs/screenshots/library/sort-and-group.jpg" width="280" alt="Сортировка и группировка библиотеки"></a><br><sub>Сортировка и группировка библиотеки</sub></td>
</tr>
</table>


</details>

<a id="reading"></a>

## 03 · Обычное чтение

Выберите **прокрутку** для непрерывного текста или **страницы** для чтения экран за экраном. Панели можно скрыть, оставив текст и компактный прогресс. Обычное чтение, Speed Reader и озвучка используют единое последнее место в книге. При переключении сохраняется глава и фрагмент текста; неактивные режимы не перезаписывают позицию.

- Оглавление с переходом к нужной главе и поиск по тексту книги.
- Переходы кнопками, горизонтальными свайпами и касаниями краёв: слева — назад, справа — вперёд; центральная область управляет панелями.
- Настройка ширины зон касания и автоматического скрытия панелей.
- Процент чтения, номер главы и страницы; переход через полосу прогресса.
- Иллюстрации и структура EPUB/FB2, включая заголовки, ссылки, сноски, стихи и таблицы.
- Запуск скорочтения из читалки или с выбранного фрагмента.

<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/reading/reader-controls.jpg"><img src="docs/screenshots/reading/reader-controls.jpg" width="280" alt="Чтение с навигацией и прогрессом"></a><br><sub>Чтение с навигацией и прогрессом</sub></td><td align="center" valign="top"><a href="docs/screenshots/reading/reader-immersive.jpg"><img src="docs/screenshots/reading/reader-immersive.jpg" width="280" alt="Чтение в полном экране"></a><br><sub>Чтение в полном экране</sub></td>
</tr>
</table>


<details>
<summary><strong>Оглавление, поиск и меню чтения</strong></summary>

<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/reading/reader-menu.jpg"><img src="docs/screenshots/reading/reader-menu.jpg" width="250" alt="Меню: оглавление, поиск, заметки и параметры текста"></a><br><sub>Меню: оглавление, поиск, заметки и параметры текста</sub></td><td align="center" valign="top"><a href="docs/screenshots/reading/table-of-contents.jpg"><img src="docs/screenshots/reading/table-of-contents.jpg" width="250" alt="Оглавление и переход к главам"></a><br><sub>Оглавление и переход к главам</sub></td><td align="center" valign="top"><a href="docs/screenshots/reading/book-search.jpg"><img src="docs/screenshots/reading/book-search.jpg" width="250" alt="Поиск по тексту книги"></a><br><sub>Поиск по тексту книги</sub></td>
</tr>
</table>


</details>

<a id="typography"></a>

## 04 · Текст под ваш привычный ритм

Начните с профиля **«Плотно»**, **«Книга»**, **«Крупно»** или **JetBrains**, затем настройте страницу под себя.

| Параметры | Что можно изменить |
| --- | --- |
| Шрифт | Системный, книжный, Sans, Mono, JetBrains Nerd; импорт своего TTF/OTF |
| Основной текст | Размер, межстрочный интервал, выравнивание слева или по ширине |
| Поля | Боковые, верхнее и нижнее |
| Абзацы | Интервал, красная строка, переносы слов |
| Заголовки | Центрирование, размер названия главы и номера/подзаголовка |
| Перелистывание | Лист, сдвиг, Fade, Zoom или без анимации; длительность перехода |
| Полный экран | Анимация входа и выхода, её длительность |

При выборе JetBrains Nerd без импортированного файла используется системный моноширинный шрифт. Настройки текста применяются и в обычном, и в полноэкранном режиме.

<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/reading/fonts-and-profiles.jpg"><img src="docs/screenshots/reading/fonts-and-profiles.jpg" width="280" alt="Профили чтения, шрифты и импорт TTF/OTF"></a><br><sub>Профили чтения, шрифты и импорт TTF/OTF</sub></td>
</tr>
</table>


<details>
<summary><strong>Точное форматирование, палитры и анимации</strong></summary>

<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/reading/paragraphs-and-headings.jpg"><img src="docs/screenshots/reading/paragraphs-and-headings.jpg" width="280" alt="Переносы, абзацы и оформление заголовков"></a><br><sub>Переносы, абзацы и оформление заголовков</sub></td><td align="center" valign="top"><a href="docs/screenshots/reading/tap-zones-and-animation.jpg"><img src="docs/screenshots/reading/tap-zones-and-animation.jpg" width="280" alt="Зоны касания и анимации страниц"></a><br><sub>Зоны касания и анимации страниц</sub></td>
</tr>
</table>
<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/reading/margins-and-theme.jpg"><img src="docs/screenshots/reading/margins-and-theme.jpg" width="280" alt="Поля, режим темы и цветовые схемы"></a><br><sub>Поля, режим темы и цветовые схемы</sub></td><td align="center" valign="top"><a href="docs/screenshots/reading/palettes-and-navigation.jpg"><img src="docs/screenshots/reading/palettes-and-navigation.jpg" width="280" alt="Палитры и способы навигации"></a><br><sub>Палитры и способы навигации</sub></td>
</tr>
</table>


</details>

<a id="notes"></a>

## 05 · Закладки, выделения и заметки

Долгое нажатие на текст открывает работу с фрагментом: его можно скопировать, выделить цветом, сохранить как цитату, дополнить заметкой или начать с него скорочтение. Границы выделения можно двигать; для существующей записи доступны изменение цвета, текста заметки и удаление.

Раздел **«Заметки и выделения»** собирает записи из текущей книги или всей библиотеки. Здесь есть фильтры по типу записи, поиск по фрагментам, заметкам и книгам, сортировка по дате, книге или главе, переход к сохранённому месту и экспорт выборки в **Markdown / TXT**.

Записи хранятся отдельно от исходных книг. На скриншоте показан интерфейс раздела до добавления первой записи.

<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/reading/annotations-workspace.jpg"><img src="docs/screenshots/reading/annotations-workspace.jpg" width="280" alt="Заметки, выделения, цитаты и поиск по записям"></a><br><sub>Заметки, выделения, цитаты и поиск по записям</sub></td>
</tr>
</table>


<a id="speed"></a>

## 06 · Speed Reader — чтение в заданном темпе

Текст появляется у фиксированной точки взгляда. Акцент выделяет букву или слово, а соседние слова и контекст помогают удерживать смысл. Скорость задаётся в **WPM — словах в минуту**, в диапазоне **100–3000**; это настройка темпа показа, а не обещание скорости понимания текста.

| Режим | Как показывается текст |
| --- | --- |
| **RSVP** | Последовательный показ слов у точки фиксации |
| **Фраза** | Показ группы слов за одну фиксацию |
| **Фокус** | Показ с акцентом на текущем фрагменте |

Профили темпа **«Ровно»**, **«Умно»** и **«Глубоко»** помогают выбрать характер показа. Дополнительно настраиваются число слов за фиксацию, умная разбивка и адаптивная длина фрагментов.

**Управление:** один тап — воспроизведение/пауза, двойной — полный экран, если эти жесты включены. Можно использовать кнопки на экране или кнопки громкости: короткое нажатие переключает воспроизведение, удержание переводит к соседнему предложению.

На паузе доступен окружающий текст: коснитесь нужного слова, чтобы продолжить с него. После долгой паузы можно автоматически вернуться к началу предложения. Переход между обычным чтением и скорочтением сохраняет связь с местом в книге.

<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/speed-reading/word-and-context.jpg"><img src="docs/screenshots/speed-reading/word-and-context.jpg" width="280" alt="Точка фиксации и контекст на паузе"></a><br><sub>Точка фиксации и контекст на паузе</sub></td><td align="center" valign="top"><a href="docs/screenshots/speed-reading/immersive-context.jpg"><img src="docs/screenshots/speed-reading/immersive-context.jpg" width="280" alt="Полный экран с контекстом и компактным управлением"></a><br><sub>Полный экран с контекстом и компактным управлением</sub></td>
</tr>
</table>


<details>
<summary><strong>Все группы настроек скорочтения</strong></summary>

| Группа | Возможности |
| --- | --- |
| **Отображение** | Шрифт и его импорт, тема читалки, ORP-направляющая, периферийные слова, сегмент/контекст под словом, прогресс главы/книги |
| **Поведение** | Тапы и кнопки громкости, контекст на паузе, умный возврат, время показа иллюстрации и заставки новой главы |
| **Тайминги** | Микропаузы после предложения, запятой и других разделителей, пауза между абзацами, замедление длинных слов |
| **Разгон** | Постепенный выход на выбранный темп после старта, иллюстрации или новой главы; начальная скорость и длительность разгона |
| **Геометрия** | Фиксированный размер или автоподгонка текста, горизонтальная и вертикальная точка фиксации, ширина области, насыщенность и межбуквенный интервал |
| **Фокус** | Акцент на ORP-букве или целом слове, число следующих слов, направляющая и поперечные метки |
| **Цвета** | Независимые цвета текста, фокуса, фона и направляющей |

<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/speed-reading/settings-hub.jpg"><img src="docs/screenshots/speed-reading/settings-hub.jpg" width="280" alt="Разделы настройки скорочтения"></a><br><sub>Разделы настройки скорочтения</sub></td><td align="center" valign="top"><a href="docs/screenshots/speed-reading/speed-and-pacing.jpg"><img src="docs/screenshots/speed-reading/speed-and-pacing.jpg" width="280" alt="Режимы, профили темпа, WPM и слов за фиксацию"></a><br><sub>Режимы, профили темпа, WPM и слов за фиксацию</sub></td>
</tr>
</table>
<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/speed-reading/display-preferences.jpg"><img src="docs/screenshots/speed-reading/display-preferences.jpg" width="280" alt="Шрифт, ORP, следующие слова и прогресс"></a><br><sub>Шрифт, ORP, следующие слова и прогресс</sub></td><td align="center" valign="top"><a href="docs/screenshots/speed-reading/behavior-preferences.jpg"><img src="docs/screenshots/speed-reading/behavior-preferences.jpg" width="280" alt="Касания, кнопки громкости и поведение после паузы"></a><br><sub>Касания, кнопки громкости и поведение после паузы</sub></td>
</tr>
</table>
<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/speed-reading/rsvp-and-timing.jpg"><img src="docs/screenshots/speed-reading/rsvp-and-timing.jpg" width="280" alt="Механика RSVP и точные паузы"></a><br><sub>Механика RSVP и точные паузы</sub></td><td align="center" valign="top"><a href="docs/screenshots/speed-reading/acceleration-and-geometry.jpg"><img src="docs/screenshots/speed-reading/acceleration-and-geometry.jpg" width="280" alt="Плавный разгон и геометрия текста"></a><br><sub>Плавный разгон и геометрия текста</sub></td>
</tr>
</table>
<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/speed-reading/guide-and-focus.jpg"><img src="docs/screenshots/speed-reading/guide-and-focus.jpg" width="280" alt="Фиксация, направляющая и стиль подсветки"></a><br><sub>Фиксация, направляющая и стиль подсветки</sub></td><td align="center" valign="top"><a href="docs/screenshots/speed-reading/custom-colors.jpg"><img src="docs/screenshots/speed-reading/custom-colors.jpg" width="280" alt="Цвета текста, фокуса, фона и направляющей"></a><br><sub>Цвета текста, фокуса, фона и направляющей</sub></td>
</tr>
</table>


</details>

<a id="audio"></a>

## 07 · Голос и аудиокниги

### Озвучка текста

В Speed Reader можно включить голос и слушать книгу установленным **офлайн-голосом Android TTS**. Доступны выбор голоса или автовыбор, скорость речи, высота тона и переход к следующей главе.

Показ слов при озвучке связан с текущей позицией речи. Доступность языков и качество голоса зависят от установленного движка TTS и его локальных голосов. Отдельная нейросетевая голосовая модель в приложение не встроена.

<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/voice/offline-tts.jpg"><img src="docs/screenshots/voice/offline-tts.jpg" width="280" alt="Офлайн-голоса Android TTS, скорость и высота речи"></a><br><sub>Офлайн-голоса Android TTS, скорость и высота речи</sub></td>
</tr>
</table>


### Готовые аудиофайлы

Аудиокниги добавляются через **«+» → «Аудиокниги»** и доступны через фильтр **AUDIO**. Плеер поддерживает воспроизведение и паузу, перемотку по шкале, переход на 15 секунд назад/вперёд и сохранение позиции. При удалении записи из аудиобиблиотеки исходный файл на устройстве остаётся.

В присланной подборке есть меню импорта аудио и фильтр AUDIO; отдельного кадра аудиоплеера нет.

<a id="appearance"></a>

## 08 · Спокойный интерфейс или атмосферная оболочка

Обычный интерфейс можно оставить лаконичным или включить экспериментальную оболочку **Wasteland / Cyberpunk**. Оболочка и палитра выбираются отдельно: оформление меняется, а книги и позиции чтения сохраняются.

**Палитры:** OLED, Flux, Бумага, Сепия, Nord, Dracula, Solarized, Catppuccin, Gruvbox, Rosé Pine Moon, CMX Purple, Wasteland CRT, Cyberpunk, Ч/Б, Ч/Б мягкая и пользовательская **«Моя»**. Режим темы — системный, светлый или тёмный.

Для экспериментальных оболочек доступны **CRT-эффекты**: линии развёртки, свечение люминофора, статичный шум и затемнение краёв. Каждый эффект можно переключить отдельно; общая интенсивность регулируется ползунком. Есть и главный выключатель всех эффектов.

<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/appearance/cyberpunk-shell.jpg"><img src="docs/screenshots/appearance/cyberpunk-shell.jpg" width="280" alt="Оболочка Cyberpunk и выбор палитры"></a><br><sub>Оболочка Cyberpunk и выбор палитры</sub></td><td align="center" valign="top"><a href="docs/screenshots/appearance/crt-effects.jpg"><img src="docs/screenshots/appearance/crt-effects.jpg" width="280" alt="Линии развёртки, свечение, шум и затемнение краёв"></a><br><sub>Линии развёртки, свечение, шум и затемнение краёв</sub></td>
</tr>
</table>


<details>
<summary><strong>Как Wasteland выглядит в библиотеке и Speed Reader</strong></summary>

<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/library/wasteland-list.jpg"><img src="docs/screenshots/library/wasteland-list.jpg" width="250" alt="Wasteland: библиотека списком"></a><br><sub>Wasteland: библиотека списком</sub></td><td align="center" valign="top"><a href="docs/screenshots/library/wasteland-grid.jpg"><img src="docs/screenshots/library/wasteland-grid.jpg" width="250" alt="Wasteland: крупные обложки"></a><br><sub>Wasteland: крупные обложки</sub></td><td align="center" valign="top"><a href="docs/screenshots/library/wasteland-compact.jpg"><img src="docs/screenshots/library/wasteland-compact.jpg" width="250" alt="Wasteland: компактная сетка"></a><br><sub>Wasteland: компактная сетка</sub></td>
</tr>
</table>
<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/speed-reading/wasteland-chapter-intro.jpg"><img src="docs/screenshots/speed-reading/wasteland-chapter-intro.jpg" width="250" alt="Wasteland: заставка главы и управление"></a><br><sub>Wasteland: заставка главы и управление</sub></td><td align="center" valign="top"><a href="docs/screenshots/speed-reading/wasteland-context.jpg"><img src="docs/screenshots/speed-reading/wasteland-context.jpg" width="250" alt="Wasteland: контекст в полном экране"></a><br><sub>Wasteland: контекст в полном экране</sub></td><td align="center" valign="top"><a href="docs/screenshots/speed-reading/wasteland-playback.jpg"><img src="docs/screenshots/speed-reading/wasteland-playback.jpg" width="250" alt="Wasteland: RSVP во время воспроизведения"></a><br><sub>Wasteland: RSVP во время воспроизведения</sub></td>
</tr>
</table>


</details>

<a id="pdf"></a>

## 09 · PDF и распознавание сканов

PDF открывается как документ со страницами, масштабированием и сохранением позиции. Если в нём есть текстовый слой, его можно использовать для поиска и скорочтения. Для сканированных страниц предусмотрено локальное OCR — распознавание текста на устройстве.

- **Tesseract:** русский, украинский и английский при наличии соответствующих языковых моделей в сборке.
- **ML Kit Latin:** резервное распознавание латинского текста без дополнительных моделей Tesseract.
- OCR запускается для страниц без встроенного текста и управляется отдельным переключателем.

**В опубликованной alpha.95 языковые модели Tesseract не вложены.** Поэтому наличие переключателей rus/ukr/eng не означает, что все три языка готовы к распознаванию. Для полноценного OCR этих языков нужна сборка с моделями. На скриншоте показаны настройки OCR; отдельного кадра открытого PDF нет.

<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/pdf-ocr/ocr-preferences.jpg"><img src="docs/screenshots/pdf-ocr/ocr-preferences.jpg" width="280" alt="PDF OCR: языки Tesseract и ML Kit Latin"></a><br><sub>PDF OCR: языки Tesseract и ML Kit Latin</sub></td>
</tr>
</table>


<a id="updates"></a>

## 10 · Установка и обновления

1. Откройте [релиз 0.2.0-alpha.95](https://github.com/nrevis/FluxReader/releases/tag/v0.2.0-alpha.95).
2. Скачайте **FluxReader-0.2.0-alpha.95-release.apk** и откройте его на Android 9 или новее.
3. Разрешите установку для приложения, из которого открываете APK, и подтвердите действие в системном установщике.

APK alpha.95 сохраняет сертификат предыдущих локальных alpha-сборок и подходит для обновления поверх alpha.92/alpha.93/alpha.94.

В **«Настройки → О программе → Обновления»** добавлены ручная проверка GitHub Releases, необязательная проверка раз в сутки, выбор alpha-канала, скачивание APK с прогрессом и переход к системной установке. Перед установкой проверяются контрольная сумма SHA-256, имя пакета, номер версии и сертификат.

> **Тестовые версии:** текущие выпуски помечены как alpha. Чтобы получать их, включите «Включая тестовые alpha-релизы» и нажмите «Проверить». Репозиторий публичный, вход в GitHub для обновлений не требуется. Скриншот раздела обновлений в подборке отсутствует.

Для самостоятельной сборки см. [инструкцию разработчика](DEVELOPMENT.md). Подробности формата обновлений — в [RELEASE-UPDATES.md](RELEASE-UPDATES.md).

---

<a id="gallery"></a>

## Галерея по функциям

В обзор отобраны ключевые кадры. Ниже сохранены **все 52 скриншота**, включая альтернативные виды и настройки; каждый можно открыть в полном размере. Номера исходных фото и расположение файлов приведены в [индексе скриншотов](docs/SCREENSHOTS.md).

<details>
<summary><strong>Библиотека, импорт и организация · 15 кадров</strong></summary>

<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/library/folders-compact.jpg"><img src="docs/screenshots/library/folders-compact.jpg" width="230" alt="Папки библиотеки: компактная сетка"></a><br><sub>Папки библиотеки: компактная сетка</sub></td><td align="center" valign="top"><a href="docs/screenshots/library/folders-list.jpg"><img src="docs/screenshots/library/folders-list.jpg" width="230" alt="Папки библиотеки: список"></a><br><sub>Папки библиотеки: список</sub></td><td align="center" valign="top"><a href="docs/screenshots/library/folders-grid.jpg"><img src="docs/screenshots/library/folders-grid.jpg" width="230" alt="Папки библиотеки: крупная сетка"></a><br><sub>Папки библиотеки: крупная сетка</sub></td>
</tr>
</table>
<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/library/selection-actions.jpg"><img src="docs/screenshots/library/selection-actions.jpg" width="230" alt="Меню выбранных книг и пакетные действия"></a><br><sub>Меню выбранных книг и пакетные действия</sub></td><td align="center" valign="top"><a href="docs/screenshots/library/books-selection.jpg"><img src="docs/screenshots/library/books-selection.jpg" width="230" alt="Обложки книг внутри папки и режим выбора"></a><br><sub>Обложки книг внутри папки и режим выбора</sub></td><td align="center" valign="top"><a href="docs/screenshots/library/sort-and-group.jpg"><img src="docs/screenshots/library/sort-and-group.jpg" width="230" alt="Сортировка и группировка библиотеки"></a><br><sub>Сортировка и группировка библиотеки</sub></td>
</tr>
</table>
<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/library/book-metadata.jpg"><img src="docs/screenshots/library/book-metadata.jpg" width="230" alt="Редактирование метаданных книги"></a><br><sub>Редактирование метаданных книги</sub></td><td align="center" valign="top"><a href="docs/screenshots/library/books-compact.jpg"><img src="docs/screenshots/library/books-compact.jpg" width="230" alt="Книги: компактная сетка"></a><br><sub>Книги: компактная сетка</sub></td><td align="center" valign="top"><a href="docs/screenshots/library/books-list.jpg"><img src="docs/screenshots/library/books-list.jpg" width="230" alt="Книги: список с автором, серией и прогрессом"></a><br><sub>Книги: список с автором, серией и прогрессом</sub></td>
</tr>
</table>
<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/library/import-menu.jpg"><img src="docs/screenshots/library/import-menu.jpg" width="230" alt="Импорт книг, ZIP, аудио и папок"></a><br><sub>Импорт книг, ZIP, аудио и папок</sub></td><td align="center" valign="top"><a href="docs/screenshots/library/wasteland-list.jpg"><img src="docs/screenshots/library/wasteland-list.jpg" width="230" alt="Wasteland: библиотека списком"></a><br><sub>Wasteland: библиотека списком</sub></td><td align="center" valign="top"><a href="docs/screenshots/library/wasteland-import.jpg"><img src="docs/screenshots/library/wasteland-import.jpg" width="230" alt="Wasteland: меню импорта"></a><br><sub>Wasteland: меню импорта</sub></td>
</tr>
</table>
<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/library/library-preferences.jpg"><img src="docs/screenshots/library/library-preferences.jpg" width="230" alt="Настройки вида, группировки и обложек"></a><br><sub>Настройки вида, группировки и обложек</sub></td><td align="center" valign="top"><a href="docs/screenshots/library/wasteland-grid.jpg"><img src="docs/screenshots/library/wasteland-grid.jpg" width="230" alt="Wasteland: крупные обложки"></a><br><sub>Wasteland: крупные обложки</sub></td><td align="center" valign="top"><a href="docs/screenshots/library/wasteland-compact.jpg"><img src="docs/screenshots/library/wasteland-compact.jpg" width="230" alt="Wasteland: компактная сетка"></a><br><sub>Wasteland: компактная сетка</sub></td>
</tr>
</table>

</details>

<details>
<summary><strong>Чтение, навигация, текст и заметки · 16 кадров</strong></summary>

<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/reading/reader-controls.jpg"><img src="docs/screenshots/reading/reader-controls.jpg" width="230" alt="Чтение с навигацией и прогрессом"></a><br><sub>Чтение с навигацией и прогрессом</sub></td><td align="center" valign="top"><a href="docs/screenshots/reading/reader-immersive.jpg"><img src="docs/screenshots/reading/reader-immersive.jpg" width="230" alt="Чтение в полном экране"></a><br><sub>Чтение в полном экране</sub></td><td align="center" valign="top"><a href="docs/screenshots/reading/reader-menu.jpg"><img src="docs/screenshots/reading/reader-menu.jpg" width="230" alt="Меню: оглавление, поиск, заметки и параметры текста"></a><br><sub>Меню: оглавление, поиск, заметки и параметры текста</sub></td>
</tr>
</table>
<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/reading/table-of-contents.jpg"><img src="docs/screenshots/reading/table-of-contents.jpg" width="230" alt="Оглавление и переход к главам"></a><br><sub>Оглавление и переход к главам</sub></td><td align="center" valign="top"><a href="docs/screenshots/reading/book-search.jpg"><img src="docs/screenshots/reading/book-search.jpg" width="230" alt="Поиск по тексту книги"></a><br><sub>Поиск по тексту книги</sub></td><td align="center" valign="top"><a href="docs/screenshots/reading/annotations-workspace.jpg"><img src="docs/screenshots/reading/annotations-workspace.jpg" width="230" alt="Заметки, выделения, цитаты и поиск по записям"></a><br><sub>Заметки, выделения, цитаты и поиск по записям</sub></td>
</tr>
</table>
<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/reading/fonts-and-profiles.jpg"><img src="docs/screenshots/reading/fonts-and-profiles.jpg" width="230" alt="Профили чтения, шрифты и импорт TTF/OTF"></a><br><sub>Профили чтения, шрифты и импорт TTF/OTF</sub></td><td align="center" valign="top"><a href="docs/screenshots/reading/margins-and-theme.jpg"><img src="docs/screenshots/reading/margins-and-theme.jpg" width="230" alt="Поля, режим темы и цветовые схемы"></a><br><sub>Поля, режим темы и цветовые схемы</sub></td><td align="center" valign="top"><a href="docs/screenshots/reading/palettes-and-navigation.jpg"><img src="docs/screenshots/reading/palettes-and-navigation.jpg" width="230" alt="Палитры и способы навигации"></a><br><sub>Палитры и способы навигации</sub></td>
</tr>
</table>
<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/reading/tap-zones-and-animation.jpg"><img src="docs/screenshots/reading/tap-zones-and-animation.jpg" width="230" alt="Зоны касания и анимации страниц"></a><br><sub>Зоны касания и анимации страниц</sub></td><td align="center" valign="top"><a href="docs/screenshots/reading/paragraphs-and-headings.jpg"><img src="docs/screenshots/reading/paragraphs-and-headings.jpg" width="230" alt="Переносы, абзацы и оформление заголовков"></a><br><sub>Переносы, абзацы и оформление заголовков</sub></td><td align="center" valign="top"><a href="docs/screenshots/reading/reader-next-page.jpg"><img src="docs/screenshots/reading/reader-next-page.jpg" width="230" alt="Следующая страница в полном экране"></a><br><sub>Следующая страница в полном экране</sub></td>
</tr>
</table>
<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/reading/wasteland-reader-menu.jpg"><img src="docs/screenshots/reading/wasteland-reader-menu.jpg" width="230" alt="Wasteland: меню чтения и экранных эффектов"></a><br><sub>Wasteland: меню чтения и экранных эффектов</sub></td><td align="center" valign="top"><a href="docs/screenshots/reading/wasteland-immersive-alt.jpg"><img src="docs/screenshots/reading/wasteland-immersive-alt.jpg" width="230" alt="Wasteland: полноэкранная страница другой книги"></a><br><sub>Wasteland: полноэкранная страница другой книги</sub></td><td align="center" valign="top"><a href="docs/screenshots/reading/wasteland-navigation.jpg"><img src="docs/screenshots/reading/wasteland-navigation.jpg" width="230" alt="Wasteland: профили и настройки навигации"></a><br><sub>Wasteland: профили и настройки навигации</sub></td>
</tr>
</table>
<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/reading/wasteland-immersive.jpg"><img src="docs/screenshots/reading/wasteland-immersive.jpg" width="230" alt="Wasteland: полноэкранное чтение"></a><br><sub>Wasteland: полноэкранное чтение</sub></td>
</tr>
</table>

</details>

<details>
<summary><strong>Скорочтение и его настройки · 17 кадров</strong></summary>

<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/speed-reading/chapter-intro.jpg"><img src="docs/screenshots/speed-reading/chapter-intro.jpg" width="230" alt="Заставка новой главы в Speed Reader"></a><br><sub>Заставка новой главы в Speed Reader</sub></td><td align="center" valign="top"><a href="docs/screenshots/speed-reading/word-and-context.jpg"><img src="docs/screenshots/speed-reading/word-and-context.jpg" width="230" alt="Точка фиксации и контекст на паузе"></a><br><sub>Точка фиксации и контекст на паузе</sub></td><td align="center" valign="top"><a href="docs/screenshots/speed-reading/immersive-context.jpg"><img src="docs/screenshots/speed-reading/immersive-context.jpg" width="230" alt="Полный экран с контекстом и компактным управлением"></a><br><sub>Полный экран с контекстом и компактным управлением</sub></td>
</tr>
</table>
<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/speed-reading/immersive-playback.jpg"><img src="docs/screenshots/speed-reading/immersive-playback.jpg" width="230" alt="Полноэкранное скорочтение во время воспроизведения"></a><br><sub>Полноэкранное скорочтение во время воспроизведения</sub></td><td align="center" valign="top"><a href="docs/screenshots/speed-reading/settings-hub.jpg"><img src="docs/screenshots/speed-reading/settings-hub.jpg" width="230" alt="Разделы настройки скорочтения"></a><br><sub>Разделы настройки скорочтения</sub></td><td align="center" valign="top"><a href="docs/screenshots/speed-reading/display-preferences.jpg"><img src="docs/screenshots/speed-reading/display-preferences.jpg" width="230" alt="Шрифт, ORP, следующие слова и прогресс"></a><br><sub>Шрифт, ORP, следующие слова и прогресс</sub></td>
</tr>
</table>
<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/speed-reading/behavior-preferences.jpg"><img src="docs/screenshots/speed-reading/behavior-preferences.jpg" width="230" alt="Касания, кнопки громкости и поведение после паузы"></a><br><sub>Касания, кнопки громкости и поведение после паузы</sub></td><td align="center" valign="top"><a href="docs/screenshots/speed-reading/rsvp-and-timing.jpg"><img src="docs/screenshots/speed-reading/rsvp-and-timing.jpg" width="230" alt="Механика RSVP и точные паузы"></a><br><sub>Механика RSVP и точные паузы</sub></td><td align="center" valign="top"><a href="docs/screenshots/speed-reading/acceleration-and-geometry.jpg"><img src="docs/screenshots/speed-reading/acceleration-and-geometry.jpg" width="230" alt="Плавный разгон и геометрия текста"></a><br><sub>Плавный разгон и геометрия текста</sub></td>
</tr>
</table>
<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/speed-reading/guide-and-focus.jpg"><img src="docs/screenshots/speed-reading/guide-and-focus.jpg" width="230" alt="Фиксация, направляющая и стиль подсветки"></a><br><sub>Фиксация, направляющая и стиль подсветки</sub></td><td align="center" valign="top"><a href="docs/screenshots/speed-reading/custom-colors.jpg"><img src="docs/screenshots/speed-reading/custom-colors.jpg" width="230" alt="Цвета текста, фокуса, фона и направляющей"></a><br><sub>Цвета текста, фокуса, фона и направляющей</sub></td><td align="center" valign="top"><a href="docs/screenshots/speed-reading/wasteland-prologue.jpg"><img src="docs/screenshots/speed-reading/wasteland-prologue.jpg" width="230" alt="Wasteland: скорочтение пролога"></a><br><sub>Wasteland: скорочтение пролога</sub></td>
</tr>
</table>
<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/speed-reading/speed-and-pacing.jpg"><img src="docs/screenshots/speed-reading/speed-and-pacing.jpg" width="230" alt="Режимы, профили темпа, WPM и слов за фиксацию"></a><br><sub>Режимы, профили темпа, WPM и слов за фиксацию</sub></td><td align="center" valign="top"><a href="docs/screenshots/speed-reading/wasteland-chapter-intro.jpg"><img src="docs/screenshots/speed-reading/wasteland-chapter-intro.jpg" width="230" alt="Wasteland: заставка главы и управление"></a><br><sub>Wasteland: заставка главы и управление</sub></td><td align="center" valign="top"><a href="docs/screenshots/speed-reading/wasteland-context.jpg"><img src="docs/screenshots/speed-reading/wasteland-context.jpg" width="230" alt="Wasteland: контекст в полном экране"></a><br><sub>Wasteland: контекст в полном экране</sub></td>
</tr>
</table>
<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/speed-reading/wasteland-playback.jpg"><img src="docs/screenshots/speed-reading/wasteland-playback.jpg" width="230" alt="Wasteland: RSVP во время воспроизведения"></a><br><sub>Wasteland: RSVP во время воспроизведения</sub></td><td align="center" valign="top"><a href="docs/screenshots/speed-reading/wasteland-settings-hub.jpg"><img src="docs/screenshots/speed-reading/wasteland-settings-hub.jpg" width="230" alt="Wasteland: настройки Speed Reader"></a><br><sub>Wasteland: настройки Speed Reader</sub></td>
</tr>
</table>

</details>

<details>
<summary><strong>Голос и озвучка · 1 кадр</strong></summary>

<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/voice/offline-tts.jpg"><img src="docs/screenshots/voice/offline-tts.jpg" width="230" alt="Офлайн-голоса Android TTS, скорость и высота речи"></a><br><sub>Офлайн-голоса Android TTS, скорость и высота речи</sub></td>
</tr>
</table>

</details>

<details>
<summary><strong>Оформление и экранные эффекты · 2 кадра</strong></summary>

<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/appearance/cyberpunk-shell.jpg"><img src="docs/screenshots/appearance/cyberpunk-shell.jpg" width="230" alt="Оболочка Cyberpunk и выбор палитры"></a><br><sub>Оболочка Cyberpunk и выбор палитры</sub></td><td align="center" valign="top"><a href="docs/screenshots/appearance/crt-effects.jpg"><img src="docs/screenshots/appearance/crt-effects.jpg" width="230" alt="Линии развёртки, свечение, шум и затемнение краёв"></a><br><sub>Линии развёртки, свечение, шум и затемнение краёв</sub></td>
</tr>
</table>

</details>

<details>
<summary><strong>PDF и OCR · 1 кадр</strong></summary>

<table>
<tr>
<td align="center" valign="top"><a href="docs/screenshots/pdf-ocr/ocr-preferences.jpg"><img src="docs/screenshots/pdf-ocr/ocr-preferences.jpg" width="230" alt="PDF OCR: языки Tesseract и ML Kit Latin"></a><br><sub>PDF OCR: языки Tesseract и ML Kit Latin</sub></td>
</tr>
</table>

</details>


---

<div align="center">

**Flux Reader · n.revis**

[Релизы](https://github.com/nrevis/FluxReader/releases) · [Исходники](https://github.com/nrevis/FluxReader)

<sub>Описание сверено с исходниками 0.2.0-alpha.95. Скриншоты предоставлены автором; выбранные на них значения и содержимое библиотеки служат примерами.</sub>

</div>
