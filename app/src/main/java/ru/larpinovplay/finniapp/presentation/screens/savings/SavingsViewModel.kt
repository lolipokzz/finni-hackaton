package ru.larpinovplay.finniapp.presentation.screens.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.larpinovplay.finniapp.domain.content.Content
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
                _state.update { fromGame(snapshot.state).copy(switchTo = it.switchTo, reached = it.reached, depositError = it.depositError) }
            }
        }
    }

    fun onAction(action: SavingsAction) {
        when (action) {
            is SavingsAction.GoalClicked -> onGoalClicked(action.goal)
            SavingsAction.ConfirmSwitch -> confirmSwitch()
            SavingsAction.DismissSwitch -> _state.update { it.copy(switchTo = null) }
            is SavingsAction.Deposit -> deposit(action.amount)
            SavingsAction.ReachGoalClicked -> viewModelScope.launch {
                // TODO(хранилище): ошибку сохранения показать пользователю при подключении DataStore
                val reached = game.reachGoal().dataOrNull()
                _state.update { it.copy(reached = reached) }
            }
            SavingsAction.DismissReached -> _state.update { it.copy(reached = null) }
        }
    }

    private fun onGoalClicked(goal: SavingsGoal) {
        val current = game.requireSnapshot().state
        when {
            goal.id == current.goal?.id -> Unit
            // Смена цели при непустой копилке требует подтверждения
            current.goal != null && current.savings > 0 -> _state.update { it.copy(switchTo = goal) }
            else -> viewModelScope.launch { game.chooseGoal(goal) }
        }
    }

    private fun confirmSwitch() {
        val goal = _state.value.switchTo ?: return
        _state.update { it.copy(switchTo = null) }
        viewModelScope.launch { game.chooseGoal(goal) }
    }

    private fun deposit(amount: Int) {
        viewModelScope.launch {
            // TODO(хранилище): ошибку сохранения показать пользователю при подключении DataStore
            val rejected = game.deposit(amount).dataOrNull() as? DepositResult.Rejected
            _state.update { it.copy(depositError = rejected) }
        }
    }

    private fun fromGame(game: GameState) = SavingsUiState(
        balance = game.balance,
        savings = game.savings,
        goal = game.goal,
        goals = content.goals,
        weeksToGoal = game.weeksToGoal(),
        completedGoalIds = game.completedGoals.map { it.id }.toSet(),
    )
}
