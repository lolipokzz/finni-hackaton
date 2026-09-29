package ru.larpinovplay.finniapp.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.larpinovplay.finniapp.domain.content.Content
import ru.larpinovplay.finniapp.domain.game.engine.GameRules
import ru.larpinovplay.finniapp.domain.game.model.BudgetDirection
import ru.larpinovplay.finniapp.domain.game.model.BudgetPlan
import ru.larpinovplay.finniapp.domain.game.model.FinishBlock
import ru.larpinovplay.finniapp.domain.game.model.FinishWeekResult
import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.game.model.GameState
import ru.larpinovplay.finniapp.domain.game.model.LevelStatus
import ru.larpinovplay.finniapp.domain.game.model.PeriodPhase
import ru.larpinovplay.finniapp.domain.game.model.TutorialStep
import ru.larpinovplay.finniapp.domain.game.model.WeekDeeds
import ru.larpinovplay.finniapp.domain.game.model.required
import ru.larpinovplay.finniapp.domain.game.model.tutorialStep
import ru.larpinovplay.finniapp.domain.game.model.weekDeeds
import ru.larpinovplay.finniapp.domain.game.model.weekSatiety
import ru.larpinovplay.finniapp.domain.game.repository.GameRepository
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.settings.model.AppSettings
import ru.larpinovplay.finniapp.domain.settings.repository.SettingsRepository
import ru.larpinovplay.finniapp.domain.shop.cheapestFoodFor
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.presentation.storage.orSnackbar
import kotlin.math.roundToInt

/**
 * Состояние главного экрана: собирает [HomeUiState] из питомца, игры и настроек.
 * Переходы в разделы сюда не попадают: [HomeAction.OpenSection] обрабатывает граф навигации.
 */
class HomeViewModel(
    private val game: GameRepository,
    private val content: Content,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    /** null, пока питомец не загружен. */
    private val _state = MutableStateFlow<HomeUiState?>(null)
    val state: StateFlow<HomeUiState?> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                game.snapshot.filterNotNull(),
                settingsRepository.observeSettings(),
            ) { snapshot, settings -> snapshot to settings }
                .collect { (snapshot, settings) ->
                    _state.update { current -> toUiState(snapshot.pet, snapshot.state, settings, game.finishBlock(), current) }
                }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.FinishWeek -> finishWeek()
            HomeAction.DismissWeekSummary -> dismissWeekSummary()
            is HomeAction.ChangePlan -> changePlan(action.direction, action.increase)
            is HomeAction.SetPlan -> setPlan(action.direction, action.amount)
            HomeAction.OpenPlan -> openPlan()
            HomeAction.ClosePlan -> closePlan()
            HomeAction.ConfirmPlan -> confirmPlan()
            is HomeAction.ShowInfo -> showInfo(action.info)
            HomeAction.DismissInfo -> dismissInfo()
            HomeAction.ShowDeeds -> showDeeds()
            HomeAction.DismissDeeds -> dismissDeeds()
            HomeAction.SkipTutorialStep -> skipTutorialStep()
            HomeAction.FinishTutorial -> finishTutorial()
            is HomeAction.OpenSection -> Unit  // переход — дело навигации
        }
    }

    /**
     * Карточка дел сменяется итогами одним обновлением: между ними не мелькнёт главный экран прошлой недели.
     * Пока итоги открыты, второй тап по ещё не исчезнувшей кнопке не закончит (в демо) и следующую неделю.
     */
    private fun finishWeek() {
        if (_state.value?.weekSummary != null) return
        viewModelScope.launch {
            when (val result = game.finishWeek().orSnackbar { finishWeek() }) {
                is FinishWeekResult.Finished -> _state.update { it?.copy(weekSummary = result.summary, deedsOpen = false) }
                // Карточка дел сама объясняет, чего не хватает
                is FinishWeekResult.Blocked -> _state.update { it?.copy(finishBlock = result.reason, deedsOpen = true) }
                null -> Unit
            }
        }
    }

    private fun dismissWeekSummary() = _state.update { it?.copy(weekSummary = null) }

    private fun openPlan() = _state.update { it?.copy(planOpen = it.planDraft != null || it.activePlan != null) }

    private fun closePlan() = _state.update { it?.copy(planOpen = false) }

    /**
     * Окно закроется само вместе с новой неделей: черновик пропадёт — и окно тоже (см. toUiState).
     * Закрой его раньше — на миг мелькнула бы прошлая подсказка обучения.
     */
    private fun confirmPlan() {
        val draft = _state.value?.planDraft ?: return
        if (draft.unallocated != 0) return
        viewModelScope.launch { game.confirmPlan(draft.plan).orSnackbar { confirmPlan() } }
    }

    private fun showInfo(info: HomeInfo) = _state.update { it?.copy(info = info) }

    private fun dismissInfo() = _state.update { it?.copy(info = null) }

    private fun showDeeds() = _state.update { it?.copy(deedsOpen = true) }

    /** Дела недели — последний шаг обучения: посмотрел — окно «Обучение пройдено», закончится по «Да». */
    private fun dismissDeeds() = _state.update { it?.copy(deedsOpen = false, tutorialDone = it.tutorial == TutorialStep.DEEDS) }

    private fun skipTutorialStep() {
        val step = _state.value?.tutorial
        when {
            step == null || step.required -> Unit   // первый уровень и план не пропустить (см. TutorialStep.required)
            step == TutorialStep.DEEDS -> _state.update { it?.copy(tutorialDone = true) }   // сразу к «Обучение пройдено»
            else -> viewModelScope.launch { game.skipTutorialStep(step).orSnackbar { skipTutorialStep() } }
        }
    }

    /** Окно закроется вместе с концом обучения (см. toUiState): закрой раньше — мелькнула бы подсветка солнышка. */
    private fun finishTutorial() {
        viewModelScope.launch { game.finishTutorial().orSnackbar { finishTutorial() } }
    }

    /** «+» добавляет шаг, но не больше, чем осталось разложить; «−» убирает шаг, но не ниже нуля. */
    private fun changePlan(direction: BudgetDirection, increase: Boolean) = _state.update { state ->
        val draft = state?.planDraft ?: return@update state
        val current = draft.plan[direction]
        val amount = if (increase) {
            current + minOf(GameRules.PLAN_STEP, draft.unallocated)
        } else {
            current - minOf(GameRules.PLAN_STEP, current)
        }
        state.copy(planDraft = draft.copy(plan = draft.plan.with(direction, amount)))
    }

    /**
     * Ползунок шагает по [GameRules.PLAN_STEP] и не заходит дальше, чем осталось разложить, —
     * но до «всё, что осталось» дотягивается, даже если остаток не кратен шагу.
     */
    private fun setPlan(direction: BudgetDirection, amount: Int) = _state.update { state ->
        val draft = state?.planDraft ?: return@update state
        val snapped = (amount.toFloat() / GameRules.PLAN_STEP).roundToInt() * GameRules.PLAN_STEP
        val max = draft.plan[direction] + draft.unallocated
        state.copy(planDraft = draft.copy(plan = draft.plan.with(direction, snapped.coerceIn(0, max))))
    }

    /**
     * Черновик плана для новой недели. Со второй недели начинаем с прошлого плана, урезанного под нынешний
     * бюджет: так проще поправить пару строк, чем раскладывать всё заново.
     */
    private fun newPlanDraft(game: GameState): HomeUiState.PlanDraft {
        val income = game.weekIncome.coerceIn(0, game.balance)
        var left = game.balance
        val last = game.history.lastOrNull()?.plan ?: BudgetPlan()
        val start = BudgetDirection.entries.fold(BudgetPlan()) { plan, direction ->
            val amount = minOf(last[direction], left)
            left -= amount
            plan.with(direction, amount)
        }
        return HomeUiState.PlanDraft(
            week = game.week,
            income = income,
            carried = game.balance - income,
            plan = start,
            // Цена еды на неделю, а не самой дешёвой еды: одной порции овощей на неделю не хватит
            need = cheapestFoodFor(GameRules.WEEKLY_HUNGER, content.shopItems.filter { it.category == ShopCategory.MANDATORY }) ?: 0,
        )
    }

    /** [current] — прежнее состояние: окна и черновик плана принадлежат экрану и переживают обновление данных. */
    private fun toUiState(
        pet: Pet,
        game: GameState,
        settings: AppSettings,
        finishBlock: FinishBlock?,
        current: HomeUiState?,
    ): HomeUiState {
        val tasks = game.availableLevels(content.levels).size
        // Приключение недели открывается после её уровней
        val adventure = game.adventureOfWeek(content.adventures)
            ?.takeIf { game.adventureStatus(it, content.adventures, content.levels) == LevelStatus.AVAILABLE }
        val deeds = GameSnapshot(game, pet).weekDeeds
        val planDraft = if (game.phase == PeriodPhase.PLANNING) {
            current?.planDraft?.takeIf { it.week == game.week && it.budget == game.balance } ?: newPlanDraft(game)
        } else {
            null
        }
        val activePlan = game.plan?.takeIf { game.phase != PeriodPhase.PLANNING }?.let { plan ->
            HomeUiState.ActivePlan(
                week = game.week,
                plan = plan,
                used = BudgetPlan(
                    mandatory = game.spentThisWeek(ShopCategory.MANDATORY),
                    optional = game.spentThisWeek(ShopCategory.OPTIONAL),
                    savings = game.savedThisWeek,
                ),
            )
        }
        return HomeUiState(
            pet = pet,
            demoMode = game.demoMode,
            balance = game.balance,
            savings = game.savings,
            goal = game.goal?.let { HomeUiState.Goal(name = it.name, cost = it.cost) },
            week = game.week,
            speech = if (settings.tipsEnabled) speech(game, pet, finishBlock, tasks, deeds) else null,
            tasksBadge = tasks + if (adventure != null) 1 else 0,
            // Сначала открытое приключение недели — без него неделю не закончить, потом ближайший уровень карты
            activeTask = adventure?.let { HomeUiState.ActiveTask(it.title, it.reward, adventure = true) }
                ?: game.availableLevels(content.levels).firstOrNull()?.let { HomeUiState.ActiveTask(it.title, it.reward, adventure = false) },
            animationsEnabled = settings.animationsEnabled,
            soundEnabled = settings.soundEnabled,
            voiceRepeatEnabled = settings.voiceRepeatEnabled,
            info = current?.info,
            weekSummary = current?.weekSummary,
            finishBlock = finishBlock,
            deeds = deeds,
            tutorial = game.tutorialStep,
            tutorialDone = current?.tutorialDone == true && game.tutorialStep != null,
            weekSatiety = game.weekSatiety,
            deedsOpen = current?.deedsOpen ?: false,
            planDraft = planDraft,
            activePlan = activePlan,
            // Открытое окно остаётся открытым, пока есть что показать; подтверждённый план закрывает окно черновика,
            // а не превращает его в окно идущей недели
            planOpen = current?.planOpen == true && (planDraft != null || (activePlan != null && current.planDraft == null)),
        )
    }

    /**
     * Одно самое важное дело сейчас — от того, что нельзя отложить, к приятному. Пока неделя не началась,
     * Финни зовёт составить план: без него ничего другого не сделать.
     */
    private fun speech(game: GameState, pet: Pet, finishBlock: FinishBlock?, tasks: Int, deeds: WeekDeeds): HomeUiState.Speech? = when {
        game.phase == PeriodPhase.PLANNING -> HomeUiState.Speech.PLAN_WEEK
        // «Мы всё успели» — только когда и правда сделаны все дела, а не просто неделю уже можно закончить
        finishBlock == null && deeds.all -> HomeUiState.Speech.WEEK_READY
        game.currentTrip != null -> HomeUiState.Speech.ON_TRIP   // Финни в поездке: звать в магазин и к заданиям некого
        // «Проголодался» — только когда сытость и правда низкая (30 и меньше), а не просто еда на неделю ещё не куплена
        pet.isHungry -> HomeUiState.Speech.HUNGRY
        finishBlock == FinishBlock.ADVENTURE_NOT_PLAYED -> HomeUiState.Speech.ADVENTURE
        finishBlock == FinishBlock.LEVELS_NOT_PLAYED -> HomeUiState.Speech.NEW_TASK   // уровни недели, за ними — приключение
        game.goal == null -> HomeUiState.Speech.CHOOSE_GOAL
        !deeds.notBored -> HomeUiState.Speech.BORED
        // Неделю можно закончить, но не все дела сделаны: показать, какие ещё можно успеть
        finishBlock == null -> HomeUiState.Speech.DEEDS_LEFT
        tasks > 0 -> HomeUiState.Speech.NEW_TASK
        else -> HomeUiState.Speech.TOMORROW
    }
}
