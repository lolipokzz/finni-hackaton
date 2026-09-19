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
import ru.larpinovplay.finniapp.domain.game.model.GameState
import ru.larpinovplay.finniapp.domain.game.repository.GameRepository
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.settings.model.AppSettings
import ru.larpinovplay.finniapp.domain.settings.repository.SettingsRepository
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
            ) { snapshot, settings -> toUiState(snapshot.pet, snapshot.state, settings) }
                .collect { fresh ->
                    // Окна (info, итог недели) принадлежат экрану и переживают обновление данных
                    _state.update { current -> fresh.copy(info = current?.info, weekSummary = current?.weekSummary) }
                }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.FinishWeek -> viewModelScope.launch {
                // TODO(хранилище): ошибку сохранения показать пользователю при подключении DataStore
                val summary = game.finishWeek().dataOrNull()
                _state.update { it?.copy(weekSummary = summary) }
            }
            HomeAction.DismissWeekSummary -> _state.update { it?.copy(weekSummary = null) }
            is HomeAction.ShowInfo -> _state.update { it?.copy(info = action.info) }
            HomeAction.DismissInfo -> _state.update { it?.copy(info = null) }
            HomeAction.PetTapped -> Unit       // TODO: реакция питомца
            is HomeAction.OpenSection -> Unit  // переход — дело навигации
        }
    }

    private fun toUiState(pet: Pet, game: GameState, settings: AppSettings): HomeUiState {
        val activeTask = game.availableTasks(content.tasks).firstOrNull()
        return HomeUiState(
            pet = pet,
            moodExplanation = when {
                pet.isHungry -> HomeUiState.MoodExplanation.Hungry
                game.history.lastOrNull()?.grew == true -> HomeUiState.MoodExplanation.Grew
                game.purchases.isNotEmpty() -> HomeUiState.MoodExplanation.Purchased(game.purchases.last().name)
                else -> HomeUiState.MoodExplanation.Waiting
            },
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
            suggestedSection = HomeSection.TASKS.takeIf { activeTask != null },
        )
    }
}
