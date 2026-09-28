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
import ru.larpinovplay.finniapp.domain.game.model.Trip
import ru.larpinovplay.finniapp.domain.game.model.WeekSummary
import ru.larpinovplay.finniapp.domain.pet.model.MoodLevel
import ru.larpinovplay.finniapp.domain.pet.model.PetMood
import ru.larpinovplay.finniapp.domain.game.model.weekSatiety
import ru.larpinovplay.finniapp.domain.game.model.weekDeeds
import ru.larpinovplay.finniapp.domain.game.model.WeekDeeds
import ru.larpinovplay.finniapp.domain.game.model.WithdrawResult
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetGrowthStage
import ru.larpinovplay.finniapp.domain.pet.model.PetLook
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.shop.model.WearableSlot
import ru.larpinovplay.finniapp.domain.task.model.TaskAnswer
import ru.larpinovplay.finniapp.domain.task.model.TaskPayload
import java.time.LocalDate

/** Правила игры без корутин, репозиториев и Android: снимок на входе, снимок и результат на выходе. */
class GameEngineTest {

    private val content = defaultContent()
    /** Еда, которой хватает на неделю: одна покупка закрывает дело «Финни сыт». */
    private val food = content.shopItems.first { it.category == ShopCategory.MANDATORY && it.satiety >= GameRules.WEEKLY_HUNGER }
    private val goal = content.goals.first()

    private val day1 = LocalDate.of(2026, 9, 1)
    private val day2 = day1.plusDays(1)

    private fun newGame(startBalance: Int = 100) = GameSnapshot(
        state = GameEngine.newGame(day1, startBalance),
        pet = Pet.newborn("Финни", PetLook(PetColor.CORAL)),
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

    private val cap = content.shopItems.first { it.slot == WearableSlot.HEAD }
    private val glasses = content.shopItems.first { it.slot == WearableSlot.EYES }

    @Test
    fun boughtClothesStayInWardrobeAfterWeekEnds() {
        val (game, result) = GameEngine.buy(newGame(), cap)

        assertEquals(PurchaseResult.Success(cap, balanceAfter = 100 - cap.price), result)
        assertEquals(listOf(cap), game.state.wardrobe)
        assertTrue(game.state.purchases.contains(cap))   // как любая покупка, это трата недели

        val nextWeek = game.planned().finish().first
        assertTrue(nextWeek.state.purchases.isEmpty())
        assertEquals(listOf(cap), nextWeek.state.wardrobe)
    }

    @Test
    fun clothesAreBoughtOnlyOnce() {
        val game = newGame().then { GameEngine.buy(it, cap) }

        val (again, result) = GameEngine.buy(game, cap)

        assertEquals(PurchaseResult.AlreadyOwned, result)
        assertEquals(game, again)
    }

    @Test
    fun onlyOwnedClothesCanBeWorn() {
        val (game, worn) = GameEngine.wear(newGame(), cap)

        assertFalse(worn)
        assertTrue(game.pet.outfit.isEmpty())
    }

    @Test
    fun oneItemPerSlotAndTakingOffFreesIt() {
        val owner = newGame(startBalance = 200)
            .then { GameEngine.buy(it, cap) }
            .then { GameEngine.buy(it, glasses) }
        val balance = owner.state.balance

        val dressed = owner.then { GameEngine.wear(it, cap) }.then { GameEngine.wear(it, glasses) }
        assertEquals(mapOf(WearableSlot.HEAD to cap.id, WearableSlot.EYES to glasses.id), dressed.pet.outfit)
        assertEquals(balance, dressed.state.balance)   // надевать бесплатно

        val undressed = dressed.then { GameEngine.takeOff(it, WearableSlot.HEAD) }
        assertEquals(mapOf(WearableSlot.EYES to glasses.id), undressed.pet.outfit)
    }

    @Test
    fun buyingFoodSpendsCoinsFeedsPetAndCoversFood() {
        val before = newGame()

        val (game, result) = GameEngine.buy(before, food)

        assertEquals(PurchaseResult.Success(food, balanceAfter = 100 - food.price), result)
        assertEquals(100 - food.price, game.state.balance)
        assertEquals(food.satiety, game.state.weekSatiety)
        assertEquals(2, game.state.ledger.size)
        assertEquals((before.pet.satiety.value + food.satiety).coerceAtMost(100), game.pet.satiety.value)
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
    fun goalRemainingNeverGoesBelowZero() {
        assertNull(newGame().state.goalRemaining())   // цели нет

        val saved = newGame().then { GameEngine.chooseGoal(it, goal) }.then { GameEngine.deposit(it, 30) }

        assertEquals(goal.cost - 30, saved.state.goalRemaining())
        assertEquals(goal.cost - 10, saved.state.goalRemaining(10))       // «если заберу 20»
        assertEquals(0, saved.state.goalRemaining(goal.cost + 5))         // накоплено больше цены
    }

    @Test
    fun tripGoalSendsPetAwayOnlyForThatWeek() {
        val sea = content.goals.first { it.trip }
        val saved = newGame(startBalance = 200).then { GameEngine.chooseGoal(it, sea) }.then { GameEngine.deposit(it, sea.cost) }
        assertNull(saved.state.currentTrip)

        val away = saved.then { GameEngine.reachGoal(it) }
        assertEquals(Trip(sea.id, away.state.week), away.state.currentTrip)

        val back = away.planned().finish().first
        assertNull(back.state.currentTrip)
        assertEquals(listOf(sea), back.state.completedGoals)
    }

    @Test
    fun ordinaryGoalIsNotATrip() {
        val game = newGame(startBalance = 200).then { GameEngine.chooseGoal(it, goal) }
            .then { GameEngine.deposit(it, goal.cost) }.then { GameEngine.reachGoal(it) }
        assertNull(game.state.currentTrip)
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
        assertEquals(saved.pet.mood.value + GameRules.GOAL_MOOD_BONUS, game.pet.mood.value)
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
    fun goodWeekMakesAllFourDeedsPaysSavingsBonusAndStartsNewWeek() {
        val week = newGame()
            .planned(mandatory = food.price, savings = 10)
            .then { GameEngine.buy(it, food) }

        val (game, summary) = week.finish()

        assertTrue(summary.deeds.all)
        assertEquals(4, summary.steps)
        assertEquals(BudgetPlan(food.price, 100 - food.price - 10, 10), summary.plan)
        assertEquals(food.price, summary.fact(BudgetDirection.MANDATORY))
        assertEquals(GameRules.SAVINGS_BONUS, summary.savingsBonus)
        assertEquals(GameRules.weekIncome(PetGrowthStage.BABY), summary.nextIncome)
        assertEquals(PetGrowthStage.TEEN.minGrowthPoints - 4, summary.stepsToNextStage)
        with(game.state) {
            assertEquals(2, this.week)
            assertEquals(PeriodPhase.PLANNING, phase)
            assertNull(plan)
            assertEquals(day2, periodStartedOn)
            assertEquals(GameRules.weekIncome(PetGrowthStage.BABY), weekIncome)
            assertEquals(100 - food.price - 10 + GameRules.weekIncome(PetGrowthStage.BABY), balance)
            assertEquals(10 + GameRules.SAVINGS_BONUS, savings)
            assertTrue(ledger.any { it.reason == LedgerReason.SavingsBonus })
            assertTrue(purchases.isEmpty())
            assertTrue(depositsThisWeek.isEmpty())
            assertEquals(listOf(10 + GameRules.SAVINGS_BONUS), depositsByWeek)
            assertEquals(listOf(summary), history)
        }
        assertEquals(4, game.pet.growthPoints)
        assertEquals(week.pet.satiety.value - GameRules.WEEKLY_HUNGER, game.pet.satiety.value)
        // Настроение от дел не зависит: неделя просто снижает его
        assertEquals(-GameRules.WEEKLY_MOOD_DECAY, summary.moodDelta)
        assertEquals(week.pet.mood.value - GameRules.WEEKLY_MOOD_DECAY, game.pet.mood.value)
    }

    @Test
    fun deedsAreVisibleDuringTheWeek() {
        val fruits = content.shopItems.first { it.id == "fruits" }
        val vegetables = content.shopItems.first { it.id == "vegetables" }
        val start = newGame()
        assertEquals(WeekDeeds(fed = false, notBored = true, savingsOnPlan = false, spendingOnPlan = false), start.weekDeeds)

        val planned = start.planned(savings = 10)
        val fruitOnly = planned.then { GameEngine.buy(it, fruits) }
        val fullWeek = fruitOnly.then { GameEngine.buy(it, vegetables) }

        assertTrue(planned.weekDeeds.savingsOnPlan && planned.weekDeeds.spendingOnPlan)
        assertFalse(fruitOnly.weekDeeds.fed)   // 30 сытости из 35 — на неделю не хватит
        assertTrue(fullWeek.weekDeeds.fed)
    }

    @Test
    fun buyingMoreOptionalThanPlannedCostsOnlyThatDeed() {
        val treat = content.shopItems.first { it.category == ShopCategory.OPTIONAL && !it.isWearable }
        val (_, summary) = newGame()
            .then { GameEngine.confirmPlan(it, BudgetPlan(mandatory = 90, optional = 0, savings = 10)) }
            .then { GameEngine.buy(it, food) }
            .then { GameEngine.buy(it, treat) }
            .finish()

        assertFalse(summary.deeds.spendingOnPlan)
        assertTrue(summary.optionalOverPlan)
        assertEquals(3, summary.steps)
    }

    @Test
    fun withdrawingBelowPromiseCostsSavingsDeedAndBonus() {
        val (_, summary) = newGame()
            .planned(mandatory = food.price, savings = 20)
            .then { GameEngine.buy(it, food) }
            .then { GameEngine.withdraw(it, 15) }
            .finish()

        assertFalse(summary.deeds.savingsOnPlan)
        assertTrue(summary.savingsUnderPlan)
        assertEquals(5, summary.saved)
        assertEquals(15, summary.withdrawn)
        assertEquals(0, summary.savingsBonus)   // отложено 5, меньше 10
        assertEquals(3, summary.steps)
    }

    @Test
    fun extraDepositCoversWithdrawalWithinTheWeek() {
        val (_, summary) = newGame()
            .planned(mandatory = food.price, savings = 10)
            .then { GameEngine.withdraw(it, 10) }
            .then { GameEngine.deposit(it, 10) }
            .then { GameEngine.buy(it, food) }
            .finish()

        assertTrue(summary.deeds.savingsOnPlan)
        assertEquals(4, summary.steps)
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

        assertTrue(summary.deeds.savingsOnPlan)
        assertEquals(0, summary.withdrawn)
    }

    @Test
    fun twoWeeksWithAllDeedsGrowPetToTeenAndRaisePocketMoney() {
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
        assertEquals(PetGrowthStage.ADULT.minGrowthPoints - 8, game.pet.pointsToNextStage)
        assertEquals(GameRules.weekIncome(PetGrowthStage.TEEN), game.state.weekIncome)
    }

    @Test
    fun summaryReportsStageChange() {
        val start = newGame(startBalance = 1_000)
        val nearTeen = start.copy(pet = start.pet.grow(PetGrowthStage.TEEN.minGrowthPoints - 1))

        val (_, summary) = nearTeen.planned(mandatory = food.price).then { GameEngine.buy(it, food) }.finish()

        assertEquals(PetGrowthStage.BABY, summary.stageBefore)
        assertEquals(PetGrowthStage.TEEN, summary.stageAfter)
        assertEquals(GameRules.weekIncome(PetGrowthStage.TEEN), summary.nextIncome)
    }

    @Test
    fun boredHungryWeekGivesFewStepsAndMoodStopsAtBored() {
        val start = newGame()
        val bored = start.copy(pet = start.pet.copy(mood = PetMood(30)))
            .planned(savings = 10)
            .then { GameEngine.withdraw(it, 10) }

        val (game, summary) = bored.finish()

        assertEquals(WeekDeeds(fed = false, notBored = false, savingsOnPlan = false, spendingOnPlan = true), summary.deeds)
        assertEquals(1, game.pet.growthPoints)
        assertEquals(PetMood.MIN, game.pet.mood.value)   // ниже «скучает» не опускается
        assertEquals(MoodLevel.BORED, game.pet.mood.level)
    }

    @Test
    fun clothesAndReachedGoalsSoftenWeeklyMoodDrop() {
        val bowtie = content.shopItems.first { it.id == "bowtie" }
        val glasses = content.shopItems.first { it.id == "glasses" }
        val dressed = newGame(startBalance = 1_000)
            .then { GameEngine.buy(it, bowtie) }
            .then { GameEngine.buy(it, glasses) }
            .planned(mandatory = food.price)

        val (_, summary) = dressed.finish()

        assertEquals(2 * GameRules.LASTING_MOOD_PER_ITEM - GameRules.WEEKLY_MOOD_DECAY, summary.moodDelta)
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

    private val choiceTasks = content.tasks.filter { it.payload is TaskPayload.Choice }
    private fun rightAnswer(task: ru.larpinovplay.finniapp.domain.task.model.Task) =
        TaskAnswer.Choice((task.payload as TaskPayload.Choice).options.first { it.correct }.id)
    private fun wrongAnswer(task: ru.larpinovplay.finniapp.domain.task.model.Task) =
        TaskAnswer.Choice((task.payload as TaskPayload.Choice).options.first { !it.correct }.id)

    /** ТЗ 2.5.8: в демо все задания доступны сразу — без недельного лимита и без ожидания после ошибки. */
    @Test
    fun demoLiftsWeeklyLimitAndLetsRetryAtOnce() {
        var game = newGame().let { it.copy(state = it.state.copy(demoMode = true)) }
        choiceTasks.take(GameRules.TASKS_PER_WEEK).forEach { game = GameEngine.answerTask(game, it, rightAnswer(it)).game }
        val next = choiceTasks[GameRules.TASKS_PER_WEEK]
        assertEquals(TaskStatus.AVAILABLE, game.state.taskStatus(next))
        assertEquals(null, game.state.tasksPerWeek)

        // Ошибочный вариант, и сразу же правильный — в ту же неделю
        val (afterMistake, wrong) = GameEngine.answerTask(game, next, wrongAnswer(next))
        assertFalse(checkNotNull(wrong).success)
        assertEquals(TaskStatus.AVAILABLE, afterMistake.state.taskStatus(next))
        val (afterRight, right) = GameEngine.answerTask(afterMistake, next, rightAnswer(next))
        assertTrue(checkNotNull(right).success)
        assertEquals(TaskStatus.DONE, afterRight.state.taskStatus(next))
    }

    @Test
    fun outsideDemoWeeklyLimitAndRetryWaitStay() {
        var game = newGame()
        choiceTasks.take(GameRules.TASKS_PER_WEEK).forEach { game = GameEngine.answerTask(game, it, rightAnswer(it)).game }
        assertEquals(TaskStatus.LIMIT_REACHED, game.state.taskStatus(choiceTasks[GameRules.TASKS_PER_WEEK]))
        assertEquals(GameRules.TASKS_PER_WEEK, game.state.tasksPerWeek)

        val task = choiceTasks.first()
        val mistaken = newGame().then { GameEngine.answerTask(it, task, wrongAnswer(task)) }
        assertEquals(TaskStatus.RETRY_NEXT_WEEK, mistaken.state.taskStatus(task))
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
