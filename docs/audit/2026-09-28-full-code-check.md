# Полная проверка кода — 2026-09-28

Ветка: `feature/full-code-check`, база — коммит `a39817e`. Проверено: весь `app/src` (domain, data, presentation,
DI, ресурсы, манифест, тесты), `docs/`, `README.md`, `PRODUCT.md` против ТЗ (`docs/reference/tz-2026.md`, сверено с PDF).

Состояние на момент проверки: `./gradlew :app:testDebugUnitTest` — **160 тестов, 0 падений**.

Правила, которые должен соблюдать исполнитель (и любой следующий агент), вынесены в скилл
[`.claude/skills/finni-android-rules/SKILL.md`](../../.claude/skills/finni-android-rules/SKILL.md). Загрузить его до начала работы.

---

## 0. Как выполнять этот план

1. **Шаг 0 — рабочее дерево.** В начале проверки в рабочем дереве был незакоммиченный рефакторинг `onAction` →
   приватные функции (`AdventureViewModel`, `HomeViewModel`, `PetCreationViewModel`, `SavingsViewModel`,
   `ShopViewModel`); к моменту коммита плана его уже не было. Если он вернётся — закоммитить отдельно до фазы 1
   (в `PetCreationViewModel.kt` поправить порядок импортов: `PetColor` стоял над `androidx.*`). Если нет — сделать
   его в рамках C2: в `when` внутри `onAction` только вызовы приватных функций, по одной на действие.
2. Задачи идут фазами. Внутри фазы — по порядку ID. Одна задача = один коммит (`<область>: <что>`), тесты зелёные
   после каждого.
3. Задачи с пометкой **[решение владельца]** не выполнять, пока владелец не выбрал вариант (раздел 9). Для каждой
   указан вариант по умолчанию.
4. После каждой фазы: `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.
   После фаз 3 и 4 (заметные изменения UI) — прогон на эмуляторе со скриншотами до/после (дизайн не должен меняться).
5. Документацию (фаза 7) править в том же PR, что и код, — но отдельными коммитами.

Приоритеты: **P0** — ломает соответствие ТЗ или демонстрацию, **P1** — надёжность/ТЗ/UX, **P2** — архитектура и
чистота, **P3** — косметика.

---

## 1. Сводка находок

| ID | P | Область | Суть |
|---|---|---|---|
| A1 | P0 | ТЗ 3.1.4, 3.4, 3.5 | `RECORD_AUDIO` в манифесте; главный экран сам просит у ребёнка доступ к микрофону (по умолчанию `voiceRepeatEnabled = true`) |
| A2 | P0 | Lifecycle | 3D-питомец, звуки и **микрофон** продолжают работать после сворачивания приложения (нет реакции на `ON_STOP`) |
| A3 | P0 | ТЗ 3.1.1 | `minSdk = 27` (Android 8.1), а ТЗ требует Android 8.0 (API 26) |
| A4 | P0 | ТЗ 2.6 | Заданий 5, а нужно ≥ 6 по 3 темам (в теме «Покупаю» одно задание) |
| A5 | P0 | ТЗ 2.5.8 | В демо-режиме действует лимит 2 задания в неделю: «все задания… доступны сразу» не выполняется |
| A6 | P0 | Демо, ТЗ 2.5.13 | Раздел взрослого обещает «тестовый питомец, 100 монет», а демо стартует с 50 (`GameRules.START_BALANCE`) |
| B1 | P1 | ТЗ 3.3 | Релизная подпись не настроена: README описывает `keystore.properties`, но `build.gradle.kts` его не читает |
| B2 | P1 | ТЗ 3.3 | `app_name = "FinniApp"` — в лаунчере и карточке RuStore должно быть «Питомец Финни» |
| B3 | P1 | ТЗ 2.5.3 | На главном экране не видны числом накопления, название текущей цели и активное задание **[решение владельца]** |
| B4 | P1 | ТЗ 3.6 | Кнопка «Назад» разная: `TextButton("← Назад")` в Adult, `Surface` с текстом «‹» без описания в TaskPlay |
| B5 | P1 | Presentation | `AdultViewModel.confirm(onComplete)` принимает навигационную лямбду; UiState в файле VM; тексты ошибок в VM |
| B6 | P1 | ТЗ 3.5, 8.1 | Реакции «удар по голове/ноге» со взвизгом «мяу!» **[решение владельца]** |
| B7 | P1 | Compose/M3 | Кнопки сделаны `Surface(onClick)` / `Modifier.clickable`, выбор — без `selectable`/`toggleable` |
| B8 | P1 | Объяснимость | В `HomeInfoDialog` числа экономики зашиты в текст (50, 35, 15, +25/+30/+45, 30/40/70) |
| B9 | P1 | Data/Domain | «Взять из копилки и купить» — две отдельные записи (`withdraw` + `buy`), не атомарно |
| B10 | P1 | Мёртвый код | `SectionStubScreen`, `HomeAction.PetTapped`, `HomeSection.title`, `ShopItem.categoryText`, `GameState.planLeft`, `GameState.tasksPerWeek`, `colors.xml`, `Example*Test`, `InMemory*` в main |
| B11 | P1 | Тесты | Нет тестов минимумов контента ТЗ 2.6, правильного/ошибочного прохождения каждого задания, 5 недель демо подряд, `TaskPlayViewModel`, `cheapestFoodFor` |
| B12 | P1 | Lifecycle | Ввод в заданиях (`remember`) теряется при повороте/смерти процесса |
| B13 | P1 | Lifecycle | `MainActivity` собирает `snapshot` через `collectAsState`, а не `collectAsStateWithLifecycle` |
| C1 | P2 | Clean arch | Доменная `TaskOutcome` помечена `@Serializable` ради ключа навигации |
| C2 | P2 | Presentation | ViewModel с разным API: `onAction` у одних, `skipTutorialStep()`, `onToggle()`, `onSettingsChange()`, `unlock()` у других |
| C3 | P2 | Presentation | Сумма «Отложить» живёт в `remember` экрана с логикой клампа, а сумма «Забрать» — во ViewModel |
| C4 | P2 | Domain | «Сколько осталось до цели» (`goal.cost - savings`) считается в 4 местах UI |
| C5 | P2 | Presentation | Текст последствия снятия в `ShopScreen.savingsConsequence` считается в UI |
| C6 | P2 | Приватность | `allowBackup="true"`: игра уходит в облачную копию Google, сброс не удаляет её |
| C7 | P2 | ТЗ 3.6 | Основной текст 13–15 sp во многих местах; фиксированные высоты под шрифт 200 % **[решение владельца]** |
| C8 | P3 | Структура | Цикл пакетов `game.model` ↔ `game.engine` (модель импортирует `GameRules`) |
| C9 | P3 | Структура | `PetColor` в файле `PetLook.kt`; `TaskResultCard` в `TasksScreen.kt`; `AdultUiState` в VM |
| C10 | P3 | Производительность | GLB (несколько МБ) читается с диска на главном потоке в `PetModelController.setModel` |
| D1–D6 | P1 | ТЗ 5 | README и `docs/01–10` описывают другую систему (модуль `:domain`, `AppContainer`, JSON-контент, экраны Onboarding/CreateProfile, «всё не начато»); нет лицензий, карточки RuStore, раздела разрешений |

Что сделано хорошо и **менять не нужно**: чистые функции `GameEngine` + `Transition`; `GameSnapshot` как единый
агрегат; запись до публикации в `GameRepositoryImpl` с `Mutex`; DTO и мапперы формата сохранения с версией;
обработка сбоев DataStore (`onDisk`, `StorageError`, «Повторить» только для исправимых ошибок); `Feedback` с проверкой
полноты ключей; разделение `PetHost` над навигацией; Koin-модули, поднимаемые в JVM-тесте.

---

## 2. Фаза 1 — P0 (соответствие ТЗ и демо)

### A1. Микрофон: выключен по умолчанию, включает только взрослый

**ТЗ:** 3.1.4 «Камера, микрофон… для обязательного сценария не нужны»; 3.4 «запрашивает только те разрешения,
которые необходимы… каждое разрешение обосновывается»; 3.5 «без сбора персональных данных ребёнка».
Сейчас на первом же показе главного экрана ребёнок видит системный диалог «Разрешить запись аудио?» — это увидят и
эксперты на шаге 4 сценария.

Вариант по умолчанию (оставить функцию, но за барьером взрослого). Альтернатива — удалить функцию целиком (см. раздел 9).

1. `domain/settings/model/AppSettings.kt`: `voiceRepeatEnabled: Boolean = false`.
2. `data/settings/SettingsSaveFile.kt`: `val voiceRepeatEnabled: Boolean = false`.
3. `presentation/screens/home/HomeScreen.kt`:
   - удалить `rememberMicrophone()` целиком и импорты `Manifest`, `PackageManager`, `ContextCompat`,
     `rememberLauncherForActivityResult`, `ActivityResultContracts`, `LocalContext` (если больше не нужны);
   - в `PetArea`: `voiceEnabled = state.voiceRepeatEnabled && state.soundEnabled` (без `micGranted`).
     `PetVoice.start()` сам проверяет разрешение и молча не стартует без него.
4. `presentation/screens/adult/AdultScreen.kt` — запрос разрешения только отсюда:
   ```kotlin
   val context = LocalContext.current
   val micRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
       if (granted) viewModel.setVoiceRepeat(true)
   }
   PreferenceSwitch("Кот повторяет слова (микрофон)", state.settings.voiceRepeatEnabled, !state.busy) { enabled ->
       when {
           !enabled -> viewModel.setVoiceRepeat(false)
           ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
               PackageManager.PERMISSION_GRANTED -> viewModel.setVoiceRepeat(true)
           else -> micRequest.launch(Manifest.permission.RECORD_AUDIO)
       }
   }
   ```
   Под переключателем — одна строка пояснения: «Звук обрабатывается только на телефоне, не сохраняется и никуда не
   отправляется».
5. Манифест: комментарий к `RECORD_AUDIO` оставить, дописать «включается только взрослым в защищённом разделе».
6. Документация: раздел «Разрешения и данные» (задача D4).

**Готово, когда:** чистая установка → создание питомца → главный экран: системного диалога нет; в разделе взрослого
переключатель выключен; включение показывает системный запрос; отказ оставляет переключатель выключенным.
`DataStoreSettingsRepositoryTest.startsWithDefaults` зелёный (сверяется с `AppSettings()`).

### A2. Питомец, звук и микрофон засыпают, когда приложение свёрнуто

**Проблема:** `PetModel3D(active = state.shown)` зависит только от навигации. При сворачивании `active` остаётся
`true`: `Choreographer` продолжает кадры, `PetVoice` продолжает `AudioRecord` в фоне.

`presentation/components/PetHost.kt`:
```kotlin
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
...
val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
val started = lifecycleState.isAtLeast(Lifecycle.State.STARTED)
...
PetModel3D(..., active = state.shown && started, ...)
```
`setActive(false)` уже ставит отрисовку на паузу, глушит `PetSounds` и через `updateListening()` останавливает
микрофон; `setActive(true)` возобновляет с того же кадра.

**Готово, когда:** на эмуляторе: главный экран → Home-кнопка → в `adb shell dumpsys audio` / индикаторе микрофона
запись не идёт; возврат — питомец анимируется дальше, без рывка.

### A3. minSdk 26 (Android 8.0)

`app/build.gradle.kts`: `minSdk = 26`. Затем `./gradlew :app:lintDebug` — не должно быть `NewApi`.
`java.time` доступен с API 26, `AudioTrack.Builder` — с 23, Filament поддерживает 26.
Обновить README и `docs/06` (там «при наличии времени опустить до 26»).

### A4. Шестое задание (тема «Покупаю»)

`data/content/TaskContent.kt` — добавить последним элементом (таблица в `docs/05` уже называет его
`payments_two_prices`):
```kotlin
Task(
    id = "payments_two_prices",
    topic = TaskTopic.PAYMENTS,
    title = "Два корма",
    intro = "Финни нужен корм, и ещё ему хочется наклейки. Корм стоит 15, наклейки — 5. " +
        "Рядом такой же корм за 25, а наклейки к нему в подарок. Как купить выгоднее?",
    reward = 5, rewardOnMistake = 3,
    explanationSuccess = "15 + 5 = 20, а это меньше 25. «Подарок» не бесплатный: сравнивай, сколько стоит всё вместе.",
    explanationMistake = "Посчитаем: корм 15 и наклейки 5 — всего 20. Корм с подарком стоит 25. Подарок обошёлся бы в 10, а не в 5.",
    hint = "Сложи цену корма и наклеек и сравни с 25.",
    payload = TaskPayload.Choice(
        listOf(
            TaskPayload.Choice.Option("apart", "Корм за 15 и наклейки за 5 отдельно", true, "Всего 20 монет — на 5 меньше."),
            TaskPayload.Choice.Option("gift", "Корм с подарком за 25", false, "Наклейки обошлись в 10 монет вместо 5."),
            TaskPayload.Choice.Option("same", "Всё равно, цена одинаковая", false, "Не одинаковая: 20 и 25. Разница — 5 монет."),
        )
    ),
),
```
Тест — в B11 (`ContentMinimumsTest`, `TaskContentTest`).

### A5. Демо-режим снимает лимит заданий в неделю

1. `domain/game/model/GameState.kt`, `taskStatus`:
   `!demoMode && tasksDoneThisWeek >= GameRules.TASKS_PER_WEEK -> TaskStatus.LIMIT_REACHED`.
   `RETRY_NEXT_WEEK` не трогать: в демо неделя закрывается сразу, повтор ошибочного задания доступен со следующей.
2. `TasksUiState.perWeek: Int?` — `null` в демо; `TasksViewModel`: `perWeek = if (game.demoMode) null else GameRules.TASKS_PER_WEEK`
   (и удалить `GameState.tasksPerWeek`, см. B10). `TasksScreen`: `WeekCounter` показывать только при `perWeek != null`.
3. Тест в `GameEngineTest`: `demoLiftsWeeklyTaskLimit` — в демо после двух ответов третье задание `AVAILABLE`;
   без демо — `LIMIT_REACHED` (существующий `answeringTaskPaysRewardOnceAndLimitsTasksPerWeek` не ломается).

### A6. Правдивый текст о демо-профиле

`AdultScreen.kt`, строки «Тестовый питомец, 100 монет…» и «…Финни Демо, 100 монет, неделя 1» → подставлять
`GameRules.START_BALANCE` (сейчас 50): `"Тестовый питомец, ${GameRules.START_BALANCE} монет, первая неделя…"`.
Исправить то же в `docs/07` («заяц, 100 монет») и `docs/03`, `docs/09` (там 100 и 60 — старая экономика).

---

## 3. Фаза 2 — тесты (закрепить ТЗ до рефакторинга)

### B11. Недостающие тесты

Стиль как в существующих: JUnit4, `kotlin.test`-ассерты не вводить, имена `camelCase` по-английски, комментарии
по-русски. Пакет — тот же плоский `ru.larpinovplay.finniapp` (как у остальных тестов).

1. **`ContentMinimumsTest`** (ТЗ 2.6, против `defaultContent()`):
   - `PetColor.entries.size >= 9`;
   - `shopItems.size >= 8`, есть и `MANDATORY`, и `OPTIONAL`;
   - `goals.size >= 3`;
   - `tasks.size >= 6`, каждая из `TaskTopic.entries` представлена;
   - `PetGrowthStage.entries.size >= 3`, пороги возрастают;
   - все `id` уникальны в каждом списке; у каждого задания `reward > rewardOnMistake > 0`, непустые
     `explanationSuccess/Mistake`; у `Choice` есть и верный, и неверный вариант.
2. **`TaskContentTest`** — для **каждого** задания из `defaultTasks` найти верный и неверный ответ и проверить
   `task.evaluate(...)`: верный → `success`, `reward`; неверный → `!success`, `rewardOnMistake`, объяснение непустое.
   Для `Allocate`/`ShopList` ответы собрать из payload (минимумы + остаток; только обязательное / всё подряд).
3. **`DemoWeeksTest`** (ТЗ 2.6 «не менее 5 периодов в демо»): `GameRepositoryImpl(InMemoryGameStore(), clock = fixed,
   adventures = defaultAdventures)`, `resetToDemo()`, 5 раз: `confirmPlan` (всё в копилку или сбалансированный план),
   пройти приключение недели (`completeAdventure(adventureOfWeek, 0)`), `finishWeek()` — **в тот же день**.
   Ожидание: `week == 6`, `history.size == 5`, ни одного `Blocked`.
4. **`TaskPlayViewModelTest`**: двойной `Submit` засчитывается один раз; при `StorageError` задание не засчитано и
   повторный `Submit` проходит (фейковый `GameStore`, как в `StorageTestSupport`).
5. **`ShopRulesTest`**: `cheapestFoodFor(35, defaultShopItems)` == 20 (две порции овощей); пустой список → `null`.

---

## 4. Фаза 3 — P1: сборка, ТЗ, надёжность

### B1. Подпись релиза из `keystore.properties`

`app/build.gradle.kts` (файла нет — сборка остаётся неподписанной и не падает):
```kotlin
import java.util.Properties

val keystoreProps = rootProject.file("keystore.properties").takeIf { it.exists() }
    ?.let { file -> Properties().apply { file.inputStream().use(::load) } }

android {
    signingConfigs {
        if (keystoreProps != null) create("release") {
            storeFile = file(keystoreProps.getProperty("storeFile"))
            storePassword = keystoreProps.getProperty("storePassword")
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
        }
    }
    buildTypes {
        release {
            if (keystoreProps != null) signingConfig = signingConfigs.getByName("release")
            ...
        }
    }
}
```
`.gitignore` уже содержит `keystore.properties`, `*.jks`, `*.keystore`. Проверка: `./gradlew :app:assembleRelease`
без файла — успешно (unsigned); с временным keystore — `apksigner verify` проходит.

### B2. Название приложения

`res/values/strings.xml`: `<string name="app_name">Питомец Финни</string>`.

### B3. Главный экран: накопления, цель и активное задание видны сразу — [решение владельца]

**ТЗ 2.5.3:** «Питомец, доступный баланс, сумма накоплений, текущая цель, основные показатели состояния и активное
задание видны одновременно». Сейчас: накопления — только долей кольца, названия цели нет, задание — только число
на бейдже и в облачке, которое прячется через 6 с.

Вариант по умолчанию (минимально меняет дизайн, повторяет существующий бейдж «Задания»):
- у пункта «Копилка» при выбранной цели — такой же бейдж, как у «Заданий», с текстом `"${savings}/${cost}"`, а подпись
  под наклейкой — название цели (`maxLines = 1`, `TextOverflow.Ellipsis`) вместо слова «Копилка»;
- у пункта «Задания» подпись — название ближайшего доступного задания или приключения недели
  (`HomeUiState.activeTask: String?`, считает `HomeViewModel`: приключение недели, иначе первое `AVAILABLE`),
  без него — «Задания».
Нужны правки `HomeUiState`, `HomeViewModel.toUiState`, `HomeScreen.BottomMenu/MenuItem`, тест в `HomeViewModelTest`.

### B4. Одинаковая кнопка «Назад»

- `AdultScreen`: заменить `TextButton("← Назад")` + заголовок на общий `ScreenHeader("Для взрослых", onBack)`.
- `TaskPlayScreen`: заменить `Surface(onClick){ Text("‹") }` + заголовок на `ScreenHeader(task.title, onBack)`.
- Проверить, что все экраны раздела используют `ScreenHeader`/`BackButton` (grep `onBack` в `screens/`).

### B5. AdultViewModel по конвенциям

1. `AdultUiState`, `AdultConfirmation` → `AdultUiState.kt`; новый `AdultAction.kt`
   (`ChangeAnswer`, `Unlock`, `SetSound`, `SetAnimations`, `SetVoiceRepeat`, `Request`, `DismissConfirmation`, `Confirm`).
2. Навигация: вместо `confirm(onComplete)` — `private val _events = Channel<AdultEvent>()`, `val events = _events.receiveAsFlow()`,
   `AdultEvent.Done`; экран: `ObserveAsEvents(viewModel.events) { onBack() }`.
   Причина: лямбда `onBack`, переданная в корутину, после поворота указывает на старый back stack.
3. Тексты ошибок: `error: AdultError?` (`WRONG_ANSWER`, `NEW_EXAMPLE`, `ACTION_FAILED`), слова — в экране.
4. Пример: `data class Example(val first: Int, val second: Int)`, генерация — `private fun newExample(random: Random)`,
   `Random` приходит в конструктор (по умолчанию `Random.Default`) — тест детерминирован. Убрать `Random` из
   значений по умолчанию data-класса.
5. Разделить `AdultScreen` / `AdultScreenContent(state, onAction)` + `@Preview`.
6. Обновить `AdultViewModelTest` под `onAction`.

### B6. Реакции «удар» — [решение владельца]

`PetSounds.hitHead/hitFoot`, клипы `HitHead`, `HitFoot.L/R`, звуки `pet_hit_*.wav` («глухой удар и взвизг „мяу!“»).
ТЗ 3.5: «визуальные реакции не должны запугивать»; 8.1: «этичность мотивации». Ребёнок, стукнувший питомца и
услышавший визг, — риск для оценки экспертов.
Вариант по умолчанию: в `PetModelController.onTap` зоны `HEAD`/`FOOT_*` запускают обычное приветствие
(`tapAnimation`), `PetHits` и звуки ударов не используются (удалить `pet_hit_*.wav`, `hitHead/hitFoot`,
`PetHitAnimations`). Альтернатива — переименовать в «щекотку» и заменить звуки на весёлые (нужны новые ассеты).

### B7. Кнопки и выбор — компонентами Material 3 (дизайн не меняется)

Общее правило и рецепты — в скилле (раздел «Compose»). Здесь — перечень мест.

**`components/StickerKit.kt`:**
| Компонент | Сейчас | Сделать |
|---|---|---|
| `PillButton` | `Surface(onClick)` | `Button(shape = CircleShape, colors = ButtonDefaults.buttonColors(color, ink), contentPadding = PaddingValues(horizontal = 16.dp), modifier = modifier.height(48.dp))` |
| `TealButton` | `Surface(onClick, enabled)` | `Button(enabled, colors = buttonColors(containerColor = Teal, disabledContainerColor = Pebble, contentColor = White, disabledContentColor = InkMuted), elevation = ButtonDefaults.buttonElevation(6.dp, disabledElevation = 0.dp))`, высота 56 dp |
| `SoftButton` | `Surface(onClick)` | `FilledTonalButton` / `Button` с `Pebble`, высота 52 dp |
| `PebbleButton` | `Surface(onClick)` + `semantics` | `FilledIconButton(onClick, Modifier.size(48.dp), shape = CircleShape, colors = IconButtonDefaults.filledIconButtonColors(containerColor = color)) { Image(..., contentDescription = description) }` |
| `BackButton` | `Surface` + `creamCard` | `IconButton(onClick, modifier.size(48.dp).creamCard(CircleShape, 8.dp, 3.dp))`, `contentDescription = "Назад"` на иконке |
| `StepButton` | `Surface(enabled)` + `.alpha()` | `FilledIconButton(enabled, colors = …disabledContainerColor/disabledContentColor)`, без `.alpha` |
| `CoachNote` «Пропустить шаг» | `Text.clickable` | `TextButton(onClick = onSkip, modifier = Modifier.align(Alignment.End))` |
| `FinniSnackbar` action | `Text.clickable` | `TextButton(onClick = data::performAction)` |
| `MeterRing(onClick)`, `CoinPill(onClick)` | `Surface(onClick)` | оставить `Surface(onClick)` (кликабельная поверхность), но добавить `role = Role.Button` в `semantics` |

**Экраны:**
| Файл | Элемент | Сделать |
|---|---|---|
| `home/HomeScreen.kt` | `PetButton`, `PlanButton` | `IconButton` + `Modifier.creamCard(...)`; `contentDescription` на иконке, без `clearAndSetSemantics` |
| `home/HomeScreen.kt` | ссылка в `SpeechBubble` (`Row.clickable`) | `TextButton` с иконкой-стрелкой; сам пузырь оставить `clickable(onClickLabel = "Спрятать")` |
| `home/HomeScreen.kt` | `MenuItem`, `TypingBubble`, `WeekSun` | составные кастомные элементы: оставить `clickable(role = Role.Button)` / `Surface(onClick)` — допустимое исключение (см. скилл) |
| `petcreation/PetCreationScreen.kt` | «Пропустить» (`Text.clickableButton`) | `TextButton` с кремовым фоном (`colors = ButtonDefaults.textButtonColors(containerColor = Cream.copy(0.85f))`) |
| `petcreation/PetCreationScreen.kt` | кружки раскрасок | `Modifier.selectable(selected, role = Role.RadioButton, onClick)`; у родительской строки — `Modifier.selectableGroup()`; удалить `clickableButton` |
| `shop/ShopScreen.kt` | вкладки `CategoryTabs` | `Modifier.selectable(isSelected, role = Role.Tab)` на `Surface` без `onClick` (или `Surface(selected = …, onClick = …)`) + `selectableGroup()` у `Row` |
| `tasks/TaskPlayScreen.kt` | варианты `ChoiceWidget` | `Surface(selected = isSelected, onClick = …)` + `semantics { role = Role.RadioButton }`, `selectableGroup()` у колонки |
| `tasks/TaskPlayScreen.kt` | `StepButton` (дубль) | удалить, использовать `StepButton` из `StickerKit` (цвета — параметрами, если нужны синие) |
| `tasks/TasksScreen.kt` | `ShopListRow` | `Row.toggleable(checked, role = Role.Checkbox, onValueChange = { onToggle() })` + `Checkbox(checked, onCheckedChange = null)` — сейчас две цели нажатия |
| `adventure/AdventureScreen.kt` | монеты сдачи, варианты ответа | `Surface(selected, onClick, enabled)` + `role = Role.RadioButton`; убрать `.alpha()` — «приглушение» через цвет/`enabled` |
| `adventure/BasketScene.kt` | товары корзины | `Surface(checked = picked, onCheckedChange = { … }, enabled = !locked)` (toggleable-перегрузка `Surface`) |
| `wardrobe/WardrobeScreen.kt` | `ItemTile` (надеть/снять) | `Surface(checked = entry.worn, onCheckedChange = { onToggle() })`, `role = Role.Switch` сохранить |
| `adventure/AdventureScreen.kt`, `tasks/TasksScreen.kt` | `Hint`, `HintButton` — две разные реализации | оставить одну (`Hint` из приключений, как «пилюля с лампочкой»), `Surface(onClick)` → `Button` по рецепту `PillButton` |

`ShopItemTile`, `GoalTile`, `AdventureHero`, `TaskRow`, `OptionRow`, `EmptySlotTile`, карточка «Для взрослых» и
`ExpandableCard` в настройках — кликабельные карточки: `Surface(onClick)` допустим (M3 «clickable card»), проверить
только, что `role`/`contentDescription` не теряются при `clearAndSetSemantics`.

**Готово, когда:** скриншоты до/после на 360×640 и 411×891 совпадают; TalkBack читает у каждой кнопки роль
«кнопка», у вариантов — «переключатель/флажок, выбрано»; `grep -rn "\.clickable(" presentation` даёт только
разрешённые исключения.

### B8. Числа экономики в объяснениях — из правил

`home/HomeInfoDialog.kt`: все числа брать из источника правды:
- «карманные — 50» → `GameRules.weekIncome(PetGrowthStage.BABY)`;
- «5 за задание и 5 за приключение» → из контента: `HomeUiState` получает `taskReward`/`adventureReward`
  (`content.tasks.maxOf { it.reward }`, `content.adventures.maxOf { it.reward }`) — считает `HomeViewModel`;
- «овощи +25, фрукты +30, мясо +45» → `HomeUiState.foods: List<Pair<String, Int>>` из
  `content.shopItems.filter { it.satiety > 0 }`;
- «падает на 35», «на 15» → `GameRules.WEEKLY_HUNGER`, `GameRules.WEEKLY_MOOD_DECAY`;
- «меньше 30», «от 70», «от 40» → `PetSatiety.HUNGRY_BELOW`, `PetMood.HAPPY_FROM`, `PetMood.CALM_FROM`.
Тест: `HomeViewModelTest` — `foods` совпадает с едой из контента.

### B9. «Взять из копилки и купить» — одна команда

1. `GameEngine.buyWithSavings(game, item): Transition<PurchaseResult>`: `missing = max(0, price - balance)`;
   если `missing > savings` — `NotEnough(missing - savings)` без изменений; иначе `withdraw(missing)` → `buy(item)`
   на промежуточном снимке; если покупка не `Success` — вернуть исходный `game`.
   `PurchaseResult.Success` получает поле `fromSavings: Int = 0`.
2. `GameRepository.buyWithSavings(item)` / `GameRepositoryImpl` через `execute { GameEngine.buyWithSavings(it, item) }`.
3. `ShopViewModel.buyWithSavings` — один вызов, `PurchaseFeedback.Bought(item, balanceAfter, fromSavings = result.fromSavings)`.
4. Тесты: `GameEngineTest.buyWithSavingsTakesExactlyMissingAndBuys`, `...ChangesNothingWhenSavingsDoNotCover`;
   существующий `ShopViewModelTest.missingCoinsCanBeTakenFromSavingsExactly` остаётся зелёным.

### B10. Удалить мёртвый код

| Что | Где | Как |
|---|---|---|
| `SectionStubScreen` | `screens/home/SectionStubScreen.kt` | удалить файл |
| `HomeSection.title` | `screens/home/HomeSection.kt` | удалить свойство (использовалось только заглушкой); `enum class HomeSection { … }` |
| `HomeSection.ADULT` | там же, `Routes.kt` | удалить, если после правок никто не шлёт `OpenSection(ADULT)` (вход — из настроек) |
| `HomeAction.PetTapped` | `HomeAction.kt`, `HomeViewModel` | удалить (никто не отправляет; `// TODO`) |
| `ShopItem.categoryText` | `shop/ShopItemUi.kt` | удалить (не используется) |
| `GameState.planLeft` | `domain/game/model/GameState.kt` + тест | удалить вместе с `GameEngineTest.planLeftShows…` (в проде бюджет считает `ShopViewModel.budgetsOf`) |
| `GameState.tasksPerWeek` | `GameState.kt` | удалить, см. A5 |
| `colors.xml` | `res/values/colors.xml` | удалить (шаблонные purple/teal, ссылок нет) |
| `ExampleUnitTest`, `ExampleInstrumentedTest` | `src/test`, `src/androidTest` | удалить (шаблон) |
| `InMemoryGameStore`, `InMemorySettingsRepository` | `src/main/.../data/...` | перенести в `src/test/java/ru/larpinovplay/finniapp/` (используются только тестами); `InMemorySettingsRepositoryTest` удалить (тест фейка) |

После — `./gradlew :app:lintDebug` (`UnusedResources`) и инспекция Android Studio «Unused symbol» по `presentation`:
удалить то, что всплывёт (например, неиспользуемые цвета `FinniColors`).

### B12. Ввод не теряется при повороте

- `tasks/TaskPlayScreen.kt`: `ChoiceWidget.selected`, `AllocateWidget.amounts`, `ShopListWidget.selected` →
  `rememberSaveable` (для `Map`/`Set` — `rememberSaveable(saver = …)` или хранить как `List<String>`/`IntArray`).
- `settings/SettingsScreen.kt`: `introOpen`, `glossaryOpen` → `rememberSaveable`.

### B13. MainActivity

`game.snapshot.collectAsState()` → `collectAsStateWithLifecycle()`; импорты упорядочить.

---

## 5. Фаза 4 — P2: архитектура и единообразие

### C1. Домен без kotlinx.serialization

1. Новый `presentation/screens/tasks/TaskOutcomeUi.kt`:
   ```kotlin
   @Serializable
   data class TaskOutcomeUi(val success: Boolean, val reward: Int, val consequence: String?, val explanation: String)
   fun TaskOutcome.toUi() = TaskOutcomeUi(success, reward, consequence, explanation)
   ```
2. `Routes.TaskResult(val taskId: String, val outcome: TaskOutcomeUi)`; `MainNavigation`: `TaskResult(route.taskId, outcome.toUi())`.
3. `TaskResultCard(result: TaskOutcomeUi, …)` — вынести из `TasksScreen.kt` в `tasks/TaskResultCard.kt`.
4. `domain/task/model/TaskOutcome.kt`: убрать `@Serializable` и импорт. Проверка: `grep -rn kotlinx.serialization app/src/main/java/ru/larpinovplay/finniapp/domain` — пусто.

### C2. Единый API ViewModel: `onAction(XAction)`

| ViewModel | Сейчас | Добавить |
|---|---|---|
| `TasksViewModel` | `skipTutorialStep()` | `TasksAction.kt`: `SkipTutorialStep` |
| `WardrobeViewModel` | `onToggle(item)`; `stateIn(Eagerly)` | `WardrobeAction.Toggle(item)`; состояние — `_state` + `asStateFlow()` как у остальных |
| `SettingsViewModel` | `onSettingsChange(AppSettings)` (перезаписывает целиком — гонка с параллельным изменением) | `SettingsAction.SetSound/SetAnimations/SetTips(enabled)`; запись через `updateSettings { it.copy(...) }` |
| `AdultViewModel` | набор методов | см. B5 |
| `TaskPlayViewModel` | уже `onAction` | — |
Экраны передают `viewModel::onAction` в `XScreenContent`. Тесты — под новый API.

### C3. Сумма «Отложить» во ViewModel

`SavingsScreen.kt` держит `picked` в `remember` и сам клампит по `GameRules.PLAN_STEP` и балансу; «Забрать» уже
живёт во VM (`SavingsUiState.Withdraw`). Сделать симметрично: `SavingsUiState.depositAmount: Int`,
`SavingsAction.ChangeDeposit(increase)`, кламп в `SavingsViewModel` (от `PLAN_STEP` или всего баланса, если он меньше,
до баланса), `Deposit` без параметра. Тест в `SavingsViewModelTest`: `depositAmountStaysWithinStepAndBalance`.

### C4. «Осталось до цели» — в домене

`GameState.goalRemaining: Int?` (`goal?.let { (it.cost - savings).coerceAtLeast(0) }`); в `SavingsUiState`,
`ProgressUiState` — готовое поле; в `WithdrawDialog` — `SavingsUiState.Withdraw.remainingBefore/After`
(считает VM). Убрать арифметику из `SavingsScreen`, `ProgressScreen`, `WithdrawDialog`, `ShopScreen`.

### C5. Последствие «взять из копилки» — в UiState

`PurchaseFeedback.NotEnough` получает `savingsAfter: Int` и `goalRemainingAfter: Int?` (считает `ShopViewModel`);
`ShopScreen.savingsConsequence` только подставляет числа в строку.

### C6. Резервная копия

`AndroidManifest.xml`: `android:allowBackup="false"`, удалить `dataExtractionRules`/`fullBackupContent` и файлы
`res/xml/backup_rules.xml`, `data_extraction_rules.xml`. Причина: детское приложение, сброс профиля взрослым должен
удалять данные полностью; облачная копия — передача данных третьей стороне (ТЗ 3.5). Убрать из `docs/06` фразу про
Auto Backup.

### C7. Текст ≥ 16 sp и шрифт 200 % — [решение владельца]

ТЗ 3.6: основной текст ≥ 16 sp. Сейчас 13–15 sp у реплик Финни (`SpeechBubble` 15), `CoachNote` (15),
строк окон (`HomeInfoDialog` 15, `DeedsCard` 14–15, `StatRow` 15), `PillButton` (15), `OnRoomLabel` (14).
Вариант по умолчанию: основной (читаемый ребёнком) текст → 16 sp, подписи/бейджи/подзаголовки — как есть.
Размеры — через `MaterialTheme.typography` (`bodyLarge` 16 sp уже есть), а не литералы `fontSize = 15.sp`.
Затем проверка на эмуляторе с `adb shell settings put system font_scale 2.0`: элементы с фиксированной высотой
(`CoinPill` 52 dp, `PillButton` 48 dp + `maxLines = 1`, `MenuItem` `softWrap = false`, `WeekSun`) не должны обрезать
текст — где обрезают, заменить `height` на `heightIn(min = …)`.

---

## 6. Фаза 5 — P3: структура и мелочи

- **C8.** `GameRules` → `domain/game/rules/GameRules.kt` (и модель, и движок зависят от правил, цикла `model` ↔
  `engine` нет). Обновить импорты.
- **C9.** `PetColor` → свой файл `domain/pet/model/PetColor.kt`; `TaskResultCard` → свой файл (C1);
  `AdultUiState` → свой файл (B5). Импорты: сначала `android`/`androidx`, затем `kotlinx`, `org`, затем проект —
  в нескольких файлах (`MainActivity`, `StickerKit`, `GameRepository`, `GameRepositoryImpl`, `DataStoreGameStore`)
  проектные импорты стоят первыми (Optimize Imports в IDE).
- **C10.** `PetModelController.setModel`: чтение GLB (`assets.open().readBytes()`) — на главном потоке. Filament
  требует главный поток для `loadModelGlb`, но чтение байтов можно вынести: `withContext(Dispatchers.IO)` в
  `LaunchedEffect(assetName)` внутри `PetModel3D`, затем передать `ByteBuffer` контроллеру. Делать только если замер
  холодного старта на устройстве > 5 с (ТЗ 3.4); иначе пометить `// ponytail:` и оставить.
- `GameRepositoryImpl.resetToDemo`: имя «Финни Демо» зашито в `data` — перенести константу в домен
  (`Pet.DEMO_NAME`) рядом с `Pet.newborn`.
- `AssetsModule`: чтение `feedback.json` (≈ 5 КБ) при первом `koinInject` на главном потоке — допустимо, оставить.

---

## 7. Фаза 6 — документация (ТЗ 5)

Документация сейчас описывает проект с модулем `:domain`, ручным `AppContainer`, JSON-контентом, экранами
Onboarding/CreateProfile/AdultGate и статусом «не начато» у всех требований. Эксперты сверяют код с документами
(ТЗ 3.4: «Исходный код соответствует сопроводительной документации»).

### D1. README.md
- Стек: Kotlin 2.4.20, AGP 9.2.1, Compose BOM 2026.09.00, Navigation 3, Koin 4.2, DataStore 1.2, Filament 1.71.5;
  `compileSdk 37`, `targetSdk 37`, `minSdk 26`; JDK 21 (скачивается Gradle автоматически, `gradle/gradle-daemon-jvm.properties`).
- Убрать модуль `domain/` и `./gradlew :domain:test` → `./gradlew :app:testDebugUnitTest`.
- Состав: `app/src/main/java/.../{app,domain,data,presentation}`, `assets/cat` (3D), `assets/content/feedback.json`,
  `data/content/*.kt` (учебный контент).
- Релиз: шаги B1, `versionName 1.0`, `versionCode 1`.
- Демо и сброс: «Настройки → Для взрослых → пример → Демо-режим / Сбросить профиль / Удалить все данные».
- Статус: ссылка на `docs/08`, убрать «этап 0».

### D2. docs/01–07 привести к коду
| Файл | Что исправить |
|---|---|
| `01` | Глоссарий: `Money/Wallet/Period/PeriodFacts/SpendCategory/PetState/Profile` → фактические `GameState.balance/savings`, `GameState.week`, `BudgetPlan`, `BudgetDirection`, `ShopCategory`, `SavingsGoal`, `WeekSummary`, `WeekDeeds`, `Pet` (`PetSatiety`, `PetMood`, `PetGrowthStage`). Таблица контента: 9 раскрасок, 11 товаров (3 обязательных/8 необязательных), 4 цели, 6 заданий + 5 приключений, 3 стадии; «где лежит» — `data/content/*.kt`. Сценарий: экраны `PetCreation`, `Home`, `WeekPlanDialog`, `Tasks/TaskPlay/Adventure`, `Shop`, `Savings`, `WeekSummaryDialog`, `Settings → Adult` |
| `02` | Переписать под `GameSnapshot(GameState, Pet)`, `LedgerEntry/LedgerReason`, `WeekSummary`, результаты команд (`PurchaseResult`, `DepositResult`, `WithdrawResult`, `ConfirmPlanResult`, `FinishWeekResult`/`FinishBlock`), `Result<_, StorageError>` |
| `03` | Процессы — по `GameEngine`: план раскладывает весь баланс, строка «Копилка» сразу в копилку, неделя закрывается после плана и приключения (в обычном режиме — не в день начала), дела недели вместо критериев A/B/C, обучение первой недели (`TutorialStep`) вместо онбординга. Убрать `CreateProfile`, `ClaimDailyBonus`, `AdultBonus`, `RecoveryOption` или пометить «не реализовано» |
| `04` | Сверить с `GameRules`/контентом (цели 60/90/110/120 — в `04` указаны 60/90/120/150); добавить правило демо: лимит заданий снят (A5) |
| `05` | Контент — Kotlin-списки в `data/content/` (не JSON); «Как добавить задание» — добавить элемент в `defaultTasks` + прогнать `ContentMinimumsTest`/`TaskContentTest`; приключения (`AdventureContent.kt`, 4 типа сцен). Тексты для ребёнка: `feedback.json` + подписи в `presentation` |
| `06` | DI — Koin (`appModule`, `storageModule`, `assetsModule`), не `AppContainer`; убрать «Настройки… в `GameState.settings`» и «Остальные ViewModel'и пока игнорируют Error // TODO»; пакеты — фактическое дерево; раздел Filament/PetHost оставить; добавить lifecycle-правило A2 |
| `07` | Граф навигации по `Routes.kt`; убрать Onboarding/CreateProfile/AdultGate/Insufficient как экраны (они — шаги/диалоги); «Adult открывается из настроек»; «Ограничение: хранится в памяти» — удалить (DataStore) |

### D3. docs/08 — матрица соответствия
Заполнить статусы и ссылки (после фаз 1–4):

| Пункт | Где | Статус |
|---|---|---|
| 2.5.1 знакомство, три решения | `PetCreationScreen` (HELLO/PLAN/GROW/DREAM), обучение `TutorialStep` | реализовано |
| 2.5.1 гостевой режим | `PetCreationViewModel` (раскраска + игровое имя питомца, без аккаунта) | реализовано |
| 2.5.1 подсказка в любой момент | `SettingsScreen` «Как играть», «Словарик», переключатель «Подсказки» | реализовано |
| 2.5.2 | `PetCreationScreen` (9 раскрасок, имя) | реализовано |
| 2.5.3 | `HomeScreen` | реализовано после B3 |
| 2.5.4 | `LedgerEntry`, `ProgressScreen` (журнал недели), `HomeInfoDialog` | реализовано |
| 2.5.5 | `WeekPlanDialog`, `ActivePlanDialog`, `HomeViewModelTest` | реализовано |
| 2.5.6 | `ShopScreen` (подтверждение, «не хватает» с вариантами), `GameEngineTest`, `ShopViewModelTest` | реализовано |
| 2.5.7 | `SavingsScreen`, `WithdrawDialog`, `GameState.weeksToGoal`, `SavingsViewModelTest` | реализовано |
| 2.5.8 | `TasksScreen`, `TaskPlayScreen`, `AdventureScreen`, `TaskContentTest` | реализовано после A4/A5 |
| 2.5.9 | `BoughtDialog`, `TaskResultCard`, `WeekSummaryDialog`, `DeedsCard` | реализовано |
| 2.5.10 | `PetGrowthStage`, `WeekDeeds`, `EconomyBalanceTest` | реализовано |
| 2.5.11 | `ProgressScreen`, «Словарик» | реализовано |
| 2.5.12 | `AdultScreen` (барьер-пример), темы, прогресс; баллы от взрослого | реализовано; баллы — не реализовано (опционально) |
| 2.5.13 | `DataStoreGameStore`, `resetToDemo`, `DemoWeeksTest` | реализовано |
| 2.5.14 | `data/content/TaskContent.kt` | реализовано |
| 2.6 | `ContentMinimumsTest` | реализовано после A4 |
И нефункциональные 3.x с учётом A1–A3, B1, C6.

### D4. Недостающие документы по ТЗ 5
- `docs/permissions-and-data.md` (ТЗ 5.9): разрешения (`RECORD_AUDIO` — зачем, кто включает, что происходит со
  звуком), какие данные хранятся (`files/datastore/game.json`, `settings.json`: имя питомца, игра, настройки),
  где, как удаляются (раздел взрослого), нет сети/аналитики/рекламы.
- `docs/licenses.md` (ТЗ 5.12, 3.3): библиотеки (AndroidX/Compose — Apache 2.0, Koin — Apache 2.0, Filament —
  Apache 2.0, kotlinx — Apache 2.0), звук мурчания freesound.org #117612 (указать автора и лицензию, при CC-BY —
  атрибуцию), 3D-модели кота (`docs/pet-model-prompt.md` — кто и чем создал), фоны комнаты, иконки, шрифт (системный).
- `docs/rustore-card.md` (ТЗ 3.3): название, категория, краткое/полное описание, иконка 512×512, ≥ 3 скриншота,
  обоснование возрастной маркировки.
- `docs/09`: заменить T-01…T-25 (старый движок) таблицей «кейс → тестовый класс/метод»; E2E-сценарий — с
  фактическими числами (старт 50, карманные 50/60/70); заполнить отчёт о проверке на физическом устройстве.
- `docs/10`: убрать `:domain`, `AppContainer`; раздел «Известные ограничения» оставить и обновить.

---

## 8. Фаза 7 — итоговая проверка

1. `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease` — зелёные.
2. Эмулятор (портрет 360×640 и типичный 411×891) — пройти сценарий Приложения А:
   первый запуск → создание питомца (нет системных диалогов) → план → задание → обязательная и необязательная покупка
   + попытка при нехватке → цель и пополнение → завершить неделю ×5 в демо → свернуть/убить процесс/открыть
   (прогресс на месте) → Настройки → Для взрослых → сброс.
3. Шрифт 200 %, TalkBack на главном экране и в магазине — роли и описания читаются.
4. Скриншоты до/после фаз 3–4: дизайн совпадает.
5. Обновить `docs/08` статусы.

---

## 9. Решения владельца (ответить до соответствующих задач)

| # | Вопрос | Варианты | По умолчанию |
|---|---|---|---|
| 1 | Микрофон «Повторюшка» (A1) | (а) оставить, выключен, включает взрослый; (б) удалить функцию, `RECORD_AUDIO` и `PetVoice` | (а) |
| 2 | Реакции «удар» (B6) | (а) заменить на обычное приветствие, удалить звуки ударов; (б) переименовать в «щекотку» + новые звуки; (в) оставить | (а) |
| 3 | Главный экран (B3) | (а) бейдж «20/60» + имя цели и ближайшее задание в подписях; (б) свой вариант дизайна | (а) |
| 4 | Размер текста (C7) | (а) основной текст 16 sp; (б) оставить и обосновать в документации | (а) |
| 5 | Формат контента | (а) оставить Kotlin-списки, поправить docs; (б) перенести задания/приключения/товары/цели в `assets/content/*.json` по `docs/05` | (а) — ТЗ 2.5.14 выполнено: новое задание — новый элемент списка без правки логики |
