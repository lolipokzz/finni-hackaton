package ru.larpinovplay.finniapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.larpinovplay.finniapp.data.content.defaultContent
import ru.larpinovplay.finniapp.data.game.GameRepositoryImpl
import ru.larpinovplay.finniapp.domain.game.model.BudgetPlan
import ru.larpinovplay.finniapp.domain.game.repository.requireSnapshot
import ru.larpinovplay.finniapp.presentation.screens.wardrobe.WardrobeAction
import ru.larpinovplay.finniapp.presentation.screens.wardrobe.WardrobeViewModel

/** Гардероб: нажатие надевает вещь, повторное — снимает; экран видит это сразу. */
@OptIn(ExperimentalCoroutinesApi::class)
class WardrobeViewModelTest {

    private val game = GameRepositoryImpl(FakeGameStore(), startBalance = 100, clock = TestClock())
    private val cap = defaultContent().shopItems.first { it.id == "cap" }

    @Before
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun resetMain() = Dispatchers.resetMain()

    @Test
    fun toggleWearsAndTakesOff() {
        runBlocking {
            game.createPet(SampleGames.newborn)
            game.confirmPlan(BudgetPlan(optional = game.requireSnapshot().state.balance))
            game.buy(cap)
        }
        val vm = WardrobeViewModel(game)
        assertFalse(vm.state.value.items.single().worn)

        vm.onAction(WardrobeAction.Toggle(cap))
        assertTrue(vm.state.value.items.single().worn)
        assertEquals(setOf("Acc_Cap"), vm.state.value.accessories)

        vm.onAction(WardrobeAction.Toggle(cap))
        assertFalse(vm.state.value.items.single().worn)
        assertTrue(vm.state.value.accessories.isEmpty())
    }
}
