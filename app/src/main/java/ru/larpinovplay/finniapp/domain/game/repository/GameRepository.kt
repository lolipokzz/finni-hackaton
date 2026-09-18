package ru.larpinovplay.finniapp.domain.game.repository

import kotlinx.coroutines.flow.StateFlow
import ru.larpinovplay.finniapp.domain.game.model.DepositResult
import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.game.model.PurchaseResult
import ru.larpinovplay.finniapp.domain.game.model.WeekSummary
import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem
import ru.larpinovplay.finniapp.domain.task.model.Task
import ru.larpinovplay.finniapp.domain.task.model.TaskAnswer
import ru.larpinovplay.finniapp.domain.task.model.TaskOutcome

/**
 * Единственная точка, через которую экраны читают игру и отдают ей команды.
 * Питомец — часть игры: он лежит в том же [snapshot], что и состояние, и обновляется с ним одной записью,
 * поэтому экран не увидит новые монеты со старой сытостью. Читать снимок можно, менять его умеют только
 * команды ниже (правила каждой описаны в GameEngine). Команды выполняются последовательно.
 */
interface GameRepository {

    /** Игра целиком; null, пока не создан питомец: игра начинается с него. */
    val snapshot: StateFlow<GameSnapshot?>

    /**
     * Начинает игру с питомцем [pet].
     * @throws IllegalStateException если игра уже начата.
     */
    suspend fun createPet(pet: Pet)

    suspend fun buy(item: ShopItem): PurchaseResult

    suspend fun chooseGoal(goal: SavingsGoal)

    suspend fun deposit(amount: Int): DepositResult

    /** Достигнутая цель или null, если цели нет или на неё ещё не накоплено. */
    suspend fun reachGoal(): SavingsGoal?

    /** Итог ответа или null, если задание сейчас недоступно. */
    suspend fun answerTask(task: Task, answer: TaskAnswer): TaskOutcome?

    suspend fun finishWeek(): WeekSummary
}

/** Игра для экранов, которые открываются только после создания питомца. */
fun GameRepository.requireSnapshot(): GameSnapshot = checkNotNull(snapshot.value) { "Питомец ещё не создан" }
