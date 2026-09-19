package ru.larpinovplay.finniapp.presentation.screens.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import ru.larpinovplay.finniapp.domain.content.Content
import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.game.repository.GameRepository

class ProgressViewModel(
    game: GameRepository,
    private val content: Content,
) : ViewModel() {

    /** null, пока питомец не загружен. */
    private val _state = MutableStateFlow<ProgressUiState?>(null)
    val state: StateFlow<ProgressUiState?> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            game.snapshot.filterNotNull().collect { _state.value = toUiState(it) }
        }
    }

    private fun toUiState(snapshot: GameSnapshot): ProgressUiState {
        val game = snapshot.state
        return ProgressUiState(
            pet = snapshot.pet,
            week = game.week,
            goal = game.goal,
            savings = game.savings,
            completedGoals = game.completedGoals,
            taskTopics = game.topicProgress(content.tasks),
            lastWeek = game.history.lastOrNull(),
            weeksCompleted = game.history.size,
            ledgerThisWeek = game.ledger.filter { it.week == game.week },
        )
    }
}
