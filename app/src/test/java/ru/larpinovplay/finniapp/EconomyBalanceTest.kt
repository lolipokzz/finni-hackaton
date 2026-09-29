package ru.larpinovplay.finniapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.larpinovplay.finniapp.data.content.defaultContent
import ru.larpinovplay.finniapp.domain.game.engine.GameEngine
import ru.larpinovplay.finniapp.domain.game.engine.GameRules
import ru.larpinovplay.finniapp.domain.game.model.BudgetPlan
import ru.larpinovplay.finniapp.domain.game.model.FinishWeekResult
import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.game.model.WeekSummary
import ru.larpinovplay.finniapp.domain.pet.model.PetGrowthStage
import ru.larpinovplay.finniapp.domain.shop.cheapestFoodFor
import java.time.LocalDate

/**
 * Баланс экономики (docs/11-economy.md, раздел 9): пять демо-недель для разных стратегий ребёнка через
 * настоящий движок. Главный принцип — ни одна крайняя стратегия не выигрывает: у каждой свой минус,
 * а сбалансированный план растит Финни быстрее всех и приводит к первой цели. Если правка чисел ломает
 * этот тест, она ломает и то, чему учит игра.
 */
class EconomyBalanceTest {

    private val content = defaultContent()
    private val vegetables = content.shopItems.first { it.id == "vegetables" }
    private val chips = content.shopItems.first { it.id == "chips" }
    private val lemonade = content.shopItems.first { it.id == "lemonade" }
    private val firstGoal = content.goals.minBy { it.cost }
    private val foodCost = checkNotNull(cheapestFoodFor(GameRules.WEEKLY_HUNGER, content.shopItems))
    private val day1 = LocalDate.of(2026, 9, 1)

    /** Как ребёнок распоряжается неделей: сколько отложить и на что потратить необязательное. */
    private class Strategy(val savings: (budget: Int) -> Int, val treats: (optional: Int) -> Int)

    private val balanced = Strategy(savings = { 15 }, treats = { 15 })
    private val spender = Strategy(savings = { 0 }, treats = { it })
    private val miser = Strategy(savings = { it - foodCost }, treats = { 0 })
    private val walletHoarder = Strategy(savings = { 0 }, treats = { 15 })   // копит в кошельке, копилку не трогает

    private class Run(val weeks: List<WeekSummary>, val game: GameSnapshot, val goalWeek: Int?) {
        val steps: Int get() = weeks.sumOf { it.steps }
    }

    private fun play(strategy: Strategy, weeks: Int = 5): Run {
        var game = GameSnapshot(GameEngine.newGame(day1), SampleGames.newborn)
        game = GameEngine.chooseGoal(game, firstGoal).game
        val summaries = mutableListOf<WeekSummary>()
        var goalWeek: Int? = null
        repeat(weeks) { i ->
            val budget = game.state.balance
            val savings = strategy.savings(budget).coerceIn(0, budget - foodCost)
            val optional = budget - foodCost - savings
            game = GameEngine.confirmPlan(game, BudgetPlan(foodCost, optional, savings)).game
            repeat(2) { game = GameEngine.buy(game, vegetables).game }   // еда на неделю по минимальной цене
            var toSpend = strategy.treats(optional).coerceAtMost(optional)
            for (treat in listOf(chips, lemonade, lemonade, chips, lemonade)) {
                if (treat.price <= toSpend) { game = GameEngine.buy(game, treat).game; toSpend -= treat.price }
            }
            if (goalWeek == null && GameEngine.reachGoal(game).result != null) {
                game = GameEngine.reachGoal(game).game
                goalWeek = i + 1
            }
            val (next, result) = GameEngine.finishWeek(game, day1.plusDays(i + 1L))
            summaries += (result as FinishWeekResult.Finished).summary
            game = next
        }
        return Run(summaries, game, goalWeek)
    }

    @Test
    fun balancedPlanDoesEveryDeedGrowsFastestAndReachesFirstDream() {
        val run = play(balanced)

        assertTrue(run.weeks.all { it.deeds.all })
        assertEquals(PetGrowthStage.ADULT, run.game.pet.growthStage)   // подросток на 2-й неделе, взрослый на 4-й
        assertNotNull(run.goalWeek)
        assertTrue("цель на неделе ${run.goalWeek}", run.goalWeek!! <= 4)
    }

    @Test
    fun noExtremeStrategyBeatsBalance() {
        val best = play(balanced).steps
        listOf(spender, miser, walletHoarder).forEach { assertTrue(play(it).steps < best) }
    }

    @Test
    fun spenderHasHappyPetButNoSavingsDeedAndNoDream() {
        val run = play(spender)

        assertTrue(run.weeks.none { it.deeds.savingsOnPlan })
        assertTrue(run.weeks.all { it.deeds.notBored })
        assertEquals(null, run.goalWeek)
    }

    @Test
    fun miserGetsDreamFirstButFinniGetsBored() {
        val run = play(miser)
        val balancedGoal = play(balanced).goalWeek!!

        assertTrue(run.goalWeek!! <= balancedGoal)
        assertTrue(run.weeks.any { !it.deeds.notBored })
    }

    @Test
    fun keepingMoneyInWalletCostsSavingsDeed() {
        val run = play(walletHoarder)

        assertTrue(run.weeks.none { it.deeds.savingsOnPlan })
        assertFalse(run.game.pet.growthStage == PetGrowthStage.ADULT)
    }

    @Test
    fun extraIncomeStaysUnderThirtyPercentOfPocketMoney() {
        val week = GameRules.weekIncome(PetGrowthStage.BABY)
        // Все уровни самой «дорогой» недели без ошибок
        val tasks = content.levels.groupBy { it.week }.values.maxOf { week -> week.sumOf { it.reward } }
        val adventure = content.adventures.maxOf { it.reward }

        assertTrue("сверху $tasks + $adventure при доходе $week", (tasks + adventure) * 100 <= week * 30)
    }
}
