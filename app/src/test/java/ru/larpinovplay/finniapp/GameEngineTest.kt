package ru.larpinovplay.finniapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.larpinovplay.finniapp.data.content.defaultContent
import ru.larpinovplay.finniapp.domain.game.engine.GameEngine
import ru.larpinovplay.finniapp.domain.game.model.DepositResult
import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.game.model.PurchaseResult
import ru.larpinovplay.finniapp.domain.game.model.TaskStatus
import ru.larpinovplay.finniapp.domain.game.model.Transition
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetGrowthStage
import ru.larpinovplay.finniapp.domain.pet.model.PetLook
import ru.larpinovplay.finniapp.domain.pet.model.PetSpecies
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.task.model.TaskAnswer
import ru.larpinovplay.finniapp.domain.task.model.TaskPayload

/** Правила игры без корутин, репозиториев и Android: снимок на входе, снимок и результат на выходе. */
class GameEngineTest {

    private val content = defaultContent()
    private val food = content.shopItems.first { it.category == ShopCategory.MANDATORY }
    private val goal = content.goals.first()

    private fun newGame(startBalance: Int = 100) = GameSnapshot(
        state = GameEngine.newGame(startBalance),
        pet = Pet.newborn("Финни", PetLook(PetSpecies.BUNNY, PetColor.CORAL)),
    )

    /** Прогоняет цепочку команд, отдавая каждой снимок предыдущей. */
    private fun GameSnapshot.then(step: (GameSnapshot) -> Transition<*>): GameSnapshot = step(this).game

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
    fun finishingGoodWeekScoresTwoGrowsPetAndPaysIncome() {
        val week = newGame()
            .then { GameEngine.buy(it, food) }
            .then { GameEngine.deposit(it, 10) }

        val (game, summary) = GameEngine.finishWeek(week)

        assertEquals(2, summary.score)
        assertTrue(summary.foodCovered && summary.savedSomething)
        with(game.state) {
            assertEquals(2, this.week)
            assertEquals(100 - food.price - 10 + 60, balance)
            assertTrue(purchases.isEmpty())
            assertTrue(depositsThisWeek.isEmpty())
            assertEquals(listOf(10), depositsByWeek)
            assertEquals(listOf(summary), history)
        }
        assertEquals(2, game.pet.growthPoints)
        assertEquals(week.pet.satiety.value - 35, game.pet.satiety.value)
        assertEquals(week.pet.mood.value + summary.moodDelta, game.pet.mood.value)
    }

    @Test
    fun petGrowsToTeenAfterEnoughGoodWeeks() {
        var game = newGame(startBalance = 1_000)

        repeat(3) {
            game = game
                .then { GameEngine.buy(it, food) }
                .then { GameEngine.deposit(it, 10) }
                .then { GameEngine.finishWeek(it) }
        }

        assertEquals(PetGrowthStage.TEEN, game.pet.growthStage)
        assertEquals(10 - 6, game.pet.pointsToNextStage)
    }

    @Test
    fun summaryReportsStageChange() {
        val start = newGame(startBalance = 1_000)
        val nearTeen = start.copy(pet = start.pet.grow(4))   // на очко до подростка

        val (_, summary) = GameEngine.finishWeek(nearTeen.then { GameEngine.buy(it, food) })

        assertEquals(PetGrowthStage.BABY, summary.stageBefore)
        assertEquals(PetGrowthStage.TEEN, summary.stageAfter)
    }

    @Test
    fun finishingEmptyWeekScoresZeroAndLowersMood() {
        val before = newGame()

        val (game, summary) = GameEngine.finishWeek(before)

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
