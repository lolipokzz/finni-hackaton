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
import ru.larpinovplay.finniapp.domain.game.model.GameState
import ru.larpinovplay.finniapp.domain.game.model.PeriodPhase
import ru.larpinovplay.finniapp.domain.game.repository.GameRepository
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.settings.model.AppSettings
import ru.larpinovplay.finniapp.domain.settings.repository.SettingsRepository
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.util.result.dataOrNull

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
            HomeAction.FinishWeek -> viewModelScope.launch {
                // TODO(хранилище): ошибку сохранения показать пользователю при подключении DataStore
                when (val result = game.finishWeek().dataOrNull()) {
                    is FinishWeekResult.Finished -> _state.update { it?.copy(weekSummary = result.summary) }
                    is FinishWeekResult.Blocked -> _state.update { it?.copy(finishNotice = result.reason, finishBlock = result.reason) }
                    null -> Unit
                }
            }
            HomeAction.DismissWeekSummary -> _state.update { it?.copy(weekSummary = null) }
            HomeAction.DismissFinishNotice -> _state.update { it?.copy(finishNotice = null) }
            is HomeAction.ChangePlan -> changePlan(action.direction, action.increase)
            HomeAction.ConfirmPlan -> viewModelScope.launch {
                val draft = _state.value?.planDraft ?: return@launch
                if (draft.unallocated != 0) return@launch
                // Окно закроется само: подтверждённый план переводит неделю в ACTIVE, и черновик пропадёт из состояния
                game.confirmPlan(draft.plan)
            }
            is HomeAction.ShowInfo -> _state.update { it?.copy(info = action.info) }
            HomeAction.DismissInfo -> _state.update { it?.copy(info = null) }
            HomeAction.PetTapped -> Unit       // TODO: реакция питомца
            is HomeAction.OpenSection -> Unit  // переход — дело навигации
        }
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
            need = content.shopItems.filter { it.category == ShopCategory.MANDATORY }.minOfOrNull { it.price } ?: 0,
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
        val activeTask = game.availableTasks(content.tasks).firstOrNull()
        val planDraft = if (game.phase == PeriodPhase.PLANNING) {
            current?.planDraft?.takeIf { it.week == game.week && it.budget == game.balance } ?: newPlanDraft(game)
        } else {
            null
        }
        return HomeUiState(
            pet = pet,
            moodExplanation = when {
                pet.isHungry -> HomeUiState.MoodExplanation.Hungry
                game.history.lastOrNull()?.grew == true -> HomeUiState.MoodExplanation.Grew
                game.purchases.isNotEmpty() -> HomeUiState.MoodExplanation.Purchased(game.purchases.last().name)
                else -> HomeUiState.MoodExplanation.Waiting
            },
            demoMode = game.demoMode,
            balance = game.balance,
            savings = game.savings,
            goal = game.goal?.let { HomeUiState.Goal(name = it.name, cost = it.cost) },
            week = game.week,
            activeTask = activeTask?.let { HomeUiState.ActiveTask(title = it.title, reward = it.reward) },
            tip = when {
                !settings.tipsEnabled -> null
                game.goal == null -> HomeUiState.Tip.ChooseGoal
                else -> HomeUiState.Tip.SaveFor(game.goal.name)
            },
            animationsEnabled = settings.animationsEnabled,
            suggestedSection = HomeSection.TASKS.takeIf { activeTask != null || finishBlock == FinishBlock.ADVENTURE_NOT_PLAYED },
            info = current?.info,
            weekSummary = current?.weekSummary,
            finishBlock = finishBlock,
            finishNotice = current?.finishNotice,
            planDraft = planDraft,
        )
    }
}
