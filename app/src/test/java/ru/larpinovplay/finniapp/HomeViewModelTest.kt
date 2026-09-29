package ru.larpinovplay.finniapp

import ru.larpinovplay.finniapp.domain.task.model.TaskPayload
import ru.larpinovplay.finniapp.domain.task.model.TaskAnswer
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
import ru.larpinovplay.finniapp.domain.game.model.TutorialStep
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.game.repository.requireSnapshot
import ru.larpinovplay.finniapp.domain.shop.cheapestFoodFor
import ru.larpinovplay.finniapp.domain.pet.model.PetGrowthStage
import ru.larpinovplay.finniapp.domain.pet.model.PetSatiety
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
        // Цена еды на неделю (две порции овощей), а не самой дешёвой порции
        assertEquals(cheapestFoodFor(GameRules.WEEKLY_HUNGER, content.shopItems), draft.need)
        assertEquals(20, draft.need)
        assertEquals(FinishBlock.PLAN_NOT_CONFIRMED, vm.state.value?.finishBlock)
    }

    /** ТЗ 2.5.3: активное задание видно на главном экране. Сначала приключение недели — без него неделю не закончить. */
    @Test
    fun activeTaskIsAdventureOfWeekFirst() {
        val vm = viewModel()

        val adventure = content.adventures.first()
        assertEquals(HomeUiState.ActiveTask(adventure.title, adventure.reward, adventure = true), vm.state.value?.activeTask)
    }

    @Test
    fun afterAdventureActiveTaskIsNextTask() {
        // Приключения засчитывает репозиторий, поэтому ему нужен их список
        val game = GameRepositoryImpl(FakeGameStore(), clock = clock, adventures = content.adventures)
        runBlocking { game.createPet(SampleGames.newborn) }
        val vm = HomeViewModel(game, content, InMemorySettingsRepository())

        runBlocking { game.completeAdventure(content.adventures.first(), mistakes = 0) }

        val task = checkNotNull(game.requireSnapshot().state.availableTasks(content.tasks).firstOrNull())
        assertEquals(HomeUiState.ActiveTask(task.title, task.reward, adventure = false), vm.state.value?.activeTask)
    }

    /** Окно плана само не всплывает: Финни зовёт, ребёнок открывает кнопкой «План», закрыть можно и без плана. */
    @Test
    fun planWindowOpensOnlyByButton() {
        val vm = viewModel()
        assertEquals(false, vm.state.value?.planOpen)
        assertEquals(HomeUiState.Speech.PLAN_WEEK, vm.state.value?.speech)

        vm.onAction(HomeAction.OpenPlan)
        assertEquals(true, vm.state.value?.planOpen)
        vm.onAction(HomeAction.ClosePlan)
        assertEquals(false, vm.state.value?.planOpen)
        assertEquals(PeriodPhase.PLANNING, game.requireSnapshot().state.phase)

        vm.onAction(HomeAction.OpenPlan)
        vm.press(BudgetDirection.OPTIONAL, increase = true, times = 10)
        vm.onAction(HomeAction.ConfirmPlan)
        assertEquals(false, vm.state.value?.planOpen)

        // Неделя идёт — та же кнопка показывает, как идёт план
        runBlocking { game.buy(content.shopItems.first { it.category == ShopCategory.OPTIONAL && it.price <= 50 }) }
        vm.onAction(HomeAction.OpenPlan)
        assertEquals(true, vm.state.value?.planOpen)
        val active = checkNotNull(vm.state.value?.activePlan)
        assertEquals(BudgetPlan(optional = 50), active.plan)
        assertEquals(content.shopItems.first { it.category == ShopCategory.OPTIONAL && it.price <= 50 }.price, active.used.optional)
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

    /** Ползунок шагает по 5 и не заходит дальше, чем осталось разложить. */
    @Test
    fun sliderSnapsToStepAndStopsAtWhatIsLeft() {
        val vm = viewModel()

        vm.onAction(HomeAction.SetPlan(BudgetDirection.OPTIONAL, 23))
        assertEquals(25, vm.draft.plan.optional)

        vm.onAction(HomeAction.SetPlan(BudgetDirection.MANDATORY, 100))
        assertEquals(GameRules.START_BALANCE - 25, vm.draft.plan.mandatory)
        assertEquals(0, vm.draft.unallocated)

        vm.onAction(HomeAction.SetPlan(BudgetDirection.MANDATORY, -3))
        assertEquals(0, vm.draft.plan.mandatory)
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

        // Открывается карточка дел: она и объясняет, почему пока нельзя
        assertEquals(FinishBlock.SAME_DAY, vm.state.value?.finishBlock)
        assertEquals(true, vm.state.value?.deedsOpen)
        assertNull(vm.state.value?.weekSummary)
        vm.onAction(HomeAction.DismissDeeds)
        assertEquals(false, vm.state.value?.deedsOpen)
    }

    /** Обучение новой игры идёт по настоящим действиям: план → цель → еда → дела недели, и заканчивается. */
    @Test
    fun tutorialFollowsRealActionsAndEnds() {
        runBlocking { game.createPet(SampleGames.newborn, withTutorial = true) }
        val vm = HomeViewModel(game, content, InMemorySettingsRepository())
        fun step() = vm.state.value?.tutorial
        assertEquals(TutorialStep.PLAN, step())

        vm.press(BudgetDirection.MANDATORY, increase = true, times = 4)
        vm.press(BudgetDirection.OPTIONAL, increase = true, times = 6)
        vm.onAction(HomeAction.ConfirmPlan)
        assertEquals(TutorialStep.GOAL, step())

        runBlocking { game.chooseGoal(content.goals.first()) }
        assertEquals(TutorialStep.SHOP, step())

        runBlocking { game.buy(content.shopItems.first { it.category == ShopCategory.MANDATORY }) }
        assertEquals(TutorialStep.TASKS, step())

        // Любой ответ засчитывает шаг: важно попробовать, а не угадать
        val task = content.tasks.first { it.payload is TaskPayload.Choice }
        runBlocking { game.answerTask(task, TaskAnswer.Choice("не-тот-ответ")) }
        assertEquals(TutorialStep.DEEDS, step())

        vm.onAction(HomeAction.ShowDeeds)
        vm.onAction(HomeAction.DismissDeeds)
        assertEquals(true, vm.state.value?.tutorialDone)   // «Обучение пройдено. Всё понятно?»
        assertEquals(TutorialStep.DEEDS, step())

        vm.onAction(HomeAction.FinishTutorial)
        assertNull(step())
        assertEquals(false, vm.state.value?.tutorialDone)
    }

    /** Шаги можно пропускать по одному — кроме плана; конец обучения всё равно спрашивает «Всё понятно?». */
    @Test
    fun tutorialStepsCanBeSkippedButNotThePlan() {
        assertNull(viewModel().state.value?.tutorial)   // обычная игра — без обучения

        val fresh = GameRepositoryImpl(FakeGameStore(), clock = clock)
        runBlocking { fresh.createPet(SampleGames.newborn, withTutorial = true) }
        val vm = HomeViewModel(fresh, content, InMemorySettingsRepository())
        fun step() = vm.state.value?.tutorial

        vm.onAction(HomeAction.SkipTutorialStep)
        assertEquals(TutorialStep.PLAN, step())   // план обязателен

        vm.press(BudgetDirection.OPTIONAL, increase = true, times = 10)
        vm.onAction(HomeAction.ConfirmPlan)
        listOf(TutorialStep.GOAL, TutorialStep.SHOP, TutorialStep.TASKS).forEach {
            assertEquals(it, step())
            vm.onAction(HomeAction.SkipTutorialStep)
        }
        assertEquals(TutorialStep.DEEDS, step())
        vm.onAction(HomeAction.SkipTutorialStep)
        assertEquals(true, vm.state.value?.tutorialDone)

        vm.onAction(HomeAction.FinishTutorial)
        assertNull(step())
    }

    @Test
    fun finniAsksForTheMostImportantThing() {
        val vm = viewModel()
        assertEquals(HomeUiState.Speech.PLAN_WEEK, vm.state.value?.speech)   // пока нет плана, Финни зовёт его составить

        vm.press(BudgetDirection.OPTIONAL, increase = true, times = 10)
        vm.onAction(HomeAction.ConfirmPlan)
        // Финни сыт (70), поэтому о еде не просит: следующее важное — выбрать цель
        assertEquals(HomeUiState.Speech.CHOOSE_GOAL, vm.state.value?.speech)
    }

    /** «Мур! Я проголодался» — только при сытости 30 и ниже. */
    @Test
    fun finniSaysHungryOnlyAtThirtyOrLower() {
        fun speechAt(satiety: Int): HomeUiState.Speech? {
            val game = GameRepositoryImpl(FakeGameStore(), clock = clock)
            runBlocking { game.createPet(SampleGames.newborn.copy(satiety = PetSatiety(satiety))) }
            val vm = HomeViewModel(game, content, InMemorySettingsRepository())
            vm.press(BudgetDirection.OPTIONAL, increase = true, times = 10)
            vm.onAction(HomeAction.ConfirmPlan)
            return vm.state.value?.speech
        }
        assertEquals(HomeUiState.Speech.HUNGRY, speechAt(30))
        assertEquals(HomeUiState.Speech.HUNGRY, speechAt(10))
        assertEquals(HomeUiState.Speech.CHOOSE_GOAL, speechAt(31))
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
        assertEquals(20 + GameRules.SAVINGS_BONUS, game.requireSnapshot().state.savings)   // и бонус копилки
        val draft = vm.draft
        assertEquals(2, draft.week)
        assertEquals(GameRules.weekIncome(PetGrowthStage.BABY), draft.income)
        assertEquals(GameRules.START_BALANCE - 20, draft.carried)
        assertEquals(BudgetPlan(10, 20, 20), draft.plan)
        assertEquals(draft.budget - 50, draft.unallocated)

        vm.onAction(HomeAction.DismissWeekSummary)
        assertNull(vm.state.value?.weekSummary)
        assertNotNull(vm.state.value?.planDraft)
        assertEquals(false, vm.state.value?.planOpen)   // план новой недели ждёт кнопки «План»
    }
}
