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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.larpinovplay.finniapp.data.content.defaultContent
import ru.larpinovplay.finniapp.data.game.GameRepositoryImpl
import ru.larpinovplay.finniapp.domain.game.model.BudgetPlan
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.game.repository.requireSnapshot
import ru.larpinovplay.finniapp.presentation.screens.shop.PurchaseFeedback
import ru.larpinovplay.finniapp.presentation.screens.shop.ShopAction
import ru.larpinovplay.finniapp.presentation.screens.shop.ShopUiState
import ru.larpinovplay.finniapp.presentation.screens.shop.ShopViewModel

/** Магазин показывает план недели по категориям и обновляет его после покупки. */
@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModelTest {

    private val content = defaultContent()
    private val food = content.shopItems.first { it.category == ShopCategory.MANDATORY }
    private val game = GameRepositoryImpl(FakeGameStore(), startBalance = 100, clock = TestClock())

    @Before
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun resetMain() = Dispatchers.resetMain()

    @Test
    fun withoutPlanNoBudgetsAreShown() {
        runBlocking { game.createPet(SampleGames.newborn) }

        assertTrue(ShopViewModel(game, content).state.value.budgets.isEmpty())
    }

    @Test
    fun budgetsFollowPlanAndPurchases() {
        runBlocking {
            game.createPet(SampleGames.newborn)
            game.confirmPlan(BudgetPlan(mandatory = 30, optional = 50, savings = 20))
        }
        val vm = ShopViewModel(game, content)
        assertEquals(ShopUiState.CategoryBudget(planned = 30, spent = 0), vm.state.value.budgets[ShopCategory.MANDATORY])

        vm.onAction(ShopAction.BuyClicked(food))
        vm.onAction(ShopAction.ConfirmPurchase)

        val budget = checkNotNull(vm.state.value.budgets[ShopCategory.MANDATORY])
        assertEquals(food.price, budget.spent)
        assertEquals(30 - food.price, budget.left)
        assertEquals(ShopUiState.CategoryBudget(planned = 50, spent = 0), vm.state.value.budgets[ShopCategory.OPTIONAL])
    }

    @Test
    fun missingCoinsCanBeTakenFromSavingsExactly() {
        val meal = content.shopItems.first { it.category == ShopCategory.MANDATORY && it.price > 10 }
        runBlocking {
            game.createPet(SampleGames.newborn)
            game.confirmPlan(BudgetPlan(optional = 10, savings = 90))   // в кошельке 10, в копилке 90
        }
        val vm = ShopViewModel(game, content)

        vm.onAction(ShopAction.BuyClicked(meal))
        vm.onAction(ShopAction.ConfirmPurchase)
        val notEnough = vm.state.value.feedback as PurchaseFeedback.NotEnough
        assertEquals(meal.price - 10, notEnough.missing)
        assertTrue(notEnough.canTakeFromSavings)
        assertEquals(90 - notEnough.missing, notEnough.savingsAfter)
        assertNull(notEnough.goalRemainingAfter)   // цель не выбрана

        vm.onAction(ShopAction.BuyWithSavings)

        assertEquals(PurchaseFeedback.Bought(meal, balanceAfter = 0, fromSavings = meal.price - 10), vm.state.value.feedback)
        val state = game.requireSnapshot().state
        assertEquals(90 - (meal.price - 10), state.savings)   // взято ровно недостающее, а не всё
        assertEquals(listOf(meal), state.purchases)
    }

    @Test
    fun savingsAreNotOfferedWhenTheyDoNotCoverTheGap() {
        val meal = content.shopItems.first { it.category == ShopCategory.MANDATORY && it.price >= 25 }
        val small = GameRepositoryImpl(FakeGameStore(), startBalance = 20, clock = TestClock())
        runBlocking {
            small.createPet(SampleGames.newborn)
            small.confirmPlan(BudgetPlan(optional = 15, savings = 5))   // в кошельке 15, в копилке 5
        }
        val vm = ShopViewModel(small, content)

        vm.onAction(ShopAction.BuyClicked(meal))
        vm.onAction(ShopAction.ConfirmPurchase)

        val notEnough = vm.state.value.feedback as PurchaseFeedback.NotEnough
        assertEquals(meal.price - 15, notEnough.missing)
        assertFalse(notEnough.canTakeFromSavings)
    }
}
