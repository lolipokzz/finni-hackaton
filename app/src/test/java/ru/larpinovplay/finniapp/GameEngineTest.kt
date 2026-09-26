package ru.larpinovplay.finniapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.larpinovplay.finniapp.data.content.defaultContent
import ru.larpinovplay.finniapp.domain.game.engine.GameEngine
import ru.larpinovplay.finniapp.domain.game.engine.GameRules
import ru.larpinovplay.finniapp.domain.game.model.BudgetDirection
import ru.larpinovplay.finniapp.domain.game.model.BudgetPlan
import ru.larpinovplay.finniapp.domain.game.model.ConfirmPlanResult
import ru.larpinovplay.finniapp.domain.game.model.DepositResult
import ru.larpinovplay.finniapp.domain.game.model.FinishBlock
import ru.larpinovplay.finniapp.domain.game.model.FinishWeekResult
import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.game.model.LedgerReason
import ru.larpinovplay.finniapp.domain.game.model.PeriodPhase
import ru.larpinovplay.finniapp.domain.game.model.PurchaseResult
import ru.larpinovplay.finniapp.domain.game.model.TaskStatus
import ru.larpinovplay.finniapp.domain.game.model.Transition
import ru.larpinovplay.finniapp.domain.game.model.WeekSummary
import ru.larpinovplay.finniapp.domain.game.model.WithdrawResult
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetGrowthStage
import ru.larpinovplay.finniapp.domain.pet.model.PetLook
import ru.larpinovplay.finniapp.domain.pet.model.PetSpecies
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.task.model.TaskAnswer
import ru.larpinovplay.finniapp.domain.task.model.TaskPayload
import java.time.LocalDate

/** Правила игры без корутин, репозиториев и Android: снимок на входе, снимок и результат на выходе. */
class GameEngineTest {

    private val content = defaultContent()
    private val food = content.shopItems.first { it.category == ShopCategory.MANDATORY }
    private val goal = content.goals.first()

    private val day1 = LocalDate.of(2026, 9, 1)
    private val day2 = day1.plusDays(1)

    private fun newGame(startBalance: Int = 100) = GameSnapshot(
        state = GameEngine.newGame(day1, startBalance),
        pet = Pet.newborn("Финни", PetLook(PetSpecies.BUNNY, PetColor.CORAL)),
    )

    /** Прогоняет цепочку команд, отдавая каждой снимок предыдущей. */
    private fun GameSnapshot.then(step: (GameSnapshot) -> Transition<*>): GameSnapshot = step(this).game

    /** Подтверждает план: [mandatory] и [savings] как заданы, остальное — на необязательное. */
    private fun GameSnapshot.planned(mandatory: Int = 0, savings: Int = 0): GameSnapshot =
        then { GameEngine.confirmPlan(it, BudgetPlan(mandatory, it.state.balance - mandatory - savings, savings)) }

    /** Закрывает неделю, которая обязана закрыться. */
    private fun GameSnapshot.finish(today: LocalDate = day2): Pair<GameSnapshot, WeekSummary> {
        val (game, result) = GameEngine.finishWeek(this, today)
        return game to (result as FinishWeekResult.Finished).summary
    }

    @Test
    fun startBalanceIsPostedToLedger() {
        val game = newGame(startBalance = 100)

        assertEquals(100, game.state.balance)
        assertEquals(1, game.state.ledger.size)
    }

    @Test
    fun buyingFoodSpendsCoinsFeedsPetAndCoversFood() {
        val before = newGame()

        val (game, result) = GameEngine.buy(before, food)

        assertEquals(PurchaseResult.Success(food, balanceAfter = 100 - food.price), result)
        assertEquals(100 - food.price, game.state.balance)
        assertTrue(game.state.foodCovered)
        assertEquals(2, game.state.ledger.size)
        assertEquals(before.pet.satiety.value + food.satiety, game.pet.satiety.value)
        assertEquals(before.pet.mood.value + food.mood, game.pet.mood.value)
    }

    @Test
    fun buyingWithoutEnoughCoinsChangesNothing() {
        val before = newGame(startBalance = 5)

        val (game, result) = GameEngine.buy(before, food)

        assertEquals(PurchaseResult.NotEnough(missing = food.price - 5), result)
        assertEquals(before, game)
    }

    @Test
    fun depositMovesCoinsToSavingsAndRejectsMoreThanBalance() {
        val start = newGame(startBalance = 50)

        val (afterFirst, first) = GameEngine.deposit(start, 20)
        val (afterSecond, second) = GameEngine.deposit(afterFirst, 31)
        val (_, third) = GameEngine.deposit(afterFirst, 0)

        assertEquals(DepositResult.Success, first)
        assertEquals(DepositResult.Rejected(balance = 30), second)
        assertEquals(DepositResult.Rejected(balance = 30), third)
        assertEquals(afterFirst, afterSecond)
        assertEquals(30, afterFirst.state.balance)
        assertEquals(20, afterFirst.state.savings)
    }

    @Test
    fun weeksToGoalIsCeilOfRemainingOverWeeklyDeposit() {
        val chosen = newGame().then { GameEngine.chooseGoal(it, goal) }
        assertNull(chosen.state.weeksToGoal())   // ещё ничего не отложено

        val saved = chosen.then { GameEngine.deposit(it, 30) }

        val remaining = goal.cost - 30
        assertEquals((remaining + 29) / 30, saved.state.weeksToGoal())
    }

    @Test
    fun reachingGoalSpendsSavingsClearsGoalAndCheersPet() {
        val chosen = newGame(startBalance = 200).then { GameEngine.chooseGoal(it, goal) }
        assertNull(GameEngine.reachGoal(chosen).result)   // пока не накоплено

        val saved = chosen.then { GameEngine.deposit(it, goal.cost) }
        val (game, reached) = GameEngine.reachGoal(saved)

        assertEquals(goal, reached)
        assertNull(game.state.goal)
        assertEquals(0, game.state.savings)
        assertEquals(listOf(goal), game.state.completedGoals)
        assertEquals(saved.pet.mood.value + 30, game.pet.mood.value)
    }

    @Test
    fun newGameStartsWithPlanOnFirstDay() {
        val game = newGame()

        assertEquals(PeriodPhase.PLANNING, game.state.phase)
        assertEquals(day1, game.state.periodStartedOn)
        assertEquals(100, game.state.weekIncome)
    }

    @Test
    fun planMustSplitWholeBalanceOnceAndWithoutMinus() {
        val start = newGame()

        val (_, tooSmall) = GameEngine.confirmPlan(start, BudgetPlan(mandatory = 20, optional = 20))
        val (_, withMinus) = GameEngine.confirmPlan(start, BudgetPlan(mandatory = 110, optional = -10))
        val (planned, ok) = GameEngine.confirmPlan(start, BudgetPlan(mandatory = 20, optional = 60, savings = 20))
        val (_, again) = GameEngine.confirmPlan(planned, BudgetPlan(optional = 100))

        assertEquals(ConfirmPlanResult.Rejected(budget = 100), tooSmall)
        assertEquals(ConfirmPlanResult.Rejected(budget = 100), withMinus)
        assertEquals(ConfirmPlanResult.Success, ok)
        assertEquals(PeriodPhase.ACTIVE, planned.state.phase)
        assertEquals(BudgetPlan(20, 60, 20), planned.state.plan)
        assertEquals(ConfirmPlanResult.Rejected(budget = 80), again)
    }

    @Test
    fun confirmedPlanSendsSavingsLineToPiggyBankRightAway() {
        val (game, _) = GameEngine.confirmPlan(newGame(), BudgetPlan(mandatory = 20, optional = 60, savings = 20))

        assertEquals(80, game.state.balance)
        assertEquals(20, game.state.savings)
        assertEquals(20, game.state.savedThisWeek)
        assertEquals(LedgerReason.PlannedDeposit, game.state.ledger.last().reason)
    }

    @Test
    fun planLeftShowsWhatRemainsPerCategoryAndGoesBelowZeroWhenOverspent() {
        val treat = content.shopItems.first { it.category == ShopCategory.OPTIONAL }
        assertNull(newGame().state.planLeft(ShopCategory.MANDATORY))   // плана ещё нет

        val game = newGame()
            .then { GameEngine.confirmPlan(it, BudgetPlan(mandatory = 30, optional = 5, savings = 65)) }
            .then { GameEngine.buy(it, food) }
            .then { GameEngine.buy(it, treat) }

        assertEquals(30 - food.price, game.state.planLeft(ShopCategory.MANDATORY))
        assertEquals(5 - treat.price, game.state.planLeft(ShopCategory.OPTIONAL))
    }

    @Test
    fun planWithoutSavingsLineLeavesPiggyBankAlone() {
        val game = newGame().planned()

        assertEquals(0, game.state.savings)
        assertEquals(1, game.state.ledger.size)
    }

    @Test
    fun withdrawReturnsCoinsUpToWholePiggyBank() {
        val planned = newGame().planned(savings = 20)

        val (afterPart, part) = GameEngine.withdraw(planned, 15)
        val (_, tooMuch) = GameEngine.withdraw(afterPart, 6)
        val (_, zero) = GameEngine.withdraw(afterPart, 0)
        val (empty, rest) = GameEngine.withdraw(afterPart, 5)

        assertEquals(WithdrawResult.Success, part)
        assertEquals(WithdrawResult.Rejected(savings = 5), tooMuch)
        assertEquals(WithdrawResult.Rejected(savings = 5), zero)
        assertEquals(WithdrawResult.Success, rest)
        assertEquals(95, afterPart.state.balance)
        assertEquals(LedgerReason.Withdraw, afterPart.state.ledger.last().reason)
        assertEquals(0, empty.state.savings)
        assertEquals(100, empty.state.balance)
        assertEquals(0, empty.state.savedThisWeek)
    }

    @Test
    fun weekCannotFinishWithoutPlan() {
        val start = newGame()

        val (game, result) = GameEngine.finishWeek(start, day2)

        assertEquals(FinishWeekResult.Blocked(FinishBlock.PLAN_NOT_CONFIRMED), result)
        assertEquals(start, game)
    }

    @Test
    fun weekCannotFinishOnTheDayItStarted() {
        val planned = newGame().planned()

        val (game, result) = GameEngine.finishWeek(planned, day1)

        assertEquals(FinishWeekResult.Blocked(FinishBlock.SAME_DAY), result)
        assertEquals(planned, game)
        assertTrue(GameEngine.finishWeek(planned, day2).result is FinishWeekResult.Finished)
        // Часы перевели назад: игру не запираем
        assertTrue(GameEngine.finishWeek(planned, day1.minusDays(1)).result is FinishWeekResult.Finished)
    }

    @Test
    fun weekWaitsForAdventureButNotForRightAnswers() {
        val adventures = content.adventures
        val planned = newGame().planned()

        val (_, blocked) = GameEngine.finishWeek(planned, day2, adventures)
        val (played, result) = GameEngine.completeAdventure(planned, adventures.first(), mistakes = 3, adventures = adventures)

        assertEquals(FinishWeekResult.Blocked(FinishBlock.ADVENTURE_NOT_PLAYED), blocked)
        assertEquals(adventures.first().rewardOnMistake, checkNotNull(result).reward)
        assertEquals(100 + adventures.first().rewardOnMistake, played.state.balance)
        assertEquals(LedgerReason.AdventureReward(adventures.first().title), played.state.ledger.last().reason)
        assertTrue(GameEngine.finishWeek(played, day2, adventures).result is FinishWeekResult.Finished)
    }

    @Test
    fun oneAdventurePerWeekInOrderAndUnplayedOneWaits() {
        val first = content.adventures.first()
        val second = first.copy(id = "second", title = "Второе")
        val adventures = listOf(first, second)
        val week1 = newGame().planned()

        val (_, outOfOrder) = GameEngine.completeAdventure(week1, second, mistakes = 0, adventures = adventures)
        val afterFirst = week1.then { GameEngine.completeAdventure(it, first, mistakes = 0, adventures = adventures) }
        val (_, twice) = GameEngine.completeAdventure(afterFirst, first, mistakes = 0, adventures = adventures)

        assertNull(outOfOrder)
        assertNull(twice)
        assertNull(afterFirst.state.adventureOfWeek(adventures))   // одно за неделю
        // Неделя 2: второе ждёт; не сыграл — оно же ждёт и на неделе 3
        val week2 = GameEngine.finishWeek(afterFirst, day2, adventures).game
        assertEquals(second, week2.state.adventureOfWeek(adventures))
    }

    @Test
    fun demoWeekFinishesTheSameDay() {
        val start = newGame()
        val demo = start.copy(state = start.state.copy(demoMode = true)).planned()

        assertTrue(GameEngine.finishWeek(demo, day1).result is FinishWeekResult.Finished)
    }

    @Test
    fun finishingGoodWeekGivesThreeStarsGrowsPetAndStartsNewWeek() {
        val week = newGame()
            .planned(mandatory = food.price, savings = 10)
            .then { GameEngine.buy(it, food) }

        val (game, summary) = week.finish()

        assertEquals(3, summary.score)
        assertTrue(summary.foodCovered && summary.savedSomething && summary.planKept)
        assertEquals(BudgetPlan(food.price, 100 - food.price - 10, 10), summary.plan)
        assertEquals(food.price, summary.fact(BudgetDirection.MANDATORY))
        assertEquals(GameRules.WEEK_INCOME, summary.nextIncome)
        with(game.state) {
            assertEquals(2, this.week)
            assertEquals(PeriodPhase.PLANNING, phase)
            assertNull(plan)
            assertEquals(day2, periodStartedOn)
            assertEquals(GameRules.WEEK_INCOME, weekIncome)
            assertEquals(100 - food.price - 10 + GameRules.WEEK_INCOME, balance)
            assertTrue(purchases.isEmpty())
            assertTrue(depositsThisWeek.isEmpty())
            assertEquals(listOf(10), depositsByWeek)
            assertEquals(listOf(summary), history)
        }
        assertEquals(3, game.pet.growthPoints)
        assertEquals(week.pet.satiety.value - 35, game.pet.satiety.value)
        assertEquals(-10 + 20, summary.moodDelta)
        assertEquals(week.pet.mood.value + summary.moodDelta, game.pet.mood.value)
    }

    @Test
    fun buyingMoreOptionalThanPlannedBreaksPlan() {
        val treat = content.shopItems.first { it.category == ShopCategory.OPTIONAL }
        val (_, summary) = newGame()
            .then { GameEngine.confirmPlan(it, BudgetPlan(mandatory = 90, optional = 0, savings = 10)) }
            .then { GameEngine.buy(it, food) }
            .then { GameEngine.buy(it, treat) }
            .finish()

        assertFalse(summary.planKept)
        assertTrue(summary.optionalOverPlan)
        assertEquals(2, summary.score)
    }

    @Test
    fun withdrawingBelowPromiseBreaksPlanButKeepsSavingsStarIfSomethingStays() {
        val (_, summary) = newGame()
            .planned(mandatory = food.price, savings = 20)
            .then { GameEngine.buy(it, food) }
            .then { GameEngine.withdraw(it, 15) }
            .finish()

        assertFalse(summary.planKept)
        assertTrue(summary.savingsUnderPlan)
        assertTrue(summary.savedSomething)
        assertEquals(5, summary.saved)
        assertEquals(15, summary.withdrawn)
        assertEquals(2, summary.score)
    }

    @Test
    fun extraDepositCoversWithdrawalWithinTheWeek() {
        val (_, summary) = newGame()
            .planned(mandatory = food.price, savings = 10)
            .then { GameEngine.withdraw(it, 10) }
            .then { GameEngine.deposit(it, 10) }
            .then { GameEngine.buy(it, food) }
            .finish()

        assertTrue(summary.planKept)
        assertEquals(3, summary.score)
    }

    @Test
    fun buyingGoalIsNotWithdrawal() {
        val cheap = goal.copy(cost = 10)
        val (_, summary) = newGame()
            .then { GameEngine.chooseGoal(it, cheap) }
            .planned(mandatory = food.price, savings = 10)
            .then { GameEngine.reachGoal(it) }
            .then { GameEngine.buy(it, food) }
            .finish()

        assertTrue(summary.savedSomething && summary.planKept)
        assertEquals(0, summary.withdrawn)
    }

    @Test
    fun petGrowsToTeenAfterTwoGoodWeeks() {
        var game = newGame(startBalance = 1_000)
        var day = day1

        repeat(2) {
            day = day.plusDays(1)
            game = game
                .planned(mandatory = food.price, savings = 10)
                .then { GameEngine.buy(it, food) }
                .finish(day).first
        }

        assertEquals(PetGrowthStage.TEEN, game.pet.growthStage)
        assertEquals(10 - 6, game.pet.pointsToNextStage)
    }

    @Test
    fun summaryReportsStageChange() {
        val start = newGame(startBalance = 1_000)
        val nearTeen = start.copy(pet = start.pet.grow(4))   // на очко до подростка

        val (_, summary) = nearTeen.planned(mandatory = food.price).then { GameEngine.buy(it, food) }.finish()

        assertEquals(PetGrowthStage.BABY, summary.stageBefore)
        assertEquals(PetGrowthStage.TEEN, summary.stageAfter)
    }

    @Test
    fun weekWithBrokenPromiseAndNoFoodScoresZeroAndLowersMood() {
        val before = newGame().planned(savings = 10).then { GameEngine.withdraw(it, 10) }

        val (game, summary) = before.finish()

        assertEquals(0, summary.score)
        assertEquals(-25, summary.moodDelta)
        assertEquals(before.pet.mood.value - 25, game.pet.mood.value)
        assertEquals(0, game.pet.growthPoints)
    }

    @Test
    fun answeringTaskPaysRewardOnceAndLimitsTasksPerWeek() {
        val choice = content.tasks.first { it.payload is TaskPayload.Choice }
        val correct = (choice.payload as TaskPayload.Choice).options.first { it.correct }
        val start = newGame()

        val (game, outcome) = GameEngine.answerTask(start, choice, TaskAnswer.Choice(correct.id))
        val (again, repeated) = GameEngine.answerTask(game, choice, TaskAnswer.Choice(correct.id))

        assertTrue(checkNotNull(outcome).success)
        assertEquals(100 + choice.reward, game.state.balance)
        assertEquals(TaskStatus.DONE, game.state.taskStatus(choice))
        assertNull(repeated)   // повторно то же задание не засчитывается
        assertEquals(game, again)
    }

    @Test
    fun topicProgressCountsDoneTasksPerTopic() {
        val choice = content.tasks.first { it.payload is TaskPayload.Choice }
        val correct = (choice.payload as TaskPayload.Choice).options.first { it.correct }

        val game = newGame().then { GameEngine.answerTask(it, choice, TaskAnswer.Choice(correct.id)) }

        val progress = game.state.topicProgress(content.tasks)
        assertEquals(content.tasks.count { it.topic == choice.topic }, progress.first { it.topic == choice.topic }.total)
        assertEquals(1, progress.sumOf { it.done })
    }
}
