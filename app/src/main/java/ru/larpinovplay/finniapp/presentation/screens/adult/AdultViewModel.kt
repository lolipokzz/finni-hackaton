package ru.larpinovplay.finniapp.presentation.screens.adult

import ru.larpinovplay.finniapp.presentation.storage.snackbar
import ru.larpinovplay.finniapp.presentation.storage.orSnackbar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.larpinovplay.finniapp.domain.content.Content
import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.game.model.TopicProgress
import ru.larpinovplay.finniapp.domain.game.repository.GameRepository
import ru.larpinovplay.finniapp.domain.settings.model.AppSettings
import ru.larpinovplay.finniapp.domain.settings.repository.SettingsRepository
import ru.larpinovplay.finniapp.domain.util.result.Result
import kotlin.random.Random

enum class AdultConfirmation { RESET_PROFILE, DELETE_ALL, DEMO }

data class AdultUiState(
    val first: Int = Random.nextInt(10, 50),
    val second: Int = Random.nextInt(10, 50),
    val answer: String = "",
    val attempts: Int = 0,
    val unlocked: Boolean = false,
    val error: String? = null,
    val pending: AdultConfirmation? = null,
    val busy: Boolean = false,
    val snapshot: GameSnapshot? = null,
    val topics: List<TopicProgress> = emptyList(),
    val settings: AppSettings = AppSettings(),
)

class AdultViewModel(
    private val game: GameRepository,
    private val settings: SettingsRepository,
    content: Content,
) : ViewModel() {
    private val _state = MutableStateFlow(AdultUiState())
    val state = _state.asStateFlow()

    private val _events = Channel<AdultEvent>(Channel.BUFFERED)
    val events: Flow<AdultEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(game.snapshot, settings.observeSettings()) { snapshot, prefs -> snapshot to prefs }
                .collect { (snapshot, prefs) ->
                    _state.update { it.copy(snapshot = snapshot, settings = prefs,
                        topics = snapshot?.state?.topicProgress(content.tasks).orEmpty()) }
                }
        }
    }

    fun onAction(action: AdultAction) {
        when (action) {
            is AdultAction.ChangeAnswer -> changeAnswer(action.value)
            AdultAction.Unlock -> unlock()
            is AdultAction.SetSound -> updateSettings { it.copy(soundEnabled = action.enabled) }
            is AdultAction.SetAnimations -> updateSettings { it.copy(animationsEnabled = action.enabled) }
            is AdultAction.SetVoiceRepeat -> updateSettings { it.copy(voiceRepeatEnabled = action.enabled) }
            is AdultAction.Request -> request(action.confirmation)
            AdultAction.DismissConfirmation -> dismissConfirmation()
            AdultAction.Confirm -> confirm()
        }
    }

    private fun changeAnswer(value: String) {
        _state.update { it.copy(answer = value.filter(Char::isDigit).take(2), error = null) }
    }

    private fun unlock() {
        val current = _state.value
        if (current.answer.toIntOrNull() == current.first + current.second) {
            _state.update { it.copy(unlocked = true, answer = "", error = null) }
        } else if (current.attempts >= 2) {
            _state.update { it.copy(first = Random.nextInt(10, 50), second = Random.nextInt(10, 50),
                attempts = 0, answer = "", error = "Попробуйте решить новый пример.") }
        } else {
            _state.update { it.copy(attempts = it.attempts + 1, answer = "", error = "Проверьте сумму и попробуйте ещё раз.") }
        }
    }


    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        if (!_state.value.unlocked || _state.value.busy) return
        viewModelScope.launch { settings.updateSettings(transform).orSnackbar { updateSettings(transform) } }
    }

    private fun request(action: AdultConfirmation) {
        if (_state.value.unlocked && !_state.value.busy) _state.update { it.copy(pending = action, error = null) }
    }

    private fun dismissConfirmation() {
        if (!_state.value.busy) _state.update { it.copy(pending = null) }
    }

    /** Выполняет подтверждённое действие; по успеху экран закрывается событием [AdultEvent.Done]. */
    private fun confirm() {
        val current = _state.value
        val action = current.pending ?: return
        if (!current.unlocked || current.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val result = when (action) {
                AdultConfirmation.RESET_PROFILE -> game.resetProfile()
                AdultConfirmation.DELETE_ALL -> {
                    val settingsResult = settings.updateSettings { AppSettings() }
                    if (settingsResult is Result.Error) settingsResult else game.resetProfile()
                }
                AdultConfirmation.DEMO -> game.resetToDemo()
            }
            when (result) {
                is Result.Success -> {
                    _state.update { it.copy(busy = false, pending = null) }
                    _events.send(AdultEvent.Done)
                }
                is Result.Error -> {
                    result.error.snackbar(retry = null)
                    _state.update { it.copy(busy = false, pending = null, error = "Не удалось выполнить действие. Попробуйте ещё раз.") }
                }
            }
        }
    }
}
