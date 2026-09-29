package ru.larpinovplay.finniapp.data.game

import ru.larpinovplay.finniapp.domain.util.result.map
import ru.larpinovplay.finniapp.domain.game.model.TutorialStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.larpinovplay.finniapp.data.game.store.GameStore
import ru.larpinovplay.finniapp.domain.game.engine.GameEngine
import ru.larpinovplay.finniapp.domain.game.engine.GameRules
import ru.larpinovplay.finniapp.domain.adventure.model.Adventure
import ru.larpinovplay.finniapp.domain.game.model.AdventureResult
import ru.larpinovplay.finniapp.domain.game.model.BudgetPlan
import ru.larpinovplay.finniapp.domain.game.model.ConfirmPlanResult
import ru.larpinovplay.finniapp.domain.game.model.DepositResult
import ru.larpinovplay.finniapp.domain.game.model.FinishBlock
import ru.larpinovplay.finniapp.domain.game.model.FinishWeekResult
import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.game.model.PurchaseResult
import ru.larpinovplay.finniapp.domain.game.model.Transition
import ru.larpinovplay.finniapp.domain.game.model.WithdrawResult
import ru.larpinovplay.finniapp.domain.game.repository.GameRepository
import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetLook
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem
import ru.larpinovplay.finniapp.domain.shop.model.WearableSlot
import ru.larpinovplay.finniapp.domain.storage.StorageError
import ru.larpinovplay.finniapp.domain.task.model.Task
import ru.larpinovplay.finniapp.domain.task.model.TaskAnswer
import ru.larpinovplay.finniapp.domain.task.model.TaskOutcome
import ru.larpinovplay.finniapp.domain.util.result.EmptyResult
import ru.larpinovplay.finniapp.domain.util.result.Result
import ru.larpinovplay.finniapp.domain.util.result.onSuccess
import java.time.Clock
import java.time.LocalDate

/**
 * Держит игру в памяти и записывает каждое её изменение в [store]. Сама правил не знает: берёт снимок
 * (игра + питомец), отдаёт его [GameEngine], записывает результат целиком одной записью.
 *
 * Запись идёт раньше публикации: [snapshot] меняется только после успешной записи, поэтому он всегда равен
 * тому, что лежит на диске, а при сбое команда не применяется и возвращает [StorageError] — его обрабатывает
 * ViewModel. Сама запись — suspend и идёт на Dispatchers.IO внутри [store]. Команды идут строго по одной,
 * чтобы два быстрых нажатия не породили гонку чтения и записи.
 *
 * Сегодняшнюю дату для правил недели берёт из [clock]; в тестах его подменяют. [adventures] — приключения из
 * контента: от них зависит, можно ли закончить неделю.
 */
class GameRepositoryImpl(
    private val store: GameStore,
    private val startBalance: Int = GameRules.START_BALANCE,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val adventures: List<Adventure> = emptyList(),
) : GameRepository {

    private val _snapshot = MutableStateFlow<GameSnapshot?>(null)
    override val snapshot: StateFlow<GameSnapshot?> = _snapshot.asStateFlow()

    private val _gameBeforeDemo = MutableStateFlow<GameSnapshot?>(null)
    override val gameBeforeDemo: StateFlow<GameSnapshot?> = _gameBeforeDemo.asStateFlow()

    private val mutex = Mutex()

    override suspend fun load(): Result<GameSnapshot?, StorageError> = mutex.withLock {
        store.load().onSuccess { loaded ->
            // Отложенная на время демо игра лежит в том же файле; сбой её чтения не мешает играть — выйти из демо
            // тогда можно будет только к созданию питомца
            _gameBeforeDemo.value = (store.loadBeforeDemo() as? Result.Success)?.data?.takeIf { loaded?.state?.demoMode == true }
            _snapshot.value = loaded
        }
    }

    override suspend fun createPet(pet: Pet, withTutorial: Boolean): EmptyResult<StorageError> = mutex.withLock {
        check(_snapshot.value == null) { "Игра уже начата" }
        persist(GameSnapshot(GameEngine.newGame(today(), startBalance).copy(tutorial = withTutorial), pet))
    }

    override suspend fun finishTutorial(): EmptyResult<StorageError> =
        execute { Transition(it.copy(state = it.state.copy(tutorial = false)), Unit) }

    override suspend fun skipTutorialStep(step: TutorialStep): EmptyResult<StorageError> =
        execute { Transition(it.copy(state = it.state.copy(tutorialSkipped = it.state.tutorialSkipped + step)), Unit) }

    override suspend fun buy(item: ShopItem): Result<PurchaseResult, StorageError> =
        execute { GameEngine.buy(it, item) }

    override suspend fun wear(item: ShopItem): Result<Boolean, StorageError> =
        execute { GameEngine.wear(it, item) }

    override suspend fun takeOff(slot: WearableSlot): EmptyResult<StorageError> =
        execute { GameEngine.takeOff(it, slot) }

    override suspend fun chooseGoal(goal: SavingsGoal): EmptyResult<StorageError> =
        execute { GameEngine.chooseGoal(it, goal) }

    override suspend fun deposit(amount: Int): Result<DepositResult, StorageError> =
        execute { GameEngine.deposit(it, amount) }

    override suspend fun withdraw(amount: Int): Result<WithdrawResult, StorageError> =
        execute { GameEngine.withdraw(it, amount) }

    override suspend fun reachGoal(): Result<SavingsGoal?, StorageError> =
        execute { GameEngine.reachGoal(it) }

    override suspend fun answerTask(task: Task, answer: TaskAnswer): Result<TaskOutcome?, StorageError> =
        execute { GameEngine.answerTask(it, task, answer) }

    override suspend fun confirmPlan(plan: BudgetPlan): Result<ConfirmPlanResult, StorageError> =
        execute { GameEngine.confirmPlan(it, plan) }

    override suspend fun completeAdventure(adventure: Adventure, mistakes: Int): Result<AdventureResult?, StorageError> =
        execute { GameEngine.completeAdventure(it, adventure, mistakes, adventures) }

    override fun finishBlock(): FinishBlock? = _snapshot.value?.state?.finishBlock(today(), adventures)

    override suspend fun finishWeek(): Result<FinishWeekResult, StorageError> =
        execute { GameEngine.finishWeek(it, today(), adventures) }

    override suspend fun resetProfile(): EmptyResult<StorageError> = mutex.withLock {
        store.clear().onSuccess {
            _gameBeforeDemo.value = null
            _snapshot.value = null
        }
    }

    override suspend fun resetToDemo(): EmptyResult<StorageError> = mutex.withLock {
        val demo = GameSnapshot(
            GameEngine.newGame(today(), startBalance).copy(demoMode = true),
            // Кот: у него весь функционал — удары, поглаживание, эмоции, гардероб, голос
            Pet.newborn("Финни Демо", PetLook(PetColor.CORAL)),
        )
        // Игра ребёнка откладывается до выхода из демо. Если демо уже идёт, отложена по-прежнему игра ребёнка,
        // а не прежнее демо
        val current = _snapshot.value
        val keep = if (current == null || current.state.demoMode) _gameBeforeDemo.value else current
        store.save(demo, keep).onSuccess {
            _gameBeforeDemo.value = keep
            _snapshot.value = demo
        }
    }

    override suspend fun addDemoCoins(amount: Int): Result<Boolean, StorageError> =
        execute { GameEngine.addDemoCoins(it, amount) }

    override suspend fun exitDemo(): EmptyResult<StorageError> = mutex.withLock {
        val current = checkNotNull(_snapshot.value) { "Игры нет" }
        check(current.state.demoMode) { "Демо-режим не включён" }
        val restored = _gameBeforeDemo.value
        val result = if (restored != null) store.save(restored, beforeDemo = null) else store.clear()
        // Сначала игра, потом отложенная копия: экран демо закрывается по вернувшейся игре и не успевает показать,
        // что отложенной игры «нет»
        result.onSuccess {
            _snapshot.value = restored
            _gameBeforeDemo.value = null
        }
    }

    private suspend fun <R> execute(command: (GameSnapshot) -> Transition<R>): Result<R, StorageError> =
        mutex.withLock {
            val current = checkNotNull(_snapshot.value) { "Питомец ещё не создан" }
            val transition = command(current)
            // Команда, которую отклонили правила, ничего не меняет: писать на диск нечего
            if (transition.game == current) return@withLock Result.Success(transition.result)
            persist(transition.game).map { transition.result }
        }

    private fun today(): LocalDate = LocalDate.now(clock)

    /** Записывает [new] и только после успеха делает его текущим; ошибка записи уходит вызывающему. */
    private suspend fun persist(new: GameSnapshot): EmptyResult<StorageError> =
        store.save(new).onSuccess { _snapshot.value = new }
}
