package ru.larpinovplay.finniapp.presentation.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import ru.larpinovplay.finniapp.domain.content.Content
import ru.larpinovplay.finniapp.domain.game.model.GameState
import ru.larpinovplay.finniapp.domain.game.repository.GameRepository
import ru.larpinovplay.finniapp.domain.game.repository.requireSnapshot

class TasksViewModel(
    game: GameRepository,
    private val content: Content,
) : ViewModel() {

    private val _state = MutableStateFlow(toUiState(game.requireSnapshot().state))
    val state: StateFlow<TasksUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            game.snapshot.filterNotNull().collect { _state.value = toUiState(it.state) }
        }
    }

    private fun toUiState(game: GameState) = TasksUiState(
        items = content.tasks.map { TaskItem(it, game.taskStatus(it)) },
        doneThisWeek = game.tasksDoneThisWeek,
        perWeek = game.tasksPerWeek,
    )
}
