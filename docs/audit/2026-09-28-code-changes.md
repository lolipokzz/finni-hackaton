# Каталог правок: места в коде и примеры — 2026-09-28

Приложение к [плану](2026-09-28-full-code-check.md): по каждой задаче — файл и строка, что там сейчас и на что
заменить. ID совпадают с планом. Номера строк — на коммит `a39817e`; после первых правок сверяться по тексту фрагмента.

Пути сокращены: `…/` = `app/src/main/java/ru/larpinovplay/finniapp/`, тесты — `app/src/test/java/ru/larpinovplay/finniapp/`.

Содержание: [P0](#p0) · [тесты](#tests) · [P1](#p1) · [Compose-компоненты (B7)](#b7) · [P2](#p2) · [удаления (B10)](#b10)

---

<a id="p0"></a>
## P0

### A1. Микрофон выключен по умолчанию, запрос — только у взрослого

**`…/domain/settings/model/AppSettings.kt:9`** и **`…/data/settings/SettingsSaveFile.kt:22`**
```kotlin
// сейчас
val voiceRepeatEnabled: Boolean = true,
// нужно
val voiceRepeatEnabled: Boolean = false,
```

**`…/presentation/screens/home/HomeScreen.kt:478–489`** — запрос разрешения у ребёнка
```kotlin
// сейчас
val wantsVoice = state.voiceRepeatEnabled && state.soundEnabled
val micGranted = petHost != null && rememberMicrophone(ask = wantsVoice)
val spec = PetSpec(
    ...
    voiceEnabled = wantsVoice && micGranted,
// нужно — PetVoice.start() сам проверяет разрешение и без него молчит
val spec = PetSpec(
    ...
    voiceEnabled = state.voiceRepeatEnabled && state.soundEnabled,
```
Удалить функцию `rememberMicrophone` (**`HomeScreen.kt:556–571`**) и ставшие ненужными импорты `Manifest`,
`PackageManager`, `ContextCompat`, `rememberLauncherForActivityResult`, `ActivityResultContracts`, `LocalContext`.

**`…/presentation/screens/adult/AdultScreen.kt:85`**
```kotlin
// сейчас
PreferenceSwitch("Кот повторяет слова (микрофон)", state.settings.voiceRepeatEnabled, !state.busy, viewModel::setVoiceRepeat)
// нужно (в начале AdultScreen)
val context = LocalContext.current
val micRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    if (granted) viewModel.setVoiceRepeat(true)
}
...
PreferenceSwitch("Кот повторяет слова (микрофон)", state.settings.voiceRepeatEnabled, !state.busy) { enabled ->
    when {
        !enabled -> viewModel.setVoiceRepeat(false)
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED -> viewModel.setVoiceRepeat(true)
        else -> micRequest.launch(Manifest.permission.RECORD_AUDIO)
    }
}
Text("Звук обрабатывается только на телефоне, не сохраняется и никуда не отправляется.")
```

**`app/src/main/AndroidManifest.xml:5`** — комментарий: «…включается только взрослым в защищённом разделе».

### A2. Питомец засыпает при сворачивании

**`…/presentation/components/PetHost.kt:208`**
```kotlin
// сейчас
active = state.shown,
// нужно
active = state.shown && started,
```
выше, в теле `PetHost` (после `if (!ready) return`):
```kotlin
val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
val started = lifecycleState.isAtLeast(Lifecycle.State.STARTED)
```
импорты: `androidx.lifecycle.Lifecycle`, `androidx.lifecycle.compose.LocalLifecycleOwner`,
`androidx.lifecycle.compose.currentStateAsState`.

### A3. minSdk

**`app/build.gradle.kts:18`**: `minSdk = 27` → `minSdk = 26`.

### A4. Шестое задание

**`…/data/content/TaskContent.kt:101`** — после задания `payments_shop_50`, перед закрывающей `)` списка, добавить
элемент `payments_two_prices` (полный код — в плане, задача A4).

### A5. Демо снимает недельный лимит заданий

**`…/domain/game/model/GameState.kt:95`**
```kotlin
// сейчас
tasksDoneThisWeek >= GameRules.TASKS_PER_WEEK -> TaskStatus.LIMIT_REACHED
// нужно
!demoMode && tasksDoneThisWeek >= GameRules.TASKS_PER_WEEK -> TaskStatus.LIMIT_REACHED
```
**`GameState.kt:52`** — удалить `val tasksPerWeek: Int get() = GameRules.TASKS_PER_WEEK`.

**`…/presentation/screens/tasks/TasksUiState.kt:10`**: `val perWeek: Int,` → `val perWeek: Int?,   // null — демо, лимита нет`

**`…/presentation/screens/tasks/TasksViewModel.kt:35`**
```kotlin
// сейчас
perWeek = game.tasksPerWeek,
// нужно
perWeek = GameRules.TASKS_PER_WEEK.takeUnless { game.demoMode },
```
**`…/presentation/screens/tasks/TasksScreen.kt:116`**
```kotlin
// сейчас
WeekCounter(state.doneThisWeek, state.perWeek)
// нужно
state.perWeek?.let { WeekCounter(state.doneThisWeek, it) }
```
Тест в `GameEngineTest.kt`:
```kotlin
@Test
fun demoLiftsWeeklyTaskLimit() {
    var game = newGame().let { it.copy(state = it.state.copy(demoMode = true)) }
    tasks.take(GameRules.TASKS_PER_WEEK).forEach { game = GameEngine.answerTask(game, it, rightAnswer(it)).game }
    assertEquals(TaskStatus.AVAILABLE, game.state.taskStatus(tasks[GameRules.TASKS_PER_WEEK]))
}
```
(`newGame()`, `tasks`, `rightAnswer` — хелперы теста; если их нет — завести по образцу
`answeringTaskPaysRewardOnceAndLimitsTasksPerWeek`.)

### A6. Правдивое описание демо

**`…/presentation/screens/adult/AdultScreen.kt:88`** и **`:112`**
```kotlin
// сейчас
Text("Тестовый питомец, 100 монет, первая неделя. …")
AdultConfirmation.DEMO -> "… тестовым профилем: Финни Демо, 100 монет, неделя 1."
// нужно
Text("Тестовый питомец, ${GameRules.START_BALANCE} монет, первая неделя. …")
AdultConfirmation.DEMO -> "… тестовым профилем: Финни Демо, ${GameRules.START_BALANCE} монет, неделя 1."
```

---

<a id="tests"></a>
## Фаза 2 — новые тесты (B11)

**`ContentMinimumsTest.kt`** (новый)
```kotlin
class ContentMinimumsTest {
    private val content = defaultContent()

    @Test
    fun contentMeetsTzMinimums() {   // ТЗ 2.6
        assertTrue(PetColor.entries.size >= 9)
        assertTrue(content.shopItems.size >= 8)
        assertEquals(ShopCategory.entries.toSet(), content.shopItems.map { it.category }.toSet())
        assertTrue(content.goals.size >= 3)
        assertTrue(content.tasks.size >= 6)
        assertEquals(TaskTopic.entries.toSet(), content.tasks.map { it.topic }.toSet())
        assertTrue(PetGrowthStage.entries.size >= 3)
        assertEquals(PetGrowthStage.entries.sortedBy { it.minGrowthPoints }, PetGrowthStage.entries)
    }

    @Test
    fun idsAreUniqueAndTasksAreFair() {
        listOf(content.shopItems.map { it.id }, content.goals.map { it.id }, content.tasks.map { it.id })
            .forEach { ids -> assertEquals(ids.size, ids.toSet().size) }
        content.tasks.forEach { task ->
            assertTrue(task.id, task.reward > task.rewardOnMistake && task.rewardOnMistake > 0)
            assertTrue(task.id, task.explanationSuccess.isNotBlank() && task.explanationMistake.isNotBlank())
        }
    }
}
```

**`TaskContentTest.kt`** (новый) — каждое задание проходится верно и ошибочно:
```kotlin
class TaskContentTest {
    @Test
    fun everyTaskHasRightAndWrongPath() {
        defaultContent().tasks.forEach { task ->
            val right = task.evaluate(task.rightAnswer())
            val wrong = task.evaluate(task.wrongAnswer())
            assertTrue(task.id, right.success)
            assertEquals(task.id, task.reward, right.reward)
            assertFalse(task.id, wrong.success)
            assertEquals(task.id, task.rewardOnMistake, wrong.reward)
        }
    }

    private fun Task.rightAnswer(): TaskAnswer = when (val p = payload) {
        is TaskPayload.Choice -> TaskAnswer.Choice(p.options.first { it.correct }.id)
        is TaskPayload.Allocate -> TaskAnswer.Allocation(
            mapOf("mandatory" to p.mandatoryMin, "savings" to p.savingsMin, "optional" to p.total - p.mandatoryMin - p.savingsMin)
        )
        is TaskPayload.ShopList -> TaskAnswer.Selection(p.items.filter { it.mandatory }.map { it.id }.toSet())
    }

    private fun Task.wrongAnswer(): TaskAnswer = when (val p = payload) {
        is TaskPayload.Choice -> TaskAnswer.Choice(p.options.first { !it.correct }.id)
        is TaskPayload.Allocate -> TaskAnswer.Allocation(mapOf("optional" to p.total))
        is TaskPayload.ShopList -> TaskAnswer.Selection(p.items.filterNot { it.mandatory }.map { it.id }.toSet())
    }
}
```

**`DemoWeeksTest.kt`** (новый) — 5 недель в демо за один день:
```kotlin
class DemoWeeksTest {
    @Test
    fun demoPlaysFiveWeeksInOneDay() = runBlocking {
        val game = GameRepositoryImpl(FakeGameStore(), clock = TestClock(), adventures = defaultAdventures)
        game.resetToDemo()
        repeat(5) {
            val state = game.requireSnapshot().state
            game.confirmPlan(BudgetPlan(savings = state.balance))
            state.adventureOfWeek(defaultAdventures)?.let { game.completeAdventure(it, mistakes = 0) }
            val result = (game.finishWeek() as Result.Success).data
            assertTrue("неделя ${state.week}: $result", result is FinishWeekResult.Finished)
        }
        assertEquals(6, game.requireSnapshot().state.week)
        assertEquals(5, game.requireSnapshot().state.history.size)
    }
}
```

**`TaskPlayViewModelTest.kt`** (новый)
```kotlin
class TaskPlayViewModelTest {
    private val store = FakeGameStore()
    private val game = GameRepositoryImpl(store, clock = TestClock())
    private val task = defaultContent().tasks.first { it.payload is TaskPayload.Choice }
    private val right = TaskAnswer.Choice((task.payload as TaskPayload.Choice).options.first { it.correct }.id)

    @Before fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun resetMain() = Dispatchers.resetMain()

    @Test
    fun doubleSubmitCountsOnce() = runBlocking {
        game.createPet(Pet.newborn("Финни", PetLook(PetColor.CORAL)))
        val vm = TaskPlayViewModel(task.id, game, defaultContent())
        vm.onAction(TaskPlayAction.Submit(right))
        vm.onAction(TaskPlayAction.Submit(right))
        assertEquals(1, game.requireSnapshot().state.taskResults.size)
    }

    @Test
    fun failedSaveKeepsTaskOpenForRetry() = runBlocking {
        game.createPet(Pet.newborn("Финни", PetLook(PetColor.CORAL)))
        val vm = TaskPlayViewModel(task.id, game, defaultContent())
        store.saveFailure = StorageError.WRITE_FAILED   // не «исправимая»: снэкбар не ждёт показа
        vm.onAction(TaskPlayAction.Submit(right))
        store.saveFailure = null
        vm.onAction(TaskPlayAction.Submit(right))
        assertEquals(1, game.requireSnapshot().state.taskResults.size)
    }
}
```

**`ShopRulesTest.kt`** (новый)
```kotlin
@Test fun weekOfFoodCostsTwoVegetables() = assertEquals(20, cheapestFoodFor(GameRules.WEEKLY_HUNGER, defaultContent().shopItems))
@Test fun noFoodNoPrice() = assertNull(cheapestFoodFor(35, emptyList()))
```

---

<a id="p1"></a>
## P1

### B1. Подпись релиза — `app/build.gradle.kts`
Сейчас блока `signingConfigs` нет, `buildTypes.release` без `signingConfig`. Код — в плане, B1.

### B2. `app/src/main/res/values/strings.xml:2`
`<string name="app_name">FinniApp</string>` → `<string name="app_name">Питомец Финни</string>`.

### B3. Главный экран: накопления, цель, задание [решение владельца]

**`…/presentation/screens/home/HomeUiState.kt`** — добавить
```kotlin
val activeTask: String? = null,   // приключение недели или первое доступное задание; null — ничего не ждёт
```
**`…/presentation/screens/home/HomeViewModel.kt`**, `toUiState`:
```kotlin
activeTask = game.adventureOfWeek(content.adventures)?.title ?: game.availableTasks(content.tasks).firstOrNull()?.title,
```
**`…/presentation/screens/home/HomeScreen.kt:581`, `:592–616`** (`BottomMenu`):
```kotlin
// сейчас
MenuItem("Задания", Modifier.weight(1f), badge = state.tasksBadge, ...)
MenuItem("Копилка", Modifier.weight(1f), description = ..., ...)
// нужно
MenuItem(state.activeTask ?: "Задания", Modifier.weight(1f), badge = state.tasksBadge, ...)
MenuItem(goal?.name ?: "Копилка", Modifier.weight(1f), badgeText = goal?.let { "${state.savings}/${it.cost}" }, ...)
```
`MenuItem`: параметр `badgeText: String? = null` рядом с `badge: Int`; подпись — `maxLines = 1`,
`overflow = TextOverflow.Ellipsis`, ширина ячейки (убрать `softWrap = false` + `wrapContentWidth(unbounded = true)`,
иначе длинное имя цели наедет на соседей).

### B4. Единая кнопка «Назад»

**`…/presentation/screens/adult/AdultScreen.kt:33–34`**
```kotlin
// сейчас
TextButton(onClick = onBack, enabled = !state.busy) { Text("← Назад") }
Text("Для взрослых", style = MaterialTheme.typography.headlineMedium)
// нужно
ScreenHeader("Для взрослых", onBack = { if (!state.busy) onBack() })
```
**`…/presentation/screens/tasks/TaskPlayScreen.kt:86–99`**
```kotlin
// сейчас
Row(...) {
    Surface(onClick = onBack, shape = CircleShape, color = FinniColors.Lavender, modifier = Modifier.size(48.dp)) {
        Box(contentAlignment = Alignment.Center) { Text("‹", ...) }   // TalkBack читает «‹»
    }
    Text(task.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
}
// нужно
ScreenHeader(task.title, onBack, Modifier.padding(top = 8.dp))
```

### B5. AdultViewModel

**`…/presentation/screens/adult/AdultViewModel.kt:21–35`** — `AdultConfirmation`, `AdultUiState` перенести в
`AdultUiState.kt`; `first`/`second` с `Random.nextInt` в значениях по умолчанию заменить:
```kotlin
data class AdultUiState(val example: Example, val answer: String = "", ...)
data class Example(val first: Int, val second: Int) { val sum get() = first + second }
```
```kotlin
class AdultViewModel(..., private val random: Random = Random.Default) : ViewModel() {
    private val _state = MutableStateFlow(AdultUiState(example = newExample()))
    private fun newExample() = Example(random.nextInt(10, 50), random.nextInt(10, 50))
```
**`AdultViewModel.kt:88–113`** — навигация лямбдой
```kotlin
// сейчас
fun confirm(onComplete: () -> Unit) { ... is Result.Success -> { ...; onComplete() } }
// нужно
private val _events = Channel<AdultEvent>()
val events = _events.receiveAsFlow()
private fun confirm() { ... is Result.Success -> { _state.update { ... }; _events.send(AdultEvent.Done) } }
```
**`AdultScreen.kt:118`**
```kotlin
// сейчас
confirmButton = { TextButton(onClick = { viewModel.confirm(onBack) }, ...) }
// нужно
ObserveAsEvents(viewModel.events) { if (it == AdultEvent.Done) onBack() }   // в начале AdultScreen
confirmButton = { TextButton(onClick = { onAction(AdultAction.Confirm) }, ...) }
```
**`AdultViewModel.kt:65–67`** — строки ошибок во VM → `error: AdultError?` (`NEW_EXAMPLE`, `WRONG_ANSWER`,
`ACTION_FAILED`, строка `:109`), слова — в `AdultScreen` через `when`.

### B6. «Удары» [решение владельца, вариант по умолчанию]

**`…/presentation/components/PetModel3D.kt:452–463`** (`onTap`)
```kotlin
// сейчас
val index = when (val zone = ...) {
    TapZone.HEAD -> headIndex
    TapZone.FOOT_LEFT -> footLeftIndex
    TapZone.FOOT_RIGHT -> footRightIndex
    null -> -1
}.takeIf { it >= 0 } ?: tapIndex
...
index == headIndex -> sounds?.hitHead()
index == footLeftIndex || index == footRightIndex -> sounds?.hitFoot()
// нужно
val index = tapIndex
if (index >= 0) switchTo(index, System.nanoTime(), crossFade = true)
sounds?.cancelEffects()
```
Удалить: `tapZone`, `TapZone`, `headIndex/footLeftIndex/footRightIndex`, `PetHitAnimations` (+ параметр `hitAnimations`
в `PetModel3D`, `PetSpec`, `PetHost`), `PetHits` (**`…/presentation/pet/PetAssets.kt:86–87`**),
`hitAnimations = PetHits` (**`HomeScreen.kt:492`**), `PetSounds.hitHead/hitFoot/boing`, файлы
`res/raw/pet_hit_head.wav`, `pet_hit_foot.wav`, `pet_boing.wav`.

<a id="b7"></a>
### B7. Кнопки и выбор — компоненты Material 3

Во всех примерах внешний вид прежний: те же цвета, форма, высота, отступы.

**`…/presentation/components/StickerKit.kt:139–148` — `PillButton`**
```kotlin
// сейчас
Surface(onClick = onClick, shape = CircleShape, color = color, modifier = modifier.height(48.dp)) {
    Row(Modifier.padding(horizontal = 16.dp), ...) { Text(text, color = ink, ...); if (arrow) Image(...) }
}
// нужно
Button(
    onClick = onClick,
    shape = CircleShape,
    colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = ink),
    contentPadding = PaddingValues(horizontal = 16.dp),
    modifier = modifier.height(48.dp),
) {
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    if (arrow) Image(painterResource(R.drawable.ic_arrow_right), null, Modifier.padding(start = 6.dp).size(16.dp), colorFilter = ColorFilter.tint(ink))
}
```

**`StickerKit.kt:223–236` — `TealButton`**
```kotlin
// нужно
Button(
    onClick = onClick,
    enabled = enabled,
    shape = CircleShape,
    colors = ButtonDefaults.buttonColors(
        containerColor = FinniColors.Teal, contentColor = Color.White,
        disabledContainerColor = FinniColors.Pebble, disabledContentColor = FinniColors.InkMuted,
    ),
    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 6.dp, disabledElevation = 0.dp),
    modifier = modifier.fillMaxWidth().height(56.dp),
) {
    icon?.let { Image(painterResource(it), null, Modifier.padding(end = 10.dp).size(24.dp).alpha(if (enabled) 1f else 0.5f)) }
    Text(text, fontSize = 17.sp, fontWeight = FontWeight.Black)
}
```

**`StickerKit.kt:337–342` — `SoftButton`**: то же, `containerColor = FinniColors.Pebble`, `contentColor = FinniColors.InkMuted`,
`modifier.fillMaxWidth().height(52.dp)`, `Text(text, fontSize = 16.sp, fontWeight = FontWeight.Black)`.

**`StickerKit.kt:153–161` — `PebbleButton`**
```kotlin
// сейчас
Surface(onClick = onClick, shape = CircleShape, color = color,
    modifier = modifier.size(48.dp).semantics { contentDescription = description }) {
    Box(contentAlignment = Alignment.Center) { Image(painterResource(icon), null, Modifier.size(22.dp)) }
}
// нужно
FilledIconButton(
    onClick = onClick,
    shape = CircleShape,
    colors = IconButtonDefaults.filledIconButtonColors(containerColor = color),
    modifier = modifier.size(48.dp),
) { Image(painterResource(icon), contentDescription = description, modifier = Modifier.size(22.dp)) }
```

**`StickerKit.kt:318–332` — `BackButton`**
```kotlin
// нужно
IconButton(onClick = onClick, modifier = modifier.size(48.dp).creamCard(CircleShape, elevation = 8.dp, border = 3.dp)) {
    Image(
        painterResource(R.drawable.ic_arrow_right), contentDescription = "Назад",
        modifier = Modifier.size(22.dp).graphicsLayer(scaleX = -1f), colorFilter = ColorFilter.tint(FinniColors.Ink),
    )
}
```

**`StickerKit.kt:360–371` — `StepButton`** (убрать `.alpha()`, выключенное — цветами)
```kotlin
// нужно
FilledIconButton(
    onClick = onClick,
    enabled = enabled,
    shape = CircleShape,
    colors = IconButtonDefaults.filledIconButtonColors(
        containerColor = FinniColors.Pebble, contentColor = FinniColors.Ink,
        disabledContainerColor = FinniColors.Pebble.copy(alpha = 0.4f), disabledContentColor = FinniColors.Ink.copy(alpha = 0.4f),
    ),
    modifier = Modifier.size(48.dp).semantics { contentDescription = description },
) { Text(symbol, fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.clearAndSetSemantics {}) }
```

**`StickerKit.kt:443–454` — `CoachNote`, «Пропустить шаг»**
```kotlin
// сейчас
Text("Пропустить шаг", ..., modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp).clip(CircleShape)
    .clickable(role = Role.Button, onClick = it).padding(horizontal = 8.dp, vertical = 14.dp))
// нужно
TextButton(
    onClick = it,
    colors = ButtonDefaults.textButtonColors(contentColor = FinniColors.InkMuted),
    contentPadding = PaddingValues(horizontal = 8.dp),
    modifier = Modifier.align(Alignment.End),
) { Text("Пропустить шаг", fontSize = 13.sp, fontWeight = FontWeight.Black) }
```

**`StickerKit.kt:480–491` — `FinniSnackbar`, кнопка действия**: так же `TextButton(onClick = data::performAction,
colors = textButtonColors(contentColor = FinniColors.Teal), modifier = Modifier.padding(start = 8.dp)) { Text(label, fontSize = 15.sp, fontWeight = FontWeight.Black) }`.

**`StickerKit.kt:108`, `:311` — `MeterRing`, `CoinPill`**: оставить `Surface(onClick)`, в `semantics { }` добавить
`role = Role.Button` (только когда `onClick != null`).

**`…/presentation/screens/home/HomeScreen.kt:331–345` — `PetButton`**
```kotlin
// сейчас
Surface(onClick = onClick, shape = CircleShape, color = Color.Transparent,
    modifier = Modifier.size(48.dp).creamCard(...).clearAndSetSemantics { contentDescription = "${pet.name}: рост и прогресс" }) {
    Box(contentAlignment = Alignment.Center) { Image(painterResource(R.drawable.ic_paw), null, ...) }
}
// нужно
IconButton(onClick = onClick, modifier = Modifier.size(48.dp).creamCard(CircleShape, elevation = 8.dp, border = 3.dp)) {
    Image(
        painterResource(R.drawable.ic_paw), contentDescription = "${pet.name}: рост и прогресс",
        modifier = Modifier.size(24.dp), colorFilter = ColorFilter.tint(FinniColors.TealBright),
    )
}
```
**`HomeScreen.kt:349–370` — `PlanButton`**: то же, модификатор
`Modifier.padding(top = 6.dp).then(modifier).size(48.dp).creamCard(...).background(Color(0xFFE6EEFF))`,
`contentDescription = "План недели"` у `Image`; ручные `role = Role.Button` и `clearAndSetSemantics` убрать.

**`HomeScreen.kt:441–457` — ссылка в `SpeechBubble`**
```kotlin
// сейчас
Row(Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button) { onAction(action) }.padding(end = 6.dp), ...) {
    Text(label, ..., color = FinniColors.Teal); Image(ic_arrow_right, ...)
}
// нужно
TextButton(
    onClick = { onAction(action) },
    colors = ButtonDefaults.textButtonColors(contentColor = FinniColors.Teal),
    contentPadding = PaddingValues(start = 0.dp, end = 6.dp),
) {
    Text(label, fontSize = 15.sp, fontWeight = FontWeight.Black)
    Image(painterResource(R.drawable.ic_arrow_right), null, Modifier.padding(start = 4.dp).size(16.dp), colorFilter = ColorFilter.tint(FinniColors.Teal))
}
```
`MenuItem` (`:631`), `TypingBubble` (`:537`), `WeekSun` (`:677`) — оставить: составные элементы без аналога в M3.

**`…/presentation/screens/petcreation/PetCreationScreen.kt:240–251` — «Пропустить»**
```kotlin
// нужно
TextButton(
    onClick = onSkip,
    colors = ButtonDefaults.textButtonColors(containerColor = FinniColors.Cream.copy(alpha = 0.85f), contentColor = FinniColors.Ink),
    modifier = Modifier.align(Alignment.CenterEnd),
) { Text("Пропустить", fontSize = 14.sp, fontWeight = FontWeight.Black) }
```
**`PetCreationScreen.kt:319–335` — раскраски**
```kotlin
// сейчас
Column(...) {
    ... Box(Modifier ... .clickableButton { onSelect(color) }
        .semantics { contentDescription = "Раскраска: ${color.title}"; this.selected = isSelected })
// нужно
Column(Modifier.fillMaxWidth().selectableGroup(), ...) {
    ... Box(Modifier ... .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(color) })
        .semantics { contentDescription = "Раскраска: ${color.title}" })
```
После обеих правок удалить `Modifier.clickableButton` (**`:254`**).

**`…/presentation/screens/shop/ShopScreen.kt:223–245` — вкладки**
```kotlin
// сейчас
Row(Modifier...creamCard(...)...) {
    Surface(onClick = { onSelect(category) }, ..., modifier = Modifier.weight(1f).height(52.dp)
        .semantics { role = Role.Tab; this.selected = isSelected })
// нужно
Row(Modifier...creamCard(...).selectableGroup()...) {
    Surface(selected = isSelected, onClick = { onSelect(category) }, ..., modifier = Modifier.weight(1f).height(52.dp)
        .semantics { role = Role.Tab })
```

**`…/presentation/screens/tasks/TaskPlayScreen.kt:120–135` — варианты ответа**
```kotlin
// сейчас
Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Surface(onClick = { selected = option.id }, ...)
// нужно
Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Surface(selected = isSelected, onClick = { selected = option.id }, ...,
        modifier = Modifier.fillMaxWidth().shadow(...).semantics { role = Role.RadioButton })
```
**`TaskPlayScreen.kt:177–187`** — локальный `StepButton` удалить, вызывать `StepButton` из `StickerKit`
(`StepButton("−", "Меньше: ${bucket.label}", enabled = value > 0) { … }`).

**`…/presentation/screens/tasks/TasksScreen.kt:396–409` — `ShopListRow`** (сейчас две цели нажатия)
```kotlin
// сейчас
Surface(onClick = onToggle, ...) { Row(...) { Checkbox(checked = checked, onCheckedChange = { onToggle() }) ... } }
// нужно
Surface(checked = checked, onCheckedChange = { onToggle() }, ..., modifier = Modifier.semantics { role = Role.Checkbox }) {
    Row(...) { Checkbox(checked = checked, onCheckedChange = null) ... }
}
```
`ShopListRow` и `HintButton` (**`TasksScreen.kt:394–418`**) используются только в `TaskPlayScreen` — перенести туда `private`.

**`…/presentation/screens/adventure/AdventureScreen.kt:284–300` — монеты сдачи** и **`:334–345` — варианты ответа**
```kotlin
// сейчас
Surface(onClick = { onAction(AdventureAction.ChooseChange(option)) }, enabled = check == null, ...,
    modifier = Modifier...semantics { contentDescription = "Сдача $option"; selected = chosen })
// нужно
Surface(selected = chosen, onClick = { onAction(AdventureAction.ChooseChange(option)) }, enabled = check == null, ...,
    modifier = Modifier...semantics { contentDescription = "Сдача $option"; role = Role.RadioButton })
```
то же для `ChooseOption`; у родительских `Row`/`Column` — `Modifier.selectableGroup()`. `.alpha(...)` для
невыбранных после ответа оставить: это приглушение, а не замена `enabled`.

**`…/presentation/screens/adventure/AdventureScreen.kt:247–257` — `Hint`**: `Surface(onClick)` → `Button` по рецепту
`PillButton` (`containerColor = Color(0xFFFFF5C9)`, `contentColor = FinniColors.CoinInk`,
`contentPadding = PaddingValues(start = 10.dp, end = 16.dp)`, `height(48.dp)`).

**`…/presentation/screens/adventure/BasketScene.kt:78–88` — товары корзины**
```kotlin
// сейчас
Surface(onClick = { onAction(AdventureAction.ToggleBasketItem(index)) }, enabled = !locked, ...,
    modifier = Modifier...semantics { role = Role.Checkbox; selected = picked })
// нужно
Surface(checked = picked, onCheckedChange = { onAction(AdventureAction.ToggleBasketItem(index)) }, enabled = !locked, ...,
    modifier = Modifier...semantics { role = Role.Checkbox })
```

**`…/presentation/screens/wardrobe/WardrobeScreen.kt:154–158` — `ItemTile`**
```kotlin
// сейчас
.clearAndSetSemantics { role = Role.Switch; selected = entry.worn; contentDescription = ... }
// нужно — у переключателя состояние «вкл/выкл», а не «выбрано»
.clearAndSetSemantics { role = Role.Switch; toggleableState = ToggleableState(entry.worn); contentDescription = ... }
```

### B8. Числа в `…/presentation/screens/home/HomeInfoDialog.kt:52–96`

```kotlin
// сейчас
"Откуда берутся: каждую неделю приходят карманные — 50, …",
"Немного сверху: 5 за задание и 5 за приключение недели. …",
"Растёт от еды из магазина: овощи +25, фрукты +30, мясо +45.",
"Каждую неделю падает на 35, …",
"Если меньше 30, Финни голоден. …",
"За неделю настроение падает на 15, …",
"Радостный от 70, спокойный от 40, ниже — скучает. …",
// нужно
"Откуда берутся: каждую неделю приходят карманные — ${GameRules.weekIncome(PetGrowthStage.BABY)}, …",
"Немного сверху: ${state.taskReward} за задание и ${state.adventureReward} за приключение недели. …",
"Растёт от еды из магазина: " + state.foods.joinToString { (name, satiety) -> "${name.lowercase()} +$satiety" } + ".",
"Каждую неделю падает на ${GameRules.WEEKLY_HUNGER}, …",
"Если меньше ${PetSatiety.HUNGRY_BELOW}, Финни голоден. …",
"За неделю настроение падает на ${GameRules.WEEKLY_MOOD_DECAY}, …",
"Радостный от ${PetMood.HAPPY_FROM}, спокойный от ${PetMood.CALM_FROM}, ниже — скучает. …",
```
`HomeUiState` + `HomeViewModel.toUiState`:
```kotlin
foods = content.shopItems.filter { it.satiety > 0 }.map { it.name to it.satiety },
taskReward = content.tasks.maxOf { it.reward },
adventureReward = content.adventures.maxOf { it.reward },
```

### B9. «Взять из копилки и купить» одной командой

**`…/domain/game/model/GameResults.kt:6`**
```kotlin
data class Success(val item: ShopItem, val balanceAfter: Int, val fromSavings: Int = 0) : PurchaseResult
```
**`…/domain/game/engine/GameEngine.kt`** — рядом с `buy`:
```kotlin
/** Берёт из копилки ровно недостающее и покупает; если копилки не хватает — ничего не меняет. */
fun buyWithSavings(game: GameSnapshot, item: ShopItem): Transition<PurchaseResult> {
    val missing = (item.price - game.state.balance).coerceAtLeast(0)
    if (missing > game.state.savings) return Transition(game, PurchaseResult.NotEnough(missing - game.state.savings))
    val withMoney = if (missing > 0) withdraw(game, missing).game else game
    val bought = buy(withMoney, item)
    val result = bought.result as? PurchaseResult.Success ?: return Transition(game, bought.result)
    return Transition(bought.game, result.copy(fromSavings = missing))
}
```
**`…/domain/game/repository/GameRepository.kt`** — `suspend fun buyWithSavings(item: ShopItem): Result<PurchaseResult, StorageError>`;
**`…/data/game/GameRepositoryImpl.kt`** — `= execute { GameEngine.buyWithSavings(it, item) }`.

**`…/presentation/screens/shop/ShopViewModel.kt:106–116`**
```kotlin
// сейчас
val missing = (item.price - game.requireSnapshot().state.balance).coerceAtLeast(0)
val withdrawn = missing == 0 || game.withdraw(missing).orSnackbar { … } == WithdrawResult.Success
val bought = if (withdrawn) game.buy(item).orSnackbar { … } as? PurchaseResult.Success else null
_state.update { it.copy(feedback = bought?.let { b -> PurchaseFeedback.Bought(item, b.balanceAfter, fromSavings = missing) }) }
// нужно
val bought = game.buyWithSavings(item).orSnackbar { buyWithSavings(notEnough) } as? PurchaseResult.Success
_state.update { it.copy(feedback = bought?.let { b -> PurchaseFeedback.Bought(item, b.balanceAfter, b.fromSavings) }) }
```

### B12. Ввод переживает поворот — `…/presentation/screens/tasks/TaskPlayScreen.kt`

```kotlin
// :121 сейчас
var selected by remember { mutableStateOf<String?>(null) }
// нужно
var selected by rememberSaveable { mutableStateOf<String?>(null) }

// :149 сейчас
var amounts by remember { mutableStateOf(p.buckets.associate { it.id to 0 }) }
// нужно
var amounts by rememberSaveable(stateSaver = mapSaver({ it }, { m -> m.mapValues { (_, v) -> v as Int } })) {
    mutableStateOf(p.buckets.associate { it.id to 0 })
}

// :196 сейчас
var selected by remember { mutableStateOf(setOf<String>()) }
// нужно
var selected by rememberSaveable(stateSaver = listSaver<Set<String>, String>({ it.toList() }, { it.toSet() })) {
    mutableStateOf(emptySet())
}
```
**`…/presentation/screens/settings/SettingsScreen.kt:111–112`**: `remember { mutableStateOf(false) }` → `rememberSaveable { mutableStateOf(false) }`.

### B13. `…/presentation/MainActivity.kt:45`
`game.snapshot.collectAsState()` → `game.snapshot.collectAsStateWithLifecycle()`; импорт
`androidx.lifecycle.compose.collectAsStateWithLifecycle` вместо `androidx.compose.runtime.collectAsState`.

---

<a id="p2"></a>
## P2

### C1. Домен без `@Serializable`

**`…/domain/task/model/TaskOutcome.kt:3–7`** — убрать `import kotlinx.serialization.Serializable` и `@Serializable`.

Новый **`…/presentation/screens/tasks/TaskOutcomeUi.kt`**:
```kotlin
@Serializable
data class TaskOutcomeUi(val success: Boolean, val reward: Int, val consequence: String?, val explanation: String)

fun TaskOutcome.toUi() = TaskOutcomeUi(success, reward, consequence, explanation)
```
**`…/presentation/navigation/Routes.kt:38`**: `val outcome: TaskOutcome` → `val outcome: TaskOutcomeUi`.
**`…/presentation/navigation/MainNavigation.kt:95`**: `TaskResult(route.taskId, outcome)` → `TaskResult(route.taskId, outcome.toUi())`.
**`…/presentation/screens/tasks/TasksScreen.kt:350–392`** — `TaskResultCard(result: TaskOutcome, …)` →
`TaskResultCard(result: TaskOutcomeUi, …)`, вынести в `tasks/TaskResultCard.kt`.

### C2. Единый `onAction`

**`…/presentation/screens/tasks/TasksViewModel.kt:42`**
```kotlin
// сейчас
fun skipTutorialStep() { ... }
// нужно (TasksAction.kt: sealed interface TasksAction { data object SkipTutorialStep : TasksAction })
fun onAction(action: TasksAction) {
    when (action) {
        TasksAction.SkipTutorialStep -> skipTutorialStep()
    }
}
private fun skipTutorialStep() { ... }
```
экран: **`TasksScreen.kt:92`** `onSkipTutorialStep = viewModel::skipTutorialStep` → `{ viewModel.onAction(TasksAction.SkipTutorialStep) }`.

**`…/presentation/screens/wardrobe/WardrobeViewModel.kt:22–33`**
```kotlin
// сейчас
val state: StateFlow<WardrobeUiState> = game.snapshot.filterNotNull().map(::toUiState)
    .stateIn(viewModelScope, SharingStarted.Eagerly, toUiState(game.requireSnapshot()))
fun onToggle(item: ShopItem) { ... }
// нужно
private val _state = MutableStateFlow(toUiState(game.requireSnapshot()))
val state = _state.asStateFlow()
init { viewModelScope.launch { game.snapshot.filterNotNull().collect { _state.value = toUiState(it) } } }
fun onAction(action: WardrobeAction) { when (action) { is WardrobeAction.Toggle -> toggle(action.item) } }
```

**`…/presentation/screens/settings/SettingsViewModel.kt:24–26`** — перезапись настроек целиком
```kotlin
// сейчас
fun onSettingsChange(settings: AppSettings) {
    viewModelScope.launch { repository.updateSettings { settings }.orSnackbar { onSettingsChange(settings) } }
}
// нужно
fun onAction(action: SettingsAction) {
    when (action) {
        is SettingsAction.SetSound -> update { it.copy(soundEnabled = action.enabled) }
        is SettingsAction.SetAnimations -> update { it.copy(animationsEnabled = action.enabled) }
        is SettingsAction.SetTips -> update { it.copy(tipsEnabled = action.enabled) }
    }
}
private fun update(transform: (AppSettings) -> AppSettings) {
    viewModelScope.launch { repository.updateSettings(transform).orSnackbar { update(transform) } }
}
```
экран: **`SettingsScreen.kt:95`, `:128–139`** — `onSettingsChange(settings.copy(soundEnabled = it))` →
`onAction(SettingsAction.SetSound(it))` и т. д.

### C3. Сумма «Отложить» — во ViewModel

**`…/presentation/screens/savings/SavingsScreen.kt:226–229`, `:276–288`**
```kotlin
// сейчас
var picked by remember { mutableIntStateOf(10) }
val maxAmount = state.balance.coerceAtLeast(GameRules.PLAN_STEP)
val amount = picked.coerceIn(GameRules.PLAN_STEP, maxAmount)
StepButton("−", …) { picked = (amount - GameRules.PLAN_STEP).coerceAtLeast(GameRules.PLAN_STEP) }
StepButton("+", …) { picked = (amount + GameRules.PLAN_STEP).coerceAtMost(maxAmount) }
TealButton("Отложить", …, { onDeposit(amount) })
// нужно
StepButton("−", …, enabled = state.canDepositLess) { onAction(SavingsAction.ChangeDeposit(increase = false)) }
StepButton("+", …, enabled = state.canDepositMore) { onAction(SavingsAction.ChangeDeposit(increase = true)) }
TealButton("Отложить", …, { onAction(SavingsAction.Deposit) })
```
`SavingsUiState`: `depositAmount: Int`, `canDepositLess/More` (вычисляемые); `SavingsViewModel`: кламп и шаг —
как сейчас в экране; `SavingsAction.Deposit` — без параметра.

### C4. «Осталось до цели» — в домене

**`…/domain/game/model/GameState.kt`**
```kotlin
/** Сколько не хватает до цели; null — цели нет. */
val goalRemaining: Int? get() = goal?.let { (it.cost - savings).coerceAtLeast(0) }
```
заменить арифметику в экранах на поле UiState:
- **`…/presentation/screens/savings/SavingsScreen.kt:224`** `val remaining = (goal.cost - state.savings).coerceAtLeast(0)` → `state.goalRemaining`;
- **`…/presentation/screens/progress/ProgressScreen.kt:249`** `"Осталось ${(goal.cost - state.savings).coerceAtLeast(0)}"` → `"Осталось ${state.goalRemaining}"`;
- **`…/presentation/screens/savings/WithdrawDialog.kt:71–72`** `before/after` → `draft.remainingBefore/After`
  (считает `SavingsViewModel.withdrawDraft`).

### C5. **`…/presentation/screens/shop/ShopScreen.kt:620–625`**
```kotlin
// сейчас
private fun savingsConsequence(fb: PurchaseFeedback.NotEnough): String {
    val after = fb.savings - fb.missing
    val goal = fb.goal ?: return "В копилке ${fb.savings} → станет $after"
    return "… До «${goal.name}» будет не хватать ${(goal.cost - after).coerceAtLeast(0)}"
}
// нужно — числа приходят готовыми из ShopViewModel
private fun savingsConsequence(fb: PurchaseFeedback.NotEnough): String {
    val goal = fb.goal ?: return "В копилке ${fb.savings} → станет ${fb.savingsAfter}"
    return "В копилке ${fb.savings} → станет ${fb.savingsAfter}. До «${goal.name}» будет не хватать ${fb.goalRemainingAfter}"
}
```

### C6. **`app/src/main/AndroidManifest.xml:10–12`**
```xml
<!-- сейчас -->
android:allowBackup="true"
android:dataExtractionRules="@xml/data_extraction_rules"
android:fullBackupContent="@xml/backup_rules"
<!-- нужно -->
android:allowBackup="false"
```
удалить `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`.

### C7. Текст 16 sp [решение владельца] — основные места
`StickerKit.kt:442` (`CoachNote` 15 sp), `HomeScreen.kt:440` (`SpeechBubble` 15 sp), `HomeScreen.kt:282`
(`TutorialDoneDialog` 15 sp), `HomeInfoDialog.kt:118` (строки 15 sp), `DeedsDialog.kt` (`TodoRow` 15, рост 14,
`FinishNote` 14), `StatRow` (`StickerKit.kt:378`, 15), `ShopScreen.kt:609–610` (`OptionRow` 15/13),
`TasksScreen.kt:~383` (`TaskResultCard`, объяснение 15). Замена: `fontSize = 15.sp` → `style = MaterialTheme.typography.bodyLarge`
(16 sp) с прежними `fontWeight`/`color`.

---

<a id="b10"></a>
## B10. Удаления (точные места)

| Удалить | Где |
|---|---|
| файл | `…/presentation/screens/home/SectionStubScreen.kt` |
| `enum class HomeSection(val title: String)` → `enum class HomeSection { TASKS, SHOP, … }` | `…/presentation/screens/home/HomeSection.kt:4–11` |
| `HomeSection.ADULT` и ветка `HomeSection.ADULT -> Adult` | `HomeSection.kt:10`, `…/presentation/navigation/Routes.kt:70` |
| `data object PetTapped : HomeAction` | `…/presentation/screens/home/HomeAction.kt:8` |
| `HomeAction.PetTapped -> Unit       // TODO: реакция питомца` | `…/presentation/screens/home/HomeViewModel.kt:98` |
| `val ShopItem.categoryText` | `…/presentation/screens/shop/ShopItemUi.kt:40–42` |
| `fun planLeft(...)` + тест `planLeftShowsWhatRemainsPerCategoryAndGoesBelowZeroWhenOverspent` | `…/domain/game/model/GameState.kt:46–47`, `GameEngineTest.kt:250–262` |
| `val tasksPerWeek` | `…/domain/game/model/GameState.kt:52` (см. A5) |
| файл | `app/src/main/res/values/colors.xml` |
| файлы | `ExampleUnitTest.kt`, `app/src/androidTest/.../ExampleInstrumentedTest.kt`, `InMemorySettingsRepositoryTest.kt` |
| перенести в `app/src/test/java/ru/larpinovplay/finniapp/` | `…/data/game/store/InMemoryGameStore.kt`, `…/data/settings/InMemorySettingsRepository.kt` (пакеты не менять) |
| `"Финни Демо"` в data → `Pet.DEMO_NAME` в домене | `…/data/game/GameRepositoryImpl.kt:123` |
