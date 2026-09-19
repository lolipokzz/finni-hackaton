package ru.larpinovplay.finniapp.data.game

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
import ru.larpinovplay.finniapp.domain.pet.model.PetLook
import ru.larpinovplay.finniapp.domain.pet.model.PetSpecies
import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem
import ru.larpinovplay.finniapp.domain.task.model.Task
import ru.larpinovplay.finniapp.domain.task.model.TaskAnswer
import ru.larpinovplay.finniapp.domain.task.model.TaskOutcome

/**
 * Хранит игру в памяти процесса. Сама правил не знает: берёт снимок (игра + питомец), отдаёт его
 * [GameEngine], записывает результат целиком одной записью. Команды идут строго по одной, чтобы два быстрых
 * нажатия не породили гонку чтения и записи. Заменится на репозиторий с Room: один агрегат, одна транзакция.
 */
class InMemoryGameRepository(
    private val startBalance: Int = GameRules.START_BALANCE,
) : GameRepository {

    private val _snapshot = MutableStateFlow<GameSnapshot?>(null)
    override val snapshot: StateFlow<GameSnapshot?> = _snapshot.asStateFlow()

    private val mutex = Mutex()

    override suspend fun createPet(pet: Pet) = mutex.withLock {
        check(_snapshot.value == null) { "Игра уже начата" }
        _snapshot.value = GameSnapshot(GameEngine.newGame(startBalance), pet)
    }

    override suspend fun buy(item: ShopItem): PurchaseResult = execute { GameEngine.buy(it, item) }

    override suspend fun chooseGoal(goal: SavingsGoal) = execute { GameEngine.chooseGoal(it, goal) }

    override suspend fun deposit(amount: Int): DepositResult = execute { GameEngine.deposit(it, amount) }

    override suspend fun reachGoal(): SavingsGoal? = execute { GameEngine.reachGoal(it) }

    override suspend fun answerTask(task: Task, answer: TaskAnswer): TaskOutcome? =
        execute { GameEngine.answerTask(it, task, answer) }

    override suspend fun finishWeek(): WeekSummary = execute { GameEngine.finishWeek(it) }

    override suspend fun resetProfile() = mutex.withLock {
        _snapshot.value = null
    }

    override suspend fun resetToDemo() = mutex.withLock {
        _snapshot.value = GameSnapshot(
            GameEngine.newGame(startBalance).copy(demoMode = true),
            Pet.newborn("Финни Демо", PetLook(PetSpecies.BUNNY, PetColor.MINT)),
        )
    }

    private suspend fun <R> execute(command: (GameSnapshot) -> Transition<R>): R = mutex.withLock {
        val current = checkNotNull(_snapshot.value) { "Питомец ещё не создан" }
        val transition = command(current)
        _snapshot.value = transition.game
        transition.result
    }
}
