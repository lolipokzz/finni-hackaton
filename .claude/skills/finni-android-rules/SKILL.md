---
name: finni-android-rules
description: Правила проекта «Питомец Финни» (Android, Kotlin, Compose, Koin, DataStore) и ошибки, найденные аудитом 2026-09-28. Загружать перед любой правкой кода, контента или docs в этом репозитории — экраны, ViewModel, domain, data, тесты, манифест, Gradle.
---

# Питомец Финни — правила для агентов

ТЗ: `docs/reference/tz-2026.md`. План исправлений аудита: `docs/audit/2026-09-28-full-code-check.md`.
Детское приложение (7–11 лет) на конкурс: эксперты проверяют и код, и соответствие ТЗ, и документацию.

## 1. Слои и зависимости

```
domain/        чистый Kotlin: модели, GameEngine (чистые функции), правила, интерфейсы репозиториев, Result/StorageError
data/          реализации репозиториев, DataStore, DTO+мапперы, контент (data/content/*.kt, assets/content/feedback.json)
presentation/  Compose-экраны, ViewModel, UiState/Action, маппинг id → иконка/подпись
app/           Application, Koin-модули
```
- `domain` не импортирует `android.*`, `androidx.*`, `kotlinx.serialization`, `data`, `presentation`.
  Допустим только `kotlinx.coroutines.flow` в интерфейсах репозиториев.
- Меняет игру только `GameEngine` (`(GameSnapshot, команда) → Transition`). Репозиторий: `Mutex` → движок → запись → публикация.
  Составную операцию (снять из копилки + купить) делать **одной** командой движка, а не двумя вызовами из VM.
- Формат файла — DTO в `data/game/store`, не доменные модели. Сменил смысл/имя поля или enum — подними `GAME_SAVE_VERSION`.
- Koin: передавать в конструктор только то, что класс использует. Новый класс — `single`/`viewModel` в `appModule`,
  то, что требует `Context`, — в `storageModule`/`assetsModule` (иначе ломается `AppModuleTest`).
- Не вводить интерфейс с одной реализацией, фабрики, «на будущее» (исключение — `GameStore`: фейк в тестах).

## 2. ViewModel и UiState

- `private val _state = MutableStateFlow(...)`, `val state = _state.asStateFlow()`; экран — `collectAsStateWithLifecycle()`.
- Вход один: `fun onAction(action: XAction)`; внутри `when` — вызовы приватных функций, по одной на действие.
- `XUiState.kt` и `XAction.kt` — отдельные файлы, не в файле VM и не в файле экрана.
- `XScreen(viewModel = koinViewModel())` берёт VM и колбэки навигации; `XScreenContent(state, onAction)` — stateless + `@Preview`.
- Навигация и прочие разовые события из VM — `Channel` → `receiveAsFlow()` → `ObserveAsEvents` в экране.
  **Никогда** не передавать в VM лямбду навигации (`confirm(onBack)`): после поворота она указывает на старый back stack.
- UiState хранит доменные модели как есть (`pet: Pet`), а не копии их полей.
- Вся арифметика и решения — во VM или домене. В composable — только подстановка готовых значений
  (плохо: `goal.cost - savings` в экране; хорошо: `state.goalRemaining`).
- Черновики ввода, которые влияют на правила (суммы, клампы по `GameRules`), — во VM. Чисто визуальное
  (раскрыта ли карточка) — в `rememberSaveable` экрана.
- Тексты для ребёнка во VM не пишутся: VM отдаёт enum/факт, слова подбирает экран (`FeedbackKey` + `LocalFeedback`).
- Ошибки хранения: `result.orSnackbar { retry() }` (см. память проекта «Persistence design»), без отдельного error-StateFlow.

## 3. Compose и Material 3 (дизайн не меняем — меняем компоненты)

| Что это | Чем делать | Не делать |
|---|---|---|
| Кнопка с текстом | `Button` / `FilledTonalButton` / `TextButton` с `colors`, `shape`, `contentPadding`, `Modifier.height(..)` под дизайн | `Surface(onClick)`, `Text.clickable`, `Row.clickable` |
| Кнопка-иконка | `IconButton` / `FilledIconButton` (`Modifier.size(48.dp)`, `shape = CircleShape`), `contentDescription` на иконке | `Surface(onClick) { Image(...) }` + `clearAndSetSemantics` |
| Один из нескольких (вкладка, вариант ответа, раскраска, сдача) | `Surface(selected, onClick)` или `Modifier.selectable(selected, role = Role.RadioButton/Tab)`; у родителя `Modifier.selectableGroup()` | `clickable` + ручной `semantics { selected = … }` |
| Вкл/выкл, флажок (корзина, гардероб, настройка) | `Surface(checked, onCheckedChange)` или `Modifier.toggleable(value, role = Checkbox/Switch)`; внутренний `Checkbox/Switch(onCheckedChange = null)` | две цели нажатия: `Surface(onClick)` + `Checkbox(onCheckedChange)` |
| Кликабельная карточка (товар, цель, задание) | `Surface(onClick)` / `Card(onClick)` | — |
| Составной кастомный элемент без аналога в M3 (пункт меню-наклейка, солнышко недели, облачко «…») | `Modifier.clickable(role = Role.Button, onClickLabel = …)` — допустимое исключение | — |

- Кремовый стиль сохраняем модификатором: `Modifier.creamCard(shape)` на кнопке + прозрачный `containerColor`
  и `elevation = null` у `Button`, чтобы не было двойной тени.
- Выключенное состояние — через `enabled` и `disabled*Color`, не через `Modifier.alpha()`.
- Сначала ищи готовое в `components/StickerKit.kt` (`TealButton`, `SoftButton`, `PillButton`, `PebbleButton`,
  `BackButton`, `StepButton`, `ScreenHeader`, `CardDialog`, `StatRow`…). Не заводи локальный дубль (`StepButton` в TaskPlay — ошибка).
- «Назад» — везде `ScreenHeader`/`BackButton` (ТЗ 3.6: единообразное расположение). Не `TextButton("← Назад")`, не «‹».
- Нажимаемое ≥ 48×48 dp; основной текст ≥ 16 sp через `MaterialTheme.typography`, не литералы `fontSize`.
  Под шрифт 200 % — `heightIn(min = …)`, а не фиксированный `height` у элементов с текстом.
- Цвет не единственный сигнал: иконка или слово рядом. `contentDescription` у значимых картинок, `null` у декоративных.
- Lifecycle: тяжёлые ресурсы (Filament, `SoundPool`, `AudioRecord`, `Choreographer`) останавливать при `ON_STOP`
  (`LocalLifecycleOwner.current.lifecycle.currentStateAsState()`), а не только при уходе с экрана.
- Ввод пользователя в полях/виджетах заданий — `rememberSaveable`, иначе теряется при повороте.

## 4. Ограничения ТЗ, которые нельзя сломать

- Разрешения: ребёнок **никогда** не видит системного запроса. `RECORD_AUDIO` запрашивается только из раздела
  взрослого (за барьером-примером), функция по умолчанию выключена. Новые разрешения — не добавлять.
- `minSdk = 26` (Android 8.0). Нет сети, рекламы, аналитики, внешних ссылок у ребёнка.
- Питомец не болеет, не травмируется, не «грустит»: минимум — «скучает». Никаких «ударов», визгов, стыдящих слов
  («плохо», «виноват», «неправильно» в адрес ребёнка). Ошибка = что случилось + как исправить.
- Минимумы контента (ТЗ 2.6): 9 раскрасок, ≥ 8 товаров двух категорий, ≥ 3 цели, ≥ 6 заданий по 3 темам,
  3 стадии, 5 недель подряд в демо. Проверяет `ContentMinimumsTest` — не удалять и не ослаблять.
- Демо-режим: неделя закрывается в тот же день, лимит заданий в неделю не действует.
- Баланс и копилка не меняются без `LedgerEntry` с причиной.

## 5. Числа и тексты

- Числа экономики — только `GameRules`, пороги `PetSatiety`/`PetMood`/`PetGrowthStage`, данные контента.
  Никаких «50», «35», «+25», «100 монет» литералом в тексте экрана — подставляй константу (текст о демо уже врал: 100 вместо 50).
- Фразы ребёнку от домена/VM — через `FeedbackKey` → `assets/content/feedback.json` (новый ключ = строка в JSON,
  иначе `FeedbackContentTest` упадёт). Подписи кнопок и заголовки — в `presentation`.
- Три слова решений одинаковы везде: «Обязательное», «Необязательное», «Копилка».

## 6. Тесты

- Команда: `./gradlew :app:testDebugUnitTest` (JUnit4, JVM, без эмулятора). Тесты в `app/src/test/java/ru/larpinovplay/finniapp/`.
- Новое правило в `GameEngine`/модели — тест в `GameEngineTest`/`PetTest` на успешный и отклонённый путь.
- Новое действие VM — тест в `XViewModelTest` (фейки `InMemoryGameStore`, `InMemorySettingsRepository`, сбои — `StorageTestSupport`).
- Новое задание/приключение — прогнать `ContentMinimumsTest`, `TaskContentTest`/`AdventureContentTest`:
  у каждого задания есть верный и ошибочный путь с объяснением.
- Изменил формат сохранения — `GameSaveMapperTest`, `DataStoreGameStoreTest`.
- Не писать тесты на фейки и на тривиальные геттеры.

## 7. Документация

Код и `docs/` меняются в одном PR. Куда писать:
- числа экономики → `docs/04-rules-and-formulas.md` (+ `docs/11-economy.md`);
- модели/команды → `docs/02`; процессы → `docs/03`; контент → `docs/05`; архитектура/DI/хранение → `docs/06`;
  экраны/навигация → `docs/07`; статус требования → `docs/08`; тест-кейсы → `docs/09`;
- разрешения и данные → `docs/permissions-and-data.md`; ассеты/библиотеки → `docs/licenses.md`.

## 8. Перед тем как сказать «готово»

1. `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` — зелёные.
2. Заметная правка UI — прогон на эмуляторе (портрет 360 dp), скриншот до/после; иначе достаточно сборки и тестов.
3. `grep -rn "\.clickable(" app/src/main/java/.../presentation` — только исключения из таблицы раздела 3.
4. Нет новых литералов экономики в текстах, нет новых разрешений в манифесте.
5. `docs/` обновлены, `docs/08` — статус пункта ТЗ.
