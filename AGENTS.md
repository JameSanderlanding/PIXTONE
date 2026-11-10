# AGENTS.md

Руководство для разработчиков и AI-агентов, которые работают с репозиторием **PIXTONE**.

---

## 0. Жёсткие правила

1. **Не хранить токены GitHub в репозитории, `.git/config` или файлах проекта.**
   Если токен нужен для пуша — использовать временный файл, а после операции удалять его и рекомендовать владельцу отозвать токен.
2. **Не переименовывать package/applicationId/базу/SharedPreferences** без явного запроса. Внутренние имена остались историческими:
   - package / applicationId: `com.vova.glyph`
   - DB: `glyph.db`
   - prefs: `glyph_prefs`, `glyph_theme`, `glyph_fonts`, `glyph_language`, `genius_gate`
   - отображаемое имя приложения: **PIXTONE**
3. **Пользовательские данные не переводить.** Названия треков, плейлистов и имена артистов должны отображаться как есть.

---

## 1. Что это за проект

**PIXTONE** — локальный музыкальный плеер для Android в пиксельной чёрно-белой эстетике.

Ключевые принципы:
- музыка только с устройства, без облака и аккаунтов;
- никакой телеметрии, рекламы и трекеров;
- минималистичный кастомный UI на Jetpack Compose;
- полноценная кастомизация: темы, палитра, шрифты;
- шесть языков интерфейса.

### Технические факты

| Параметр | Значение |
|---|---|
| Язык | Kotlin 2.2 |
| UI | Jetpack Compose, Material 3 |
| Плеер | AndroidX Media3 ExoPlayer + MediaSessionService |
| Async | Kotlin Coroutines, StateFlow / SharedFlow |
| БД | SQLiteOpenHelper, схема `glyph.db` версии 12 |
| Сеть | OkHttp, org.json, Jsoup |
| Картинки | Coil |
| Сборка | Gradle 9.6, AGP 9.4.1, JDK 21 |
| SDK | minSdk 26, targetSdk 37, compileSdk 37 |
| versionCode / versionName | 2 / 1.1.0 |
| Лицензия | MIT |

---

## 2. Структура репозитория

```
PIXTONE/
├── AGENTS.md                     ← этот файл
├── README.md                     ← публичное описание проекта
├── LICENSE                       ← MIT
├── build.gradle.kts              ← корневые плагины
├── settings.gradle.kts           ← имя проекта и модули
├── gradle.properties             ← настройки Gradle (без локальных путей)
├── gradle/wrapper/               ← Gradle wrapper
├── app/
│   ├── build.gradle.kts          ← конфиг модуля, зависимости, имя APK
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/licenses/      ← OFL / Apache / MIT / notices
│       ├── res/
│       │   ├── font/             ← doto.ttf, pixelify_sans.ttf, space_grotesk.ttf
│       │   ├── drawable/         ← иконки и векторные ассеты
│       │   ├── mipmap-anydpi-v26/← adaptive launcher icon
│       │   └── values/           ← строки, тема, цвета иконки
│       └── java/com/vova/glyph/
│           ├── MainActivity.kt
│           ├── MainViewModel.kt
│           ├── GlyphApp.kt
│           ├── AppContainer.kt
│           ├── data/
│           ├── playback/
│           ├── i18n/
│           └── ui/
└── docs/screenshots/             ← скриншоты для README (`*_en.png`)
```

### Git-ветки

| Ветка | Назначение |
|---|---|
| `mobile` | **ветка по умолчанию**, весь Android-проект |
| `windows` | пустая ветка-заглушка под будущий порт |
| `linux` | пустая ветка-заглушка под будущий порт |
| `main` | **удалена**; не воссоздавать без просьбы |

`windows` и `linux` содержат только пустой корневой коммит. Не добавлять туда файлы без отдельной задачи.

---

## 3. Карта кода

### Точка входа

| Файл | Ответственность |
|---|---|
| `MainActivity.kt` | единственная Activity, edge-to-edge, `setContent { GlyphTheme { GlyphRoot(vm) } }` |
| `GlyphApp.kt` | `Application`: инициализация `ThemeController`, `FontController`, `LangController`, создание `AppContainer` |
| `AppContainer.kt` | ручной DI: prefs, db, repository, scanner, player, geniusGate, lyrics/cover repositories, appScope |
| `MainViewModel.kt` | состояние приложения, сканирование, плейлисты, избранное, обложки, тексты, навигация |

### `data/`

| Файл | Что делает |
|---|---|
| `Models.kt` | `Track`, `Playlist`, `displayArtist` |
| `MusicRepository.kt` | обёртка над БД + StateFlow: tracks/favorites/playlists; refresh, favorites, playlists |
| `GlyphDb.kt` | SQLiteOpenHelper, схема и все SQL-запросы |
| `MusicScanner.kt` | обход папки через SAF (`DocumentFile`), параллельное чтение тегов и встроенных обложек |
| `AudioFormats.kt` | список поддерживаемых аудио-расширений и проверка по имени файла |
| `ArtworkStore.kt` | сохранение/удаление обложек, `needsRemoteCover`, `hasArtwork` |
| `CoverRepository.kt` | поиск обложек: Genius → Deezer → iTunes |
| `LyricsRepository.kt` | поиск текстов: LRCLIB → lyrics.ovh → Genius; парсинг LRC |
| `GeniusGate.kt` | общий rate limiter для Genius (пауза после 429/403, интервал между запросами) |
| `Prefs.kt` | SharedPreferences `glyph_prefs` |
| `QueryText.kt` | нормализация и транслитерация поисковых запросов |
| `Lyrics.kt` | `LyricsResult`, `LyricLine`, `CachedLyrics`, парсинг LRC, gap-маркеры |

### `playback/`

| Файл | Что делает |
|---|---|
| `PlaybackService.kt` | `MediaSessionService`, ExoPlayer, session activity, ленивое создание audio-эффектов, команда `QUERY_DSP` |
| `PlayerConnection.kt` | подключение к сервису, очередь, shuffle, repeat, состояние `PlayerUiState`, прогресс `PlayerProgress`, `completionCount`, `DspUiState` и запись `fx_*` в prefs |
| `AudioEffects.kt` | системные `Equalizer`, `BassBoost`, `LoudnessEnhancer` на audio-session плеера, снимок возможностей для UI |
| `Dsp.kt` | `DspUiState`, формат команды `QUERY_DSP`, `Bundle`-сериализация, кодирование полос EQ |
| `dsp/Freeverb.kt` | чистый Freeverb: 8 комбов и 4 оллпасса на канал, параметры через `ReverbSettings` |
| `dsp/ReverbAudioProcessor.kt` | `AudioProcessor` с Freeverb, пропускает PCM байт-в-байт при выключении |
| `dsp/DspRenderersFactory.kt` | `DefaultRenderersFactory` с `DefaultAudioSink` и собственной цепочкой процессоров |
| `dsp/EqCurve.kt` | пресеты эквалайзера (`FLAT`, `BASS`, `VOCAL`, `ROCK`) с поправкой на скорость |

### `i18n/`

| Файл | Что делает |
|---|---|
| `Language.kt` | `AppLanguage`, `LangController`, `tr()` |
| `Translations.kt` | словарь русских строк → 5 языков + regex-правила для динамических строк |

### `ui/`

| Файл | Что делает |
|---|---|
| `GlyphRoot.kt` | корневая навигация, вкладки, оверлеи, `GlyphMessageHost` |
| `theme/Theme.kt` | цвета из палитры, типографика из выбранных шрифтов, `GlyphTheme` |
| `theme/Palette.kt` | палитра, пресеты, `ThemeController`, пользовательские пресеты, draft create/edit |
| `theme/Fonts.kt` | доступные шрифты, `FontController`, роли шрифтов |
| `components/LocalizedText.kt` | **кастомный `Text`**, который прогоняет строку через `tr()` |
| `components/MessageHost.kt` | очередь всплывающих сообщений, pixel-dissolve, закрытие по тапу |
| `components/Common.kt` | дизайн-система: кнопки, заголовки, обложки, lower nav, EmptyState и т.д. |
| `components/Dialogs.kt` | диалоги создания плейлиста, подтверждения, текста песни |
| `components/ColorPickerDialog.kt` | RGB-слайдеры + HEX-ввод |
| `components/LicenseTextDialog.kt` | просмотр текста лицензии из assets |
| `components/GlyphIcons.kt` | векторные иконки `Glyph.*` (Canvas) |
| `screens/LibraryScreen.kt` | библиотека, поиск, плейлисты, сканирование |
| `screens/AlbumsScreen.kt` | сетка альбомов, собранных из тегов |
| `screens/AlbumDetailScreen.kt` | треки альбома, удаление альбома из приложения |
| `screens/FavoritesScreen.kt` | избранное |
| `screens/PlayerOverlay.kt` | полноэкранный плеер, кнопка и секции панели ЗВУК |
| `screens/PlayerSoundPanel.kt` | раскрывающиеся настройки звука в плеере: скорость, реверб, бас, громкость, эквалайзер |
| `screens/PlaylistDetailScreen.kt` | плейлист, добавление/удаление треков |
| `screens/SettingsScreen.kt` | настройки: музыка, тема, шрифты, язык, лицензии |
| `screens/OnboardingScreen.kt` | первый запуск и выбор папки |

---

## 4. Как приложение работает

### 4.1 Запуск

1. `GlyphApp.onCreate()`:
   - `ThemeController.attach()` — загружает палитру/пресет из `glyph_theme`;
   - `FontController.attach()` — шрифты из `glyph_fonts`;
   - `LangController.attach()` — язык из `glyph_language`;
   - создаёт `AppContainer`, запускает `repository.refresh()` и `player.connect()`.
2. `MainActivity` открывает `GlyphTheme`, который читает палитру и шрифты как Compose-состояние.
3. `GlyphRoot` решает, показать онбординг или основной интерфейс.
4. `MainViewModel` подписан на `StateFlow` репозитория и плеера.

### 4.2 Сканирование папки

1. Пользователь выбирает папку через SAF (`OpenDocumentTree`), URI сохраняется в `Prefs`.
2. `MusicScanner.scan()` рекурсивно обходит `DocumentFile`, фильтрует аудио по расширениям (`AudioFormats`).
3. Метаданные новых файлов читаются параллельно: 4 воркера под `Semaphore`, каждый файл через свой `MediaMetadataRetriever` (title/artist/album/duration/`embeddedPicture`).
4. Встроенная обложка сохраняется через `ArtworkStore.saveEmbeddedArt()`.
5. Порядок треков в результате сохраняется порядком обхода, прогресс отдаётся в UI не чаще, чем раз в `PROGRESS_INTERVAL_MS`.
6. `MusicRepository.addLocalTracks()` делает upsert в таблицу `tracks`.
7. Затем `MainViewModel.refreshCovers()` подтягивает настоящие обложки для новых треков.
8. Во время сканирования UI показывает карточку со спиннером и счётчиком найденных треков.

Важно: при повторном сканировании существующие треки не перечитываются; добавляются только новые URI.

### 4.3 Воспроизведение

1. `MainViewModel.playTracks()` передаёт список и стартовый индекс в `PlayerConnection`.
2. `PlayerConnection` строит очередь `MediaItem` и управляет ExoPlayer через `MediaController`.
3. `PlaybackService` держит `ExoPlayer` и `MediaSession`, отдаёт метаданные и обложку в системный плеер.
4. Состояние разделено на два потока:
   - `PlayerUiState` (`state`) — структурное: трек, play/pause, buffering, очередь, repeat/shuffle, ошибки, `completionCount`. Меняется только от событий плеера.
   - `PlayerProgress` (`progress`) — позиция и длительность. Тикает раз в 500 мс во время игры и раз в 1 с на паузе, но **только пока есть подписчик** (`subscriptionCount`), поэтому в фоне таймер останавливается.
5. Высокочастотный `progress` читают только те композаблы, которым он нужен (`MiniPlayerBar`, `PlayerProgressBlock`, диалоги текста). Списки и навигация подписаны только на `state`, иначе они бы перерисовывались дважды в секунду.
6. Shuffle реализован вручную перестановкой очереди.
7. При завершении трека увеличивается `completionCount` и запоминается `completedTrackId` — на это опирается автозакрытие текста. Завершение определяется `STATE_ENDED` и детектом перескока позиции в начало (repeat/shuffle).
8. Перемотка: `PlayerConnection.seekTo()` пишет целевую позицию в `PlayerProgress` сразу (`pendingSeekMs`), чтобы ползунок не отскакивал назад, пока сессия подтверждает seek. UI-слайдер (`SegmentedProgress`) трёт seek-события до одного раза в 100 мс и держит локальную позицию пальца, пока идёт drag.

### 4.4 Тексты песен

1. `MainViewModel.fetchLyrics(track)` запускает `LyricsRepository.fetchLyrics()`.
2. Источники опрашиваются параллельно: LRCLIB (get + search), lyrics.ovh, Genius.
3. Сначала проверяется кэш в таблице `lyrics`.
4. Если найден обычный текст, даётся короткая фора на синхронизированный (`SYNC_GRACE_MS`).
5. `FIRST_RESULT_TIMEOUT_MS = 30 сек`.
6. При блокировке Genius (`429/403`) поиск ждёт снятия блокировки до 4 минут, затем повторяет.
7. Успешный/неуспешный результат кэшируется; неудача не блокирует повтор навсегда.
8. В плеере текст автоматически закрывается, когда трек закончился.
9. В диалоге есть кнопка «Повторить».

### 4.5 Обложки

1. `CoverRepository.downloadCover()` ищет по кандидатам (с транслитом).
2. Порядок источников: Genius → Deezer → iTunes.
3. `GeniusGate` ограничивает частоту запросов к Genius.
4. `ArtworkStore.saveNetworkArt()` сохраняет JPEG с именем `net_<source>_<hash>.jpg`.
5. `needsRemoteCover()` решает, нужна ли замена: нет картинки, встроенная картинка или запасной источник.
6. Ручное обновление обложек в настройках ищет только треки без реальной обложки.
7. Пока ищется текст, поиск обложек временно уступает Genius.

### 4.6 Альбомы

- Отдельной таблицы нет: альбомы вычисляются из `tracks.album` при каждом изменении библиотеки.
- `Albums.group(tracks)` группирует по паре «альбом + исполнитель» (регистр не важен).
- Треки без тега альбома попадают в один виртуальный альбом «Без альбома» (`Albums.UNKNOWN_KEY`), он всегда последний в списке.
- Обложка альбома — первая непустая обложка его треков; длительность — сумма длительностей.
- Вкладка `Tab.ALBUMS` показывает сетку (2 колонки); тап открывает `AlbumDetailScreen`.
- Удаление альбома (`MainViewModel.deleteAlbum`) убирает **только записи из приложения**:
  - `GlyphDb.deleteTracks()` в одной транзакции чистит `playlist_tracks`, `lyrics` и `tracks`;
  - файлы на устройстве не трогаются, повторный скан папки вернёт треки;
  - обложки удаляются через `ArtworkStore.deleteReplacedArt()`;
  - если играл трек из этого альбома, плеер останавливается.

### 4.7 Плейлисты

- Хранятся в `playlists` и `playlist_tracks`.
- `playlist_tracks.position` задаёт порядок.
- Создание, удаление, добавление и удаление треков идут через `MusicRepository`.
- Обложка плейлиста копируется в `files/covers/` через `ArtworkStore.copyPlaylistCover()`.
- Сообщение о добавлении конкретного трека в плейлист намеренно не показывается; показывается только «уже в плейлисте» и ошибки.

### 4.8 Темы и шрифты

- `ThemeController` — синглтон с Compose-состоянием:
  - `palette` (8 цветов);
  - `presetId`;
  - `customPresets` (список пользовательских пресетов, JSON в prefs).
- Встроенные пресеты: `PIXTONE`, `AMBER`, `ACID`, `ICE`, `VIOLET`, `MONO`.
- Пользовательский пресет можно создать, а затем **редактировать** (карандаш в списке пресетов) и **удалять** (корзина в списке пресетов, с подтверждением).
- Удаление выбранного пресета возвращает тему к `PIXTONE`; встроенные пресеты удалить нельзя.
- Логика черновика:
  - `startDraft()` — создание;
  - `startEdit(preset)` — редактирование;
  - `setDraftColor()` — live-preview;
  - `saveDraft(name)` — создаёт или обновляет пресет;
  - `cancelDraft()` — откат к теме до входа;
  - `deletePreset(preset)` — удаляет пользовательский пресет и сохраняет список.
- `FontController` хранит выбранный шрифт для трёх ролей: body, display, brand.
- Доступные шрифты: Doto, Pixelify Sans, Space Grotesk, системный.

### 4.9 Языки

- Языки: RU, EN, DE, FR, ZH, JA.
- Исходные строки в коде — **русские**.
- Кастомный `Text` из `ui/components/LocalizedText.kt` вызывает `tr(text)`.
- `tr()`:
  - для RU возвращает исходную строку;
  - строки без кириллицы не трогает (названия треков, бренды);
  - сначала ищет точное совпадение в словаре;
  - затем применяет regex-правила для динамических строк;
  - результат regex-прохода кэшируется в LRU (`ruleCache`, 512 записей), кэш сбрасывается в `attach()`/`select()`.
- Кастомный `Text` дополнительно помнит результат перевода через `remember(text, language)`, чтобы рекомпозиция строки не запускала перевод заново.
- `LangController.select()` сохраняет язык и мгновенно перерисовывает UI.

### 4.10 Сортировка списков треков

- Состояние — `TrackSorting` (`by: TrackSort`, `reverse`) в `MainViewModel`. Три независимых состояния по экранам (`SortTarget`): `librarySorting`, `albumSorting`, `playlistSorting`. Библиотека и плейлисты запоминаются между запусками (`glyph_prefs`: `sort_library`, `sort_playlist`, формат `CRITERION:0|1`), альбомы — только в памяти.
- Значения `TrackSort`: `TITLE` (по названию без учёта регистра), `ADDED` (по `added_at`), `MODIFIED` (по `file_mtime`), `TRACK_NO` (по номеру трека из тегов), `PLAYLIST` (порядок `playlist_tracks.position`).
- Значения по умолчанию: библиотека — `TITLE`, альбом — `TRACK_NO`, плейлист — `PLAYLIST`.
- `TrackSorter.apply()` — чистая функция сортировки, покрыта unit-тестами. При равных ключах порядок добивается по названию; треки с неизвестным номером или датой файла уходят в конец.
- UI: `SortRow` из `ui/components/Sorts.kt` — чипы критериев + «РЕВЕРС». В библиотеке строка живёт под полем поиска и скрывается вместе с ним, в альбоме и плейлисте — над sticky-заголовком треков.
- `file_mtime` (`DocumentFile.lastModified()`) и `track_no` (`METADATA_KEY_CD_TRACK_NUMBER`) заполняются при сканировании, для старых треков догоняются фоном (`startMetadataBackfill()`). Значение `0` = «ещё не проверяли», `-1` = «проверили, данных нет».

### 4.11 Звук и DSP

- Все настройки звука — в раскрывающейся панели ЗВУК в раскрытом плеере (кнопка `PlayerSoundSection`). Порядок секций: скорость, реверберация, бас, громкость, эквалайзер.
- Скорость: один слайдер в раскрытом плеере, диапазон `0.5…1.5`, шаг `0.05`. Применяется через `PlaybackParameters(speed, speed)`, поэтому тон едет вместе со скоростью. Значение живёт в `fx_speed` и применяется на `connect()`.
- Источник правды — `glyph_prefs` с ключами `fx_*`: UI пишет, `PlaybackService` слушает `OnSharedPreferenceChangeListener` (сильная ссылка в поле сервиса) и применяет эффекты в процессе-владельце `AudioTrack`.
- `Equalizer`, `BassBoost` и `LoudnessEnhancer` создаются лениво при `audioSessionId > 0` и пересоздаются по `AnalyticsListener.onAudioSessionIdChanged`. При `audioSessionId <= 0` эффекты не создаются вообще. Если конструктор бросил исключение или `hasControl()` вернул `false`, UI показывает «не поддерживается».
- Пресеты EQ считаются по центральным частотам полос устройства и гейнам из `bandLevelRange()`. При скорости τ полоса на выходе `f` соответствует исходной частоте `f / τ` — кривая пресета подстраивается при изменении скорости. Ручная правка полосы переводит пресет в `CUSTOM` и сохраняет уровни в `fx_eq_bands`.
- Возможности устройства UI получает сессионной командой `QUERY_DSP` через `MediaSession.Callback.onCustomCommand`; команда добавляется в `onConnect`. Ответ — `Bundle`: число полос, центральные частоты, диапазон гейнов, текущие значения и поддержка bass/loudness.
- Реверб — свой `AudioProcessor` на `BaseAudioProcessor`, стоит до Sonic, поэтому хвост растягивается вместе со скоростью. `roomSize`, `damp` и `wet` — `@Volatile` и читаются на каждом буфере; `onFlush` и `onReset` чистят линии задержки после seek. При выключенном ревербе PCM проходит байт-в-байт.
- При скорости ≠ 1.0 Media3 отключает audio offload сам — на слабых устройствах растёт расход батареи.

### 4.12 Сообщения

- `MainViewModel.notify()` эмитит строку в `SharedFlow`.
- `GlyphMessageHost` показывает сообщения по одному.
- Анимация: fade-in → пауза → пиксельное растворение.
- Тап по сообщению закрывает его мгновенно.
- Очередь сохраняется.

---

## 5. Данные и хранилища

### SQLite (`glyph.db`, версия 12)

| Таблица | Назначение |
|---|---|
| `tracks` | треки: title, artist, album, genre, duration, uri, artwork, favorite, source, source_id, added_at, file_size, file_mtime, track_no |
| `playlists` | плейлисты: name, description, cover, created_at |
| `playlist_tracks` | связь плейлист↔трек + position |
| `lyrics` | кэш текстов: plain, synced, found, fetched_at |

### SharedPreferences

| Файл | Ключи |
|---|---|
| `glyph_prefs` | `onboarding_done`, `folder_uri`, `folder_name`, `shuffle`, `sort_library`, `sort_playlist`, `hidden_albums`, `fx_speed`, `fx_eq_enabled`, `fx_eq_preset`, `fx_eq_bands`, `fx_bass_enabled`, `fx_bass_strength`, `fx_loudness_enabled`, `fx_loudness_gain`, `fx_reverb_enabled`, `fx_reverb_room`, `fx_reverb_damp`, `fx_reverb_wet` |
| `glyph_theme` | `preset`, 8 цветов, `custom_presets` (JSON) |
| `glyph_fonts` | `body`, `display`, `brand` |
| `glyph_language` | `language` |
| `genius_gate` | `blocked_until`, `last_request_at` |

### Файлы приложения

- `files/art/` — обложки треков (`embedded_*`, `net_*`);
- `files/covers/` — обложки плейлистов.

---

## 6. UI и навигация

- Одна Activity, навигация — это состояние в `MainViewModel`:
  - `Tab.LIBRARY` / `Tab.ALBUMS` / `Tab.FAVORITES`;
  - `showPlayer`;
  - `showSettings`;
  - `openPlaylistId`;
  - `openAlbumKey`;
  - `onboardingVisible`.
- `GlyphRoot` собирает экран из этих состояний; Navigation Compose не используется.
- Настройки — это внутренние страницы `SettingsPage`: MAIN, THEME, THEME_EDITOR, FONTS, LANGUAGE, LICENSES. Настройки звука живут не здесь, а в раскрывающейся панели ЗВУК в `PlayerOverlay`.
- Системная «назад» обрабатывается через `BackHandler` в `GlyphRoot` и `SettingsScreen`.

### Важно про `Text`

В UI-файлах импортируется **не** `androidx.compose.material3.Text`, а
`com.vova.glyph.ui.components.Text` (для файлов в пакете `ui.components` — просто `Text`).
Этот composable переводит строку. Если добавить обычный material `Text`, строка не будет переведена.

`MicroLabel`, `GlyphButton`, `ScreenHeader`, `EmptyState` **переводят текст в верхний регистр до перевода**. Поэтому для строк, отображаемых ими, в словаре нужен ключ в верхнем регистре (иногда и обычный вариант).

---

## 7. Как добавлять изменения

### Новая строка интерфейса

1. Использовать русскую строку в коде (как остальные).
2. Добавить её в `Translations.kt` в `DICT`:
   ```kotlin
   "НОВАЯ СТРОКА" to t("NEW STRING", "NEUE ZEILE", "NOUVELLE LIGNE", "新字符串", "新しい文字列"),
   ```
3. Если строка отображается через `MicroLabel`/`GlyphButton`/`EmptyState`/`ScreenHeader` — ключ должен быть в UPPERCASE.
4. Для динамических строк добавить `Rule` с regex и `%1`, `%2`.
5. Проверить английскую версию вручную.

### Новый экран

1. Создать composable в `ui/screens/`.
2. Добавить состояние/навигацию в `MainViewModel` или локально.
3. Подключить в `GlyphRoot` или `SettingsScreen`.
4. Использовать компоненты из `ui/components` и кастомный `Text`.
5. Не забыть `BackHandler`, если экран открывается поверх.

### Новый пресет темы

1. Добавить `ThemePreset` в `ThemePresets` в `Palette.kt`.
2. Убедиться, что тема читается через `Glyph*`-цвета.
3. Проверить контраст и читаемость на всех экранах.

### Новый шрифт

1. Положить `.ttf` в `res/font/`.
2. Добавить `FontFamily` и `AppFont` в `Fonts.kt`.
3. Добавить лицензию в `assets/licenses/`.
4. Проверить, что шрифт поддерживает кириллицу и китайский/японский (иначе использовать системный fallback).

### Новый язык

1. Добавить `AppLanguage` в `Language.kt`.
2. Добавить колонку перевода в `Tr` и все `t(...)` вызовы — это большой объём.
3. Обновить `Rule`-строки.
4. Проверить экраны с длинными строками.

### Изменение release-процесса

- APK называется автоматически: `Pixtone-<versionName>.apk` (правило в `app/build.gradle.kts`, `androidComponents`).
- После изменения `versionName` APK переименуется сам.
- Релиз создаётся через GitHub Releases, тег вида `v1.0.0`.
- APK прикрепляется к релизу как asset.
- Токен для пуша/релиза — только временный, после использования удалить и отозвать.

---

## 8. Сборка

Требования: JDK 21, Android SDK 37.

```bash
./gradlew :app:assembleDebug
```

Результат:

```
app/build/outputs/apk/debug/Pixtone-1.0.1.apk
```

Полезные команды:

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug --offline
```

`local.properties` содержит локальный путь к SDK и **не коммитится**. В `gradle.properties` не должно быть локальных путей вроде `org.gradle.java.home`.

Android Studio: открыть корень проекта, использовать JDK 21 (Settings → Build Tools → Gradle → Gradle JDK).

---

## 9. Конвенции кода

- Kotlin, 4 пробела, официальный стиль.
- **Без комментариев в коде.**
- Состояние — `StateFlow`/`SharedFlow` + Compose state; без LiveData.
- ViewModel не держит Android View, но имеет `Application` и `AppContainer`.
- Сетевые и файловые операции — на `Dispatchers.IO`.
- Высокочастотное состояние (`PlayerProgress`) читать только в листовых композаблах; в списках и навигации — только `PlayerUiState`.
- Массовое чтение метаданных — параллельно с ограничением (`Semaphore`), прогресс в UI — с троттлингом.
- В lazy-списках указывать `key` и `contentType`; индекс элемента брать из `itemsIndexed`, а не через `indexOfFirst`.
- Жесты слайдеров: `pointerInput(Unit)` + `rememberUpdatedState(onSeek)` — ключ `pointerInput` не должен зависеть от лямбды, иначе drag отменяется на каждой рекомпозиции.
- Ошибки не должны ронять UI: `runCatching`, `safeFetch`, fallback-источники.
- Новые строки — по-русски, переводы — в `Translations.kt`.
- Не использовать `androidx.compose.material3.Text` напрямую в UI.
- Не переводить пользовательские данные.
- Не менять package/DB/prefs имена без необходимости.

---

## 10. Проверка перед сдачей

1. `./gradlew :app:compileDebugKotlin` — без ошибок.
2. `./gradlew :app:assembleDebug` — APK собирается.
3. Проверить затронутый экран в Android Studio/эмуляторе.
4. Проверить английскую локализацию (или все 6 языков, если менялись строки).
5. Проверить светлую/тёмную? Проект тёмный; проверить пресеты тем.
6. Проверить, что пользовательские данные (названия треков) не переводятся.
7. Убедиться, что не добавлены комментарии в код.

---

## 11. Скриншоты и README

- Скриншоты лежат в `docs/screenshots/`, имена оканчиваются на `_en.png` — это важно для обхода кеша GitHub.
- Снимки сделаны в **английском** интерфейсе и **стандартной теме PIXTONE**.
- Демо-библиотека и обложки сгенерированы специально для проекта.
- Текст песни на скриншоте — public domain гимн «Amazing Grace».
- README на английском; при добавлении фич обновлять разделы Features / Tech stack / Screenshots.

---

## 12. Лицензии

- Код проекта — MIT (`LICENSE`).
- Шрифты — SIL OFL 1.1, тексты в `assets/licenses/`.
- Библиотеки — Apache 2.0 / MIT, уведомления в `assets/licenses/THIRD_PARTY_NOTICES.txt`.
- В приложении есть экран **Settings → Licenses**.
- Не удалять лицензионные файлы из assets.

---

## 13. Частые проблемы

| Симптом | Причина / решение |
|---|---|
| `JAVA_HOME is not set` | Указать JDK 21: `JAVA_HOME=C:\Users\<user>\.jdks\jbr-21.0.11` |
| APK называется `app-debug.apk` | Проверить `androidComponents` блок в `app/build.gradle.kts`; сделать clean build |
| Текст в UI не переводится | Строка отрисована material `Text`, либо ключ не совпадает (регистр/шаблон) |
| Перевод пропал после `uppercase()` | Добавить ключ в верхнем регистре |
| Genius 429/403 | `GeniusGate` блокирует источник; подождать или использовать fallback |
| Обложки не обновляются | Ручное обновление ищет только треки без реальной обложки |
| Плейлист пуст после добавления | Проверить `playlist_tracks` и `position` |
| Релиз не обновляется | Удалить старый release и tag, создать заново после пуша |
| Кеш картинок на GitHub | Переименовать файл или изменить URL |

---

## 14. Публикация

Репозиторий: https://github.com/JameSanderlanding/PIXTONE

- Public.
- Default branch: `mobile`.
- Releases: `v1.0.0` и новее.
- В release прикладывается `Pixtone-<version>.apk`.
- Токен для публикации не хранить; после использования отзывать.

Если нужен новый релиз:
1. собрать APK;
2. закоммитить и запушить изменения;
3. создать тег `vX.Y.Z`;
4. создать GitHub Release с описанием;
5. прикрепить APK;
6. проверить скачивание и размер.
