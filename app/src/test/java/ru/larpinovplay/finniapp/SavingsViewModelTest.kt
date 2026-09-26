package ru.larpinovplay.finniapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import ru.larpinovplay.finniapp.data.content.defaultContent
import ru.larpinovplay.finniapp.data.game.GameRepositoryImpl
import ru.larpinovplay.finniapp.domain.game.model.BudgetPlan
import ru.larpinovplay.finniapp.domain.game.repository.requireSnapshot
import ru.larpinovplay.finniapp.presentation.screens.savings.SavingsAction
import ru.larpinovplay.finniapp.presentation.screens.savings.SavingsViewModel

/** Забрать из копилки: окно показывает последствия, сумма не выходит за то, что есть. */
@OptIn(ExperimentalCoroutinesApi::class)
class SavingsViewModelTest {

    private val content = defaultContent()
    private val game = GameRepositoryImpl(FakeGameStore(), startBalance = 100, clock = TestClock())

    @Before
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun resetMain() = Dispatchers.resetMain()

    /** Игра с целью и 20 монетами в копилке, отложенными по плану. */
    private fun viewModel(): SavingsViewModel {
        runBlocking {
            game.createPet(SampleGames.newborn)
            game.chooseGoal(content.goals.first())
            game.confirmPlan(BudgetPlan(mandatory = 20, optional = 60, savings = 20))
        }
        return SavingsViewModel(game, content)
    }

    @Test
    fun withdrawWindowShowsHowGoalMovesAway() {
        val vm = viewModel()

        vm.onAction(SavingsAction.WithdrawClicked)
        vm.onAction(SavingsAction.ChangeWithdraw(increase = true))

        val draft = checkNotNull(vm.state.value.withdraw)
        assertEquals(10, draft.amount)
        assertEquals(20, draft.savingsBefore)
        assertEquals(10, draft.savingsAfter)
        val state = game.requireSnapshot().state
        assertEquals(state.weeksToGoal(), draft.weeksBefore)
        assertEquals(state.weeksToGoal(10), draft.weeksAfter)
    }

    @Test
    fun amountStaysBetweenOneAndWholePiggyBank() {
        val vm = viewModel()
        vm.onAction(SavingsAction.WithdrawClicked)

        repeat(10) { vm.onAction(SavingsAction.ChangeWithdraw(increase = true)) }
        assertEquals(20, vm.state.value.withdraw?.amount)

        repeat(10) { vm.onAction(SavingsAction.ChangeWithdraw(increase = false)) }
        assertEquals(1, vm.state.value.withdraw?.amount)
    }

    @Test
    fun confirmingMovesCoinsToWalletAndClosesWindow() {
        val vm = viewModel()
        vm.onAction(SavingsAction.WithdrawClicked)

        vm.onAction(SavingsAction.ConfirmWithdraw)

        assertNull(vm.state.value.withdraw)
        assertEquals(15, game.requireSnapshot().state.savings)
        assertEquals(85, game.requireSnapshot().state.balance)
    }
}
