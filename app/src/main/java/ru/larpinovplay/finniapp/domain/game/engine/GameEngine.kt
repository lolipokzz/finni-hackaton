package ru.larpinovplay.finniapp.domain.game.engine

import ru.larpinovplay.finniapp.domain.game.model.DepositResult
import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.game.model.GameState
import ru.larpinovplay.finniapp.domain.game.model.LedgerEntry
import ru.larpinovplay.finniapp.domain.game.model.LedgerReason
import ru.larpinovplay.finniapp.domain.game.model.PurchaseResult
import ru.larpinovplay.finniapp.domain.game.model.TaskResult
import ru.larpinovplay.finniapp.domain.game.model.TaskStatus
import ru.larpinovplay.finniapp.domain.game.model.Transition
import ru.larpinovplay.finniapp.domain.game.model.WeekSummary
import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem
import ru.larpinovplay.finniapp.domain.task.evaluate
import ru.larpinovplay.finniapp.domain.task.model.Task
import ru.larpinovplay.finniapp.domain.task.model.TaskAnswer
import ru.larpinovplay.finniapp.domain.task.model.TaskOutcome

/**
 * Игра как чистые функции (docs/06-architecture.md): `(снимок, команда) → (новый снимок, результат)`.
 * Единственное место, где создаётся запись журнала и меняются кошелёк и питомец. Без корутин и Android,
 * поэтому проверяется обычными юнит-тестами. Хранение и последовательность команд — дело GameRepository.
 *
 * Правила — из docs/04-rules-and-formulas.md, с поправкой на то, что план бюджета убран:
 * неделя оценивается по двум критериям (еда куплена, что-то отложено), максимум 2 очка.
 */
object GameEngine {

    fun newGame(startBalance: Int = GameRules.START_BALANCE): GameState =
        GameState().post(LedgerReason.StartCoins, +startBalance)

    fun buy(game: GameSnapshot, item: ShopItem): Transition<PurchaseResult> {
        val s = game.state
        if (item.price > s.balance) {
            return Transition(game, PurchaseResult.NotEnough(missing = item.price - s.balance))
        }
        val state = s.post(LedgerReason.Purchase(item.name), -item.price).copy(purchases = s.purchases + item)
        val pet = game.pet.changeSatiety(item.satiety).changeMood(item.mood)
        return Transition(GameSnapshot(state, pet), PurchaseResult.Success(item, balanceAfter = state.balance))
    }

    fun chooseGoal(game: GameSnapshot, goal: SavingsGoal): Transition<Unit> =
        Transition(game.copy(state = game.state.copy(goal = goal)), Unit)

    fun deposit(game: GameSnapshot, amount: Int): Transition<DepositResult> {
        val s = game.state
        if (amount <= 0 || amount > s.balance) return Transition(game, DepositResult.Rejected(s.balance))
        val state = s.post(LedgerReason.Deposit, -amount, +amount).copy(depositsThisWeek = s.depositsThisWeek + amount)
        return Transition(game.copy(state = state), DepositResult.Success)
    }

    /** Достигнутая цель или null, если цели нет или на неё ещё не накоплено. */
    fun reachGoal(game: GameSnapshot): Transition<SavingsGoal?> {
        val s = game.state
        val goal = s.goal
        if (goal == null || s.savings < goal.cost) return Transition(game, null)
        val state = s.post(LedgerReason.GoalReached(goal.name), 0, -goal.cost).copy(
            completedGoals = s.completedGoals + goal,
            goal = null,
        )
        val pet = game.pet.changeMood(GameRules.GOAL_MOOD_BONUS)
        return Transition(GameSnapshot(state, pet), goal)
    }

    /** Итог ответа или null, если задание сейчас недоступно. */
    fun answerTask(game: GameSnapshot, task: Task, answer: TaskAnswer): Transition<TaskOutcome?> {
        val s = game.state
        if (s.taskStatus(task) != TaskStatus.AVAILABLE) return Transition(game, null)
        val outcome = task.evaluate(answer)
        val state = s.post(LedgerReason.TaskReward(task.title), +outcome.reward).copy(
            taskResults = s.taskResults + TaskResult(task.id, s.week, outcome.success, outcome.reward),
            tasksDoneThisWeek = s.tasksDoneThisWeek + 1,
        )
        return Transition(game.copy(state = state), outcome)
    }

    fun finishWeek(game: GameSnapshot): Transition<WeekSummary> {
        val s = game.state
        val saved = s.depositsThisWeek.sum()
        val foodCovered = s.foodCovered
        val savedSomething = saved > 0
        val score = (if (foodCovered) 1 else 0) + (if (savedSomething) 1 else 0)
        val moodDelta = -10 + when (score) { 2 -> 20; 1 -> 0; else -> -15 }

        val before = game.pet
        val pet = before.changeSatiety(-GameRules.WEEKLY_HUNGER).changeMood(moodDelta).grow(score)

        val summary = WeekSummary(
            week = s.week,
            foodCovered = foodCovered,
            savedSomething = savedSomething,
            spentMandatory = s.purchases.filter { it.category == ShopCategory.MANDATORY }.sumOf { it.price },
            spentOptional = s.purchases.filter { it.category == ShopCategory.OPTIONAL }.sumOf { it.price },
            saved = saved,
            score = score,
            moodDelta = moodDelta,
            stageBefore = before.growthStage,
            stageAfter = pet.growthStage,
        )
        val state = s.copy(
            history = s.history + summary,
            depositsByWeek = s.depositsByWeek + saved,
            depositsThisWeek = emptyList(),
            purchases = emptyList(),
            tasksDoneThisWeek = 0,
            week = s.week + 1,
        ).post(LedgerReason.WeekIncome, +GameRules.WEEK_INCOME)
        return Transition(GameSnapshot(state, pet), summary)
    }

    private fun GameState.post(reason: LedgerReason, balanceDelta: Int, savingsDelta: Int = 0): GameState = copy(
        balance = balance + balanceDelta,
        savings = savings + savingsDelta,
        ledger = ledger + LedgerEntry(week, reason, balanceDelta, savingsDelta),
    )
}
