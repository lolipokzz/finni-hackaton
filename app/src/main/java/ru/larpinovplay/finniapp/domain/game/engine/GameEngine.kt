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
import ru.larpinovplay.finniapp.domain.game.model.LevelResult
import ru.larpinovplay.finniapp.domain.game.model.LevelStatus
import ru.larpinovplay.finniapp.domain.game.model.PeriodPhase
import ru.larpinovplay.finniapp.domain.game.model.PurchaseResult
import ru.larpinovplay.finniapp.domain.game.model.Transition
import ru.larpinovplay.finniapp.domain.game.model.Trip
import ru.larpinovplay.finniapp.domain.game.model.WeekSummary
import ru.larpinovplay.finniapp.domain.game.model.weekDeeds
import ru.larpinovplay.finniapp.domain.game.model.WithdrawResult
import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem
import ru.larpinovplay.finniapp.domain.shop.model.WearableSlot
import ru.larpinovplay.finniapp.domain.task.model.Level
import java.time.LocalDate

/**
 * Игра как чистые функции (docs/06-architecture.md): `(снимок, команда) → (новый снимок, результат)`.
 * Единственное место, где создаётся запись журнала и меняются кошелёк и питомец. Без корутин и Android,
 * поэтому проверяется обычными юнит-тестами. Хранение и последовательность команд — дело GameRepository.
 *
 * Правила — из docs/04-rules-and-formulas.md. Неделя идёт так: приходят монеты → ребёнок делит их планом
 * ([confirmPlan]) → покупает и копит → закрывает неделю ([finishWeek]): каждое сделанное дело недели — шаг роста.
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
     * из отложенного за неделю, поэтому дело «Копилка по плану» и бонус копилки считаются уже после него.
     */
    fun withdraw(game: GameSnapshot, amount: Int): Transition<WithdrawResult> {
        val s = game.state
        if (amount <= 0 || amount > s.savings) return Transition(game, WithdrawResult.Rejected(s.savings))
        val state = s.post(LedgerReason.Withdraw, +amount, -amount).copy(withdrawalsThisWeek = s.withdrawalsThisWeek + amount)
        return Transition(game.copy(state = state), WithdrawResult.Success)
    }

    /** Достигнутая цель или null, если цели нет или на неё ещё не накоплено. Цель-поездка отправляет питомца в поездку на эту неделю. */
    fun reachGoal(game: GameSnapshot): Transition<SavingsGoal?> {
        val s = game.state
        val goal = s.goal
        if (goal == null || s.savings < goal.cost) return Transition(game, null)
        val state = s.post(LedgerReason.GoalReached(goal.name), 0, -goal.cost).copy(
            completedGoals = s.completedGoals + goal,
            goal = null,
            trip = if (goal.trip) Trip(goal.id, s.week) else s.trip,
        )
        val pet = game.pet.changeMood(GameRules.GOAL_MOOD_BONUS)
        return Transition(GameSnapshot(state, pet), goal)
    }

    /**
     * Засчитывает первое прохождение [level] с [mistakes] ошибками: звёзды ([GameRules.levelStars]) и награду.
     * Ошибки меняют только их: пройти уровень — главное. null, если уровень закрыт (не его неделя или не пройден
     * предыдущий на тропинке [levels]) или уже пройден.
     */
    fun completeLevel(game: GameSnapshot, level: Level, mistakes: Int, levels: List<Level>): Transition<LevelResult?> {
        val s = game.state
        if (s.levelStatus(level, levels) != LevelStatus.AVAILABLE) return Transition(game, null)
        val errors = mistakes.coerceIn(0, GameRules.levelTasks(level))
        val result = LevelResult(
            levelId = level.id,
            week = s.week,
            stars = GameRules.levelStars(errors),
            reward = if (errors == 0) level.reward else level.rewardOnMistake,
        )
        val state = s.post(LedgerReason.TaskReward(level.title), +result.reward).copy(levelResults = s.levelResults + result)
        return Transition(game.copy(state = state), result)
    }

    /**
     * Золотое испытание пройденного уровня — без монет, только звание. true — испытание пройдено: без ошибок
     * и не дольше [GameRules.challengeSeconds] за [seconds]; золото остаётся за уровнем навсегда.
     */
    fun completeChallenge(game: GameSnapshot, level: Level, mistakes: Int, seconds: Int): Transition<Boolean> {
        val s = game.state
        val won = s.levelResult(level) != null && mistakes == 0 &&
            seconds <= GameRules.challengeSeconds(level)
        if (!won || level.id in s.goldLevels) return Transition(game, won)
        return Transition(game.copy(state = s.copy(goldLevels = s.goldLevels + level.id)), true)
    }

    /**
     * Засчитывает приключение недели с [mistakes] ошибками и платит награду. Ошибки влияют только на её размер:
     * пройти — главное. null, если [adventure] сейчас закрыто или уже пройдено ([GameState.adventureStatus]):
     * не приключение недели или не пройдены уровни недели [levels].
     */
    fun completeAdventure(
        game: GameSnapshot,
        adventure: Adventure,
        mistakes: Int,
        adventures: List<Adventure>,
        levels: List<Level> = emptyList(),
    ): Transition<AdventureResult?> {
        val s = game.state
        if (s.adventureStatus(adventure, adventures, levels) != LevelStatus.AVAILABLE) return Transition(game, null)
        val perfect = mistakes == 0
        val result = AdventureResult(adventure.id, s.week, perfect, if (perfect) adventure.reward else adventure.rewardOnMistake)
        val state = s.post(LedgerReason.AdventureReward(adventure.title), +result.reward)
            .copy(adventureResults = s.adventureResults + result)
        return Transition(game.copy(state = state), result)
    }

    /**
     * Начисляет [amount] монет в демо-режиме (запись журнала «Монеты демо-режима»): эксперт проверяет покупки и
     * копилку, не дожидаясь карманных. В обычной игре ничего не делает и возвращает false — там монеты только
     * зарабатываются. Посреди плана бюджет недели растёт: план раскладывает весь кошелёк.
     */
    fun addDemoCoins(game: GameSnapshot, amount: Int): Transition<Boolean> {
        val s = game.state
        if (!s.demoMode || amount <= 0) return Transition(game, false)
        return Transition(game.copy(state = s.post(LedgerReason.DemoCoins, +amount)), true)
    }

    /**
     * Закрывает неделю, если [GameState.finishBlock] не мешает: засчитывает дела недели шагами роста,
     * платит бонус копилки, меняет питомца, начинает новую неделю в фазе плана и зачисляет карманные
     * по новой стадии питомца (docs/11-economy.md).
     */
    fun finishWeek(
        game: GameSnapshot,
        today: LocalDate,
        adventures: List<Adventure> = emptyList(),
        levels: List<Level> = emptyList(),
    ): Transition<FinishWeekResult> {
        val s = game.state
        s.finishBlock(today, adventures, levels)?.let { return Transition(game, FinishWeekResult.Blocked(it)) }

        val plan = s.plan ?: BudgetPlan()
        val saved = s.savedThisWeek
        // Дела считаются до недельного падения настроения: засчитывается то, что было к закрытию недели
        val deeds = game.weekDeeds
        val bonus = if (saved >= GameRules.SAVINGS_BONUS_MIN) GameRules.SAVINGS_BONUS else 0
        // Настроение от дел не зависит: неделя его снижает, а вещи и цели немного поддерживают
        val lasting = minOf(s.lastingJoys * GameRules.LASTING_MOOD_PER_ITEM, GameRules.LASTING_MOOD_CAP)
        val moodDelta = lasting - GameRules.WEEKLY_MOOD_DECAY

        val before = game.pet
        val pet = before.changeSatiety(-GameRules.WEEKLY_HUNGER).changeMood(moodDelta).grow(deeds.steps)
        val income = GameRules.weekIncome(pet.growthStage)

        val summary = WeekSummary(
            week = s.week,
            plan = plan,
            deeds = deeds,
            spentMandatory = s.spentThisWeek(ShopCategory.MANDATORY),
            spentOptional = s.spentThisWeek(ShopCategory.OPTIONAL),
            saved = saved,
            withdrawn = s.withdrawalsThisWeek.sum(),
            savingsBonus = bonus,
            moodDelta = pet.mood.value - before.mood.value,
            lastingMood = lasting,
            stageBefore = before.growthStage,
            stageAfter = pet.growthStage,
            stepsToNextStage = pet.pointsToNextStage,
            nextIncome = income,
        )
        val withBonus = if (bonus > 0) s.post(LedgerReason.SavingsBonus, 0, +bonus) else s
        val state = withBonus.copy(
            history = s.history + summary,
            depositsByWeek = s.depositsByWeek + (saved + bonus),
            depositsThisWeek = emptyList(),
            withdrawalsThisWeek = emptyList(),
            purchases = emptyList(),
            week = s.week + 1,
            phase = PeriodPhase.PLANNING,
            plan = null,
            periodStartedOn = today,
        ).post(LedgerReason.WeekIncome, +income)
        return Transition(GameSnapshot(state, pet), FinishWeekResult.Finished(summary))
    }

    private fun GameState.post(reason: LedgerReason, balanceDelta: Int, savingsDelta: Int = 0): GameState = copy(
        balance = balance + balanceDelta,
        savings = savings + savingsDelta,
        ledger = ledger + LedgerEntry(week, reason, balanceDelta, savingsDelta),
    )
}
