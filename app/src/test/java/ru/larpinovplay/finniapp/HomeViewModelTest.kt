package ru.larpinovplay.finniapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import ru.larpinovplay.finniapp.data.content.defaultContent
import ru.larpinovplay.finniapp.data.game.GameRepositoryImpl
import ru.larpinovplay.finniapp.data.settings.InMemorySettingsRepository
import ru.larpinovplay.finniapp.domain.game.engine.GameRules
import ru.larpinovplay.finniapp.domain.game.model.BudgetDirection
import ru.larpinovplay.finniapp.domain.game.model.BudgetPlan
import ru.larpinovplay.finniapp.domain.game.model.FinishBlock
import ru.larpinovplay.finniapp.domain.game.model.PeriodPhase
import ru.larpinovplay.finniapp.domain.game.repository.requireSnapshot
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.presentation.screens.home.HomeAction
import ru.larpinovplay.finniapp.presentation.screens.home.HomeUiState
import ru.larpinovplay.finniapp.presentation.screens.home.HomeViewModel

/** Начало и конец недели на главном экране: окно плана, кнопка конца недели и итоги. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val content = defaultContent()
    private val clock = TestClock()
    private val game = GameRepositoryImpl(FakeGameStore(), clock = clock)

    @Before
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun resetMain() = Dispatchers.resetMain()

    private fun viewModel(): HomeViewModel {
        runBlocking { game.createPet(SampleGames.newborn) }
        return HomeViewModel(game, content, InMemorySettingsRepository())
    }

    private val HomeViewModel.draft: HomeUiState.PlanDraft get() = checkNotNull(state.value?.planDraft)

    private fun HomeViewModel.press(direction: BudgetDirection, increase: Boolean, times: Int = 1) =
        repeat(times) { onAction(HomeAction.ChangePlan(direction, increase)) }

    @Test
    fun firstWeekAsksToSplitStartCoins() {
        val vm = viewModel()

        val draft = vm.draft
        assertEquals(GameRules.START_BALANCE, draft.budget)
        assertEquals(0, draft.carried)
        assertEquals(BudgetPlan(), draft.plan)
        assertEquals(content.shopItems.filter { it.category == ShopCategory.MANDATORY }.minOf { it.price }, draft.need)
        assertEquals(FinishBlock.PLAN_NOT_CONFIRMED, vm.state.value?.finishBlock)
    }

    @Test
    fun plusStopsAtBudgetAndMinusStopsAtZero() {
        val vm = viewModel()

        vm.press(BudgetDirection.MANDATORY, increase = false)
        vm.press(BudgetDirection.SAVINGS, increase = true, times = 20)
        vm.press(BudgetDirection.OPTIONAL, increase = true)

        assertEquals(BudgetPlan(savings = GameRules.START_BALANCE), vm.draft.plan)
        assertEquals(0, vm.draft.unallocated)
    }

    @Test
    fun confirmingPlanClosesWindowAndStartsWeek() {
        val vm = viewModel()
        vm.onAction(HomeAction.ConfirmPlan)   // ещё не всё разложено — ничего не происходит
        assertNotNull(vm.state.value?.planDraft)

        vm.press(BudgetDirection.MANDATORY, increase = true, times = 2)
        vm.press(BudgetDirection.OPTIONAL, increase = true, times = 8)
        vm.onAction(HomeAction.ConfirmPlan)

        assertNull(vm.state.value?.planDraft)
        assertEquals(PeriodPhase.ACTIVE, game.requireSnapshot().state.phase)
        assertEquals(BudgetPlan(mandatory = 10, optional = 40), game.requireSnapshot().state.plan)
        assertEquals(FinishBlock.SAME_DAY, vm.state.value?.finishBlock)
    }

    @Test
    fun finishingTooEarlyExplainsWhy() {
        val vm = viewModel()
        vm.press(BudgetDirection.OPTIONAL, increase = true, times = 10)
        vm.onAction(HomeAction.ConfirmPlan)

        vm.onAction(HomeAction.FinishWeek)

        assertEquals(FinishBlock.SAME_DAY, vm.state.value?.finishNotice)
        assertNull(vm.state.value?.weekSummary)
        vm.onAction(HomeAction.DismissFinishNotice)
        assertNull(vm.state.value?.finishNotice)
    }

    @Test
    fun nextDayShowsSummaryThenPlanStartingFromLastPlan() {
        val vm = viewModel()
        vm.press(BudgetDirection.MANDATORY, increase = true, times = 2)
        vm.press(BudgetDirection.OPTIONAL, increase = true, times = 4)
        vm.press(BudgetDirection.SAVINGS, increase = true, times = 4)
        vm.onAction(HomeAction.ConfirmPlan)
        clock.nextDay()

        vm.onAction(HomeAction.FinishWeek)

        val summary = checkNotNull(vm.state.value?.weekSummary)
        assertEquals(BudgetPlan(10, 20, 20), summary.plan)
        // 20 ушли в копилку сразу, остальное не потрачено и переходит на новую неделю; план начинается с прошлого
        assertEquals(20, game.requireSnapshot().state.savings)
        val draft = vm.draft
        assertEquals(2, draft.week)
        assertEquals(GameRules.WEEK_INCOME, draft.income)
        assertEquals(GameRules.START_BALANCE - 20, draft.carried)
        assertEquals(BudgetPlan(10, 20, 20), draft.plan)
        assertEquals(draft.budget - 50, draft.unallocated)

        vm.onAction(HomeAction.DismissWeekSummary)
        assertNull(vm.state.value?.weekSummary)
        assertNotNull(vm.state.value?.planDraft)
    }
}
