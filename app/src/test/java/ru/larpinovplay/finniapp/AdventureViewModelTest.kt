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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.larpinovplay.finniapp.data.content.defaultContent
import ru.larpinovplay.finniapp.data.game.GameRepositoryImpl
import ru.larpinovplay.finniapp.domain.adventure.PaymentCheck
import ru.larpinovplay.finniapp.domain.adventure.model.AdventureScene
import ru.larpinovplay.finniapp.domain.game.model.BudgetPlan
import ru.larpinovplay.finniapp.domain.game.model.FinishBlock
import ru.larpinovplay.finniapp.domain.game.repository.requireSnapshot
import ru.larpinovplay.finniapp.presentation.screens.adventure.AdventureAction
import ru.larpinovplay.finniapp.presentation.screens.adventure.AdventureUiState.SceneCheck
import ru.larpinovplay.finniapp.presentation.screens.adventure.AdventureViewModel

/** «Первый поход в магазин» от начала до конца: оплата, сдача, итог и награда в игре. */
@OptIn(ExperimentalCoroutinesApi::class)
class AdventureViewModelTest {

    private val content = defaultContent()
    private val adventure = content.adventures.first()
    private val pay = adventure.scenes.filterIsInstance<AdventureScene.Pay>().first()
    private val clock = TestClock()
    private val game = GameRepositoryImpl(FakeGameStore(), startBalance = 100, clock = clock, adventures = content.adventures)

    @Before
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun resetMain() = Dispatchers.resetMain()

    private fun viewModel(): AdventureViewModel {
        runBlocking {
            game.createPet(SampleGames.newborn)
            game.confirmPlan(BudgetPlan(optional = 100))
        }
        return AdventureViewModel(adventure.id, game, content)
    }

    private val AdventureViewModel.now get() = checkNotNull(state.value)

    /** Выкладывает на прилавок купюры и монеты с такими номиналами. */
    private fun AdventureViewModel.put(vararg values: Int) {
        val free = pay.wallet.indices.toMutableList()
        values.forEach { value ->
            val index = free.first { pay.wallet[it] == value }
            free.remove(index)
            onAction(AdventureAction.TogglePiece(index))
        }
    }

    /** Проходит сюжет до сцены оплаты. */
    private fun AdventureViewModel.toPayScene() {
        while (now.scene !is AdventureScene.Pay) onAction(AdventureAction.Next)
    }

    @Test
    fun perfectRunPaysFullRewardAndUnlocksWeekEnd() {
        val vm = viewModel()
        assertEquals(FinishBlock.ADVENTURE_NOT_PLAYED, game.finishBlock())

        vm.toPayScene()
        vm.put(10, 10)
        vm.onAction(AdventureAction.Pay)
        assertEquals(SceneCheck.Payment(PaymentCheck.WithChange(paid = 20, change = 3)), vm.now.check)
        vm.onAction(AdventureAction.Next)
        vm.onAction(AdventureAction.ChooseChange(3))
        repeat(adventure.scenes.size) { vm.onAction(AdventureAction.Next) }

        val result = checkNotNull(vm.now.finish?.result)
        assertTrue(result.perfect)
        assertEquals(adventure.reward, result.reward)
        assertEquals(100 + adventure.reward, game.requireSnapshot().state.balance)
        assertEquals(FinishBlock.SAME_DAY, game.finishBlock())
    }

    @Test
    fun mistakesAreExplainedAndOnlyLowerTheReward() {
        val vm = viewModel()
        vm.toPayScene()

        vm.put(10, 5)
        vm.onAction(AdventureAction.Pay)
        assertEquals(SceneCheck.Payment(PaymentCheck.NotEnough(missing = 2)), vm.now.check)
        assertFalse(vm.now.canGoNext)

        vm.onAction(AdventureAction.Retry)
        vm.put(2)
        vm.onAction(AdventureAction.Pay)
        vm.onAction(AdventureAction.Next)
        // Заплатил ровно: про сдачу не спрашиваем
        assertEquals(SceneCheck.Change(chosen = null, correct = 0, paid = 17, price = 17), vm.now.check)
        repeat(adventure.scenes.size) { vm.onAction(AdventureAction.Next) }

        val result = checkNotNull(vm.now.finish?.result)
        assertFalse(result.perfect)
        assertEquals(adventure.rewardOnMistake, result.reward)
    }

    @Test
    fun wrongChangeIsExplainedAndStoryGoesOn() {
        val vm = viewModel()
        vm.toPayScene()
        vm.put(50)
        vm.onAction(AdventureAction.Pay)
        vm.onAction(AdventureAction.Next)

        vm.onAction(AdventureAction.ChooseChange(43))

        val check = vm.now.check as SceneCheck.Change
        assertFalse(check.right)
        assertEquals(33, check.correct)
        assertTrue(vm.now.canGoNext)
        assertEquals(1, vm.now.mistakes)
    }

    @Test
    fun secondRunGivesNoReward() {
        viewModel().apply {
            toPayScene(); put(10, 5, 2); onAction(AdventureAction.Pay)
            repeat(adventure.scenes.size + 1) { onAction(AdventureAction.Next) }
        }
        val balance = game.requireSnapshot().state.balance

        val again = AdventureViewModel(adventure.id, game, content)
        again.toPayScene(); again.put(10, 5, 2); again.onAction(AdventureAction.Pay)
        repeat(adventure.scenes.size + 1) { again.onAction(AdventureAction.Next) }

        assertNull(again.now.finish?.result)
        assertEquals(balance, game.requireSnapshot().state.balance)
    }

    /** Приключение [id] из контента, открытое на экране; награда тут не проверяется — только шаги. */
    private fun open(id: String): AdventureViewModel {
        runBlocking { game.createPet(SampleGames.newborn) }
        return AdventureViewModel(id, game, content)
    }

    @Test
    fun wrongAnswerIsExplainedAndBasketCanBeFixed() {
        val vm = open("park_walk")
        vm.onAction(AdventureAction.Next)

        vm.onAction(AdventureAction.ChooseOption("park_enough_yes"))
        val answer = vm.now.check as SceneCheck.Choice
        assertFalse(answer.option.correct)
        assertTrue(vm.now.canGoNext)   // на вопрос хватает любого ответа
        vm.onAction(AdventureAction.Next)

        val basket = vm.now.scene as AdventureScene.Basket
        basket.items.indices.forEach { vm.onAction(AdventureAction.ToggleBasketItem(it)) }   // всё сразу — не влезает
        vm.onAction(AdventureAction.CheckBasket)
        assertFalse(vm.now.canGoNext)
        vm.onAction(AdventureAction.Retry)
        vm.onAction(AdventureAction.ToggleBasketItem(basket.items.indexOfFirst { it.name == "Мороженое" }))
        vm.onAction(AdventureAction.ToggleBasketItem(basket.items.indexOfFirst { it.name == "Сахарная вата" }))
        vm.onAction(AdventureAction.CheckBasket)

        assertTrue(vm.now.canGoNext)
        assertEquals(2, vm.now.mistakes)
    }

    @Test
    fun storyContinuesTheChildsDecision() {
        val vm = open("scratched_paw")
        vm.onAction(AdventureAction.Next)
        vm.onAction(AdventureAction.ChooseOption("paw_keep_some"))
        vm.onAction(AdventureAction.Next)
        vm.onAction(AdventureAction.Next)

        val story = vm.now.scene as AdventureScene.Story
        assertEquals(story.variants.getValue("paw_keep_some"), story.textAfter(vm.now.chosenOptions))
    }
}
