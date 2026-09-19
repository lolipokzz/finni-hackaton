package ru.larpinovplay.finniapp.data.game

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.larpinovplay.finniapp.data.game.store.GameStore
import ru.larpinovplay.finniapp.domain.game.engine.GameEngine
import ru.larpinovplay.finniapp.domain.game.engine.GameRules
import ru.larpinovplay.finniapp.domain.game.model.DepositResult
import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.game.model.PurchaseResult
import ru.larpinovplay.finniapp.domain.game.model.Transition
import ru.larpinovplay.finniapp.domain.game.model.WeekSummary
import ru.larpinovplay.finniapp.domain.game.repository.GameRepository
import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem
import ru.larpinovplay.finniapp.domain.storage.StorageError
import ru.larpinovplay.finniapp.domain.task.model.Task
import ru.larpinovplay.finniapp.domain.task.model.TaskAnswer
import ru.larpinovplay.finniapp.domain.task.model.TaskOutcome
import ru.larpinovplay.finniapp.domain.util.result.EmptyResult
import ru.larpinovplay.finniapp.domain.util.result.Result
import ru.larpinovplay.finniapp.domain.util.result.map
import ru.larpinovplay.finniapp.domain.util.result.onSuccess

/**
 * Держит игру в памяти и записывает каждое её изменение в [store]. Сама правил не знает: берёт снимок
 * (игра + питомец), отдаёт его [GameEngine], записывает результат целиком одной записью.
 *
 * Запись идёт раньше публикации: [snapshot] меняется только после успешной записи, поэтому он всегда равен
 * тому, что лежит на диске, а при сбое команда просто не применяется и возвращает [StorageError]. Команды идут
 * строго по одной, чтобы два быстрых нажатия не породили гонку чтения и записи; ожидание записи (единицы
 * миллисекунд) на них тоже лежит.
 */
class GameRepositoryImpl(
    private val store: GameStore,
    private val startBalance: Int = GameRules.START_BALANCE,
) : GameRepository {

    private val _snapshot = MutableStateFlow<GameSnapshot?>(null)
    override val snapshot: StateFlow<GameSnapshot?> = _snapshot.asStateFlow()

    private val mutex = Mutex()

    override suspend fun load(): Result<GameSnapshot?, StorageError> = mutex.withLock {
        store.load().onSuccess { _snapshot.value = it }
    }

    override suspend fun createPet(pet: Pet): EmptyResult<StorageError> = mutex.withLock {
        check(_snapshot.value == null) { "Игра уже начата" }
        persist(GameSnapshot(GameEngine.newGame(startBalance), pet))
    }

    override suspend fun buy(item: ShopItem): Result<PurchaseResult, StorageError> =
        execute { GameEngine.buy(it, item) }

    override suspend fun chooseGoal(goal: SavingsGoal): EmptyResult<StorageError> =
        execute { GameEngine.chooseGoal(it, goal) }

    override suspend fun deposit(amount: Int): Result<DepositResult, StorageError> =
        execute { GameEngine.deposit(it, amount) }

    override suspend fun reachGoal(): Result<SavingsGoal?, StorageError> =
        execute { GameEngine.reachGoal(it) }

    override suspend fun answerTask(task: Task, answer: TaskAnswer): Result<TaskOutcome?, StorageError> =
        execute { GameEngine.answerTask(it, task, answer) }

    override suspend fun finishWeek(): Result<WeekSummary, StorageError> =
        execute { GameEngine.finishWeek(it) }

    private suspend fun <R> execute(command: (GameSnapshot) -> Transition<R>): Result<R, StorageError> =
        mutex.withLock {
            val current = checkNotNull(_snapshot.value) { "Питомец ещё не создан" }
            val transition = command(current)
            // Команда, которую отклонили правила, ничего не меняет: писать на диск нечего
            if (transition.game == current) return@withLock Result.Success(transition.result)
            persist(transition.game).map { transition.result }
        }

    /** Записывает [new] и только после успеха делает его текущим. */
    private suspend fun persist(new: GameSnapshot): EmptyResult<StorageError> =
        store.save(new).onSuccess { _snapshot.value = new }
}
