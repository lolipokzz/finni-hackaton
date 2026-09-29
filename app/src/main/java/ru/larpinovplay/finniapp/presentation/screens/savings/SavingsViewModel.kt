package ru.larpinovplay.finniapp.presentation.screens.savings

import ru.larpinovplay.finniapp.presentation.storage.orSnackbar
import ru.larpinovplay.finniapp.domain.game.model.TutorialStep
import ru.larpinovplay.finniapp.domain.game.model.tutorialStep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.larpinovplay.finniapp.domain.content.Content
import ru.larpinovplay.finniapp.domain.game.engine.GameRules
import ru.larpinovplay.finniapp.domain.game.model.DepositResult
import ru.larpinovplay.finniapp.domain.game.model.GameState
import ru.larpinovplay.finniapp.domain.game.repository.GameRepository
import ru.larpinovplay.finniapp.domain.game.repository.requireSnapshot
import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal
import ru.larpinovplay.finniapp.domain.util.result.dataOrNull

class SavingsViewModel(
    private val game: GameRepository,
    private val content: Content,
) : ViewModel() {

    private val _state = MutableStateFlow(fromGame(game.requireSnapshot().state))
    val state: StateFlow<SavingsUiState> = _state.asStateFlow()

    init {
        // Деньги, копилка и цель приходят из игры; окна и ошибка принадлежат экрану
        viewModelScope.launch {
            game.snapshot.filterNotNull().collect { snapshot ->
                _state.update {
                    val fresh = fromGame(snapshot.state)
                    fresh.copy(
                        switchTo = it.switchTo,
                        reached = it.reached,
                        depositError = it.depositError,
                        depositPicked = it.depositPicked,
                        withdraw = it.withdraw?.let { w -> withdrawDraft(snapshot.state, w.amount) },
                    )
                }
            }
        }
    }

    fun onAction(action: SavingsAction) {
        when (action) {
            is SavingsAction.GoalClicked -> onGoalClicked(action.goal)
            SavingsAction.ConfirmSwitch -> confirmSwitch()
            SavingsAction.DismissSwitch -> dismissSwitch()
            is SavingsAction.ChangeDeposit -> changeDeposit(action.increase)
            SavingsAction.Deposit -> deposit(_state.value.depositAmount)
            SavingsAction.ReachGoalClicked -> reachGoal()
            SavingsAction.DismissReached -> dismissReached()
            SavingsAction.WithdrawClicked -> openWithdraw()
            is SavingsAction.ChangeWithdraw -> changeWithdraw(action.increase)
            SavingsAction.ConfirmWithdraw -> confirmWithdraw()
            SavingsAction.DismissWithdraw -> dismissWithdraw()
            SavingsAction.SkipTutorialStep -> skipTutorialStep()
        }
    }

    private fun dismissSwitch() = _state.update { it.copy(switchTo = null) }

    private fun reachGoal() {
        viewModelScope.launch {
            val reached = game.reachGoal().orSnackbar { reachGoal() } ?: return@launch   // второй тап: цель уже достигнута
            _state.update { it.copy(reached = reached) }
        }
    }

    private fun dismissReached() = _state.update { it.copy(reached = null) }

    private fun openWithdraw() {
        val game = game.requireSnapshot().state
        _state.update { it.copy(withdraw = withdrawDraft(game, minOf(GameRules.PLAN_STEP, game.savings))) }
    }

    private fun dismissWithdraw() = _state.update { it.copy(withdraw = null) }

    private fun skipTutorialStep() {
        viewModelScope.launch { game.skipTutorialStep(TutorialStep.GOAL).orSnackbar { skipTutorialStep() } }
    }

    private fun onGoalClicked(goal: SavingsGoal) {
        val current = game.requireSnapshot().state
        when {
            goal.id == current.goal?.id -> Unit
            // Смена цели при непустой копилке требует подтверждения
            current.goal != null && current.savings > 0 -> _state.update { it.copy(switchTo = goal) }
            else -> viewModelScope.launch { game.chooseGoal(goal).orSnackbar { onGoalClicked(goal) } }
        }
    }

    /** Окно смены цели или снятия закрывается вместе с новым состоянием: старые цифры не мелькнут. */
    private fun confirmSwitch() {
        val goal = _state.value.switchTo ?: return
        viewModelScope.launch {
            game.chooseGoal(goal).orSnackbar { onGoalClicked(goal) }   // повтор снова спросит: копилка не пуста
            _state.update { it.copy(switchTo = null) }
        }
    }

    /** Шаг — [GameRules.PLAN_STEP], сумма — от шага (или всего кошелька, если в нём меньше шага) до всего кошелька. */
    private fun changeDeposit(increase: Boolean) = _state.update {
        val step = if (increase) GameRules.PLAN_STEP else -GameRules.PLAN_STEP
        it.copy(depositPicked = (it.depositAmount + step).coerceIn(it.depositMin, it.depositMax))
    }

    private fun deposit(amount: Int) {
        viewModelScope.launch {
            val rejected = game.deposit(amount).orSnackbar { deposit(amount) } as? DepositResult.Rejected
            _state.update { it.copy(depositError = rejected) }
        }
    }

    /** Окно снятия на [amount] монет; null, если в копилке пусто. Сумма всегда от 1 до всего, что есть. */
    private fun withdrawDraft(game: GameState, amount: Int): SavingsUiState.Withdraw? {
        if (game.savings <= 0) return null
        val clamped = amount.coerceIn(1, game.savings)
        return SavingsUiState.Withdraw(
            amount = clamped,
            savingsBefore = game.savings,
            weeksBefore = game.weeksToGoal(),
            weeksAfter = game.weeksToGoal(game.savings - clamped),
            remainingBefore = game.goalRemaining(),
            remainingAfter = game.goalRemaining(game.savings - clamped),
        )
    }

    private fun changeWithdraw(increase: Boolean) {
        val current = _state.value.withdraw ?: return
        val step = if (increase) GameRules.PLAN_STEP else -GameRules.PLAN_STEP
        val draft = withdrawDraft(game.requireSnapshot().state, current.amount + step)
        _state.update { it.copy(withdraw = draft) }
    }

    private fun confirmWithdraw() {
        val draft = _state.value.withdraw ?: return
        withdraw(draft.amount)
    }

    private fun withdraw(amount: Int) {
        viewModelScope.launch {
            game.withdraw(amount).orSnackbar { withdraw(amount) }
            _state.update { it.copy(withdraw = null) }
        }
    }

    private fun fromGame(game: GameState) = SavingsUiState(
        balance = game.balance,
        savings = game.savings,
        goal = game.goal,
        goals = content.goals,
        weeksToGoal = game.weeksToGoal(),
        goalRemaining = game.goalRemaining(),
        completedGoalIds = game.completedGoals.map { it.id }.toSet(),
        coach = game.tutorialStep == TutorialStep.GOAL,
    )
}
