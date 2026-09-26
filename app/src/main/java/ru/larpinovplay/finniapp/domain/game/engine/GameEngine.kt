package ru.larpinovplay.finniapp.domain.game.engine

import ru.larpinovplay.finniapp.domain.adventure.model.Adventure
import ru.larpinovplay.finniapp.domain.game.model.AdventureResult
import ru.larpinovplay.finniapp.domain.game.model.BudgetPlan
import ru.larpinovplay.finniapp.domain.game.model.ConfirmPlanResult
import ru.larpinovplay.finniapp.domain.game.model.DepositResult
import ru.larpinovplay.finniapp.domain.game.model.FinishWeekResult
import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.game.model.GameState
import ru.larpinovplay.finniapp.domain.game.model.LedgerEntry
import ru.larpinovplay.finniapp.domain.game.model.LedgerReason
import ru.larpinovplay.finniapp.domain.game.model.PeriodPhase
import ru.larpinovplay.finniapp.domain.game.model.PurchaseResult
import ru.larpinovplay.finniapp.domain.game.model.TaskResult
import ru.larpinovplay.finniapp.domain.game.model.TaskStatus
import ru.larpinovplay.finniapp.domain.game.model.Transition
import ru.larpinovplay.finniapp.domain.game.model.WeekSummary
import ru.larpinovplay.finniapp.domain.game.model.WithdrawResult
import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem
import ru.larpinovplay.finniapp.domain.shop.model.WearableSlot
import ru.larpinovplay.finniapp.domain.task.evaluate
import ru.larpinovplay.finniapp.domain.task.model.Task
import ru.larpinovplay.finniapp.domain.task.model.TaskAnswer
import ru.larpinovplay.finniapp.domain.task.model.TaskOutcome
import java.time.LocalDate

/**
 * Игра как чистые функции (docs/06-architecture.md): `(снимок, команда) → (новый снимок, результат)`.
 * Единственное место, где создаётся запись журнала и меняются кошелёк и питомец. Без корутин и Android,
 * поэтому проверяется обычными юнит-тестами. Хранение и последовательность команд — дело GameRepository.
 *
 * Правила — из docs/04-rules-and-formulas.md. Неделя идёт так: приходят монеты → ребёнок делит их планом
 * ([confirmPlan]) → покупает и копит → закрывает неделю ([finishWeek]) и получает до трёх звёзд.
 * Дата приходит параметром, поэтому движок не зависит от часов устройства.
 */
object GameEngine {

    fun newGame(today: LocalDate, startBalance: Int = GameRules.START_BALANCE): GameState =
        GameState(periodStartedOn = today).post(LedgerReason.StartCoins, +startBalance)

    /**
     * Подтверждает план недели. План должен разложить весь баланс, без минусов, и только один раз за неделю.
     * Строка «Копилка» сразу переводится в копилку: отложить — поведение по умолчанию, а забрать можно ([withdraw]).
     */
    fun confirmPlan(game: GameSnapshot, plan: BudgetPlan): Transition<ConfirmPlanResult> {
        val s = game.state
        val valid = s.phase == PeriodPhase.PLANNING &&
            plan.mandatory >= 0 && plan.optional >= 0 && plan.savings >= 0 &&
            plan.total == s.balance
        if (!valid) return Transition(game, ConfirmPlanResult.Rejected(budget = s.balance))
        val planned = s.copy(phase = PeriodPhase.ACTIVE, plan = plan)
        val state = if (plan.savings > 0) {
            planned.post(LedgerReason.PlannedDeposit, -plan.savings, +plan.savings)
                .copy(depositsThisWeek = planned.depositsThisWeek + plan.savings)
        } else {
            planned
        }
        return Transition(game.copy(state = state), ConfirmPlanResult.Success)
    }

    /**
     * Покупка: списывает монеты и сразу действует на питомца. Одежда ещё и попадает в гардероб навсегда
     * (надевается отдельно, см. [wear]) и считается тратой недели, как любая покупка.
     */
    fun buy(game: GameSnapshot, item: ShopItem): Transition<PurchaseResult> {
        val s = game.state
        if (item.isWearable && s.owns(item)) return Transition(game, PurchaseResult.AlreadyOwned)
        if (item.price > s.balance) {
            return Transition(game, PurchaseResult.NotEnough(missing = item.price - s.balance))
        }
        val state = s.post(LedgerReason.Purchase(item.name), -item.price).copy(
            purchases = s.purchases + item,
            wardrobe = if (item.isWearable) s.wardrobe + item else s.wardrobe,
        )
        val pet = game.pet.changeSatiety(item.satiety).changeMood(item.mood)
        return Transition(GameSnapshot(state, pet), PurchaseResult.Success(item, balanceAfter = state.balance))
    }

    /** Надевает вещь из гардероба на её место (прежняя вещь с этого места снимается). false — вещи нет в гардеробе. */
    fun wear(game: GameSnapshot, item: ShopItem): Transition<Boolean> {
        if (!item.isWearable || !game.state.owns(item)) return Transition(game, false)
        return Transition(game.copy(pet = game.pet.wear(item)), true)
    }

    fun takeOff(game: GameSnapshot, slot: WearableSlot): Transition<Unit> =
        Transition(game.copy(pet = game.pet.takeOff(slot)), Unit)

    fun chooseGoal(game: GameSnapshot, goal: SavingsGoal): Transition<Unit> =
        Transition(game.copy(state = game.state.copy(goal = goal)), Unit)

    fun deposit(game: GameSnapshot, amount: Int): Transition<DepositResult> {
        val s = game.state
        if (amount <= 0 || amount > s.balance) return Transition(game, DepositResult.Rejected(s.balance))
        val state = s.post(LedgerReason.Deposit, -amount, +amount).copy(depositsThisWeek = s.depositsThisWeek + amount)
        return Transition(game.copy(state = state), DepositResult.Success)
    }

    /**
     * Забирает [amount] из копилки в кошелёк. Можно всё, до нуля: это деньги ребёнка. Снятое вычитается
     * из отложенного за неделю, поэтому звёзды «Копилка» и «План» считаются уже после него.
     */
    fun withdraw(game: GameSnapshot, amount: Int): Transition<WithdrawResult> {
        val s = game.state
        if (amount <= 0 || amount > s.savings) return Transition(game, WithdrawResult.Rejected(s.savings))
        val state = s.post(LedgerReason.Withdraw, +amount, -amount).copy(withdrawalsThisWeek = s.withdrawalsThisWeek + amount)
        return Transition(game.copy(state = state), WithdrawResult.Success)
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

    /**
     * Засчитывает приключение недели с [mistakes] ошибками и платит награду. Ошибки влияют только на её размер:
     * пройти — главное. null, если [adventure] сейчас не приключение недели (уже пройдено или не по порядку).
     */
    fun completeAdventure(
        game: GameSnapshot,
        adventure: Adventure,
        mistakes: Int,
        adventures: List<Adventure>,
    ): Transition<AdventureResult?> {
        val s = game.state
        if (s.adventureOfWeek(adventures)?.id != adventure.id) return Transition(game, null)
        val perfect = mistakes == 0
        val result = AdventureResult(adventure.id, s.week, perfect, if (perfect) adventure.reward else adventure.rewardOnMistake)
        val state = s.post(LedgerReason.AdventureReward(adventure.title), +result.reward)
            .copy(adventureResults = s.adventureResults + result)
        return Transition(game.copy(state = state), result)
    }

    /**
     * Закрывает неделю, если [GameState.finishBlock] не мешает: считает звёзды, меняет питомца,
     * начинает новую неделю в фазе плана и зачисляет карманные деньги.
     */
    fun finishWeek(game: GameSnapshot, today: LocalDate, adventures: List<Adventure> = emptyList()): Transition<FinishWeekResult> {
        val s = game.state
        s.finishBlock(today, adventures)?.let { return Transition(game, FinishWeekResult.Blocked(it)) }

        val plan = s.plan ?: BudgetPlan()
        val spentMandatory = s.spentThisWeek(ShopCategory.MANDATORY)
        val spentOptional = s.spentThisWeek(ShopCategory.OPTIONAL)
        val saved = s.savedThisWeek
        val foodCovered = s.foodCovered
        val savedSomething = saved > 0
        // Перерасход на нужное не нарушает план: ребёнок не должен бояться накормить питомца
        val planKept = spentOptional <= plan.optional && saved >= plan.savings
        val score = listOf(foodCovered, savedSomething, planKept).count { it }
        val moodDelta = -GameRules.WEEKLY_MOOD_DECAY + GameRules.MOOD_DELTA_BY_SCORE[score]

        val before = game.pet
        val pet = before.changeSatiety(-GameRules.WEEKLY_HUNGER).changeMood(moodDelta).grow(score)

        val summary = WeekSummary(
            week = s.week,
            plan = plan,
            foodCovered = foodCovered,
            savedSomething = savedSomething,
            planKept = planKept,
            spentMandatory = spentMandatory,
            spentOptional = spentOptional,
            saved = saved,
            withdrawn = s.withdrawalsThisWeek.sum(),
            score = score,
            moodDelta = moodDelta,
            stageBefore = before.growthStage,
            stageAfter = pet.growthStage,
            nextIncome = GameRules.WEEK_INCOME,
        )
        val state = s.copy(
            history = s.history + summary,
            depositsByWeek = s.depositsByWeek + saved,
            depositsThisWeek = emptyList(),
            withdrawalsThisWeek = emptyList(),
            purchases = emptyList(),
            tasksDoneThisWeek = 0,
            week = s.week + 1,
            phase = PeriodPhase.PLANNING,
            plan = null,
            periodStartedOn = today,
        ).post(LedgerReason.WeekIncome, +GameRules.WEEK_INCOME)
        return Transition(GameSnapshot(state, pet), FinishWeekResult.Finished(summary))
    }

    private fun GameState.post(reason: LedgerReason, balanceDelta: Int, savingsDelta: Int = 0): GameState = copy(
        balance = balance + balanceDelta,
        savings = savings + savingsDelta,
        ledger = ledger + LedgerEntry(week, reason, balanceDelta, savingsDelta),
    )
}
