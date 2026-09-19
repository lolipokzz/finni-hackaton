package ru.larpinovplay.finniapp.presentation.screens.adult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.larpinovplay.finniapp.domain.content.Content
import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.game.model.TopicProgress
import ru.larpinovplay.finniapp.domain.game.repository.GameRepository
import ru.larpinovplay.finniapp.domain.settings.model.AppSettings
import ru.larpinovplay.finniapp.domain.settings.repository.SettingsRepository
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

    init {
        viewModelScope.launch {
            combine(game.snapshot, settings.observeSettings()) { snapshot, prefs -> snapshot to prefs }
                .collect { (snapshot, prefs) ->
                    _state.update { it.copy(snapshot = snapshot, settings = prefs,
                        topics = snapshot?.state?.topicProgress(content.tasks).orEmpty()) }
                }
        }
    }

    fun changeAnswer(value: String) {
        _state.update { it.copy(answer = value.filter(Char::isDigit).take(2), error = null) }
    }

    fun unlock() {
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

    fun setSound(enabled: Boolean) = updateSettings { it.copy(soundEnabled = enabled) }
    fun setAnimations(enabled: Boolean) = updateSettings { it.copy(animationsEnabled = enabled) }

    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        if (!_state.value.unlocked || _state.value.busy) return
        viewModelScope.launch { settings.updateSettings(transform) }
    }

    fun request(action: AdultConfirmation) {
        if (_state.value.unlocked && !_state.value.busy) _state.update { it.copy(pending = action, error = null) }
    }

    fun dismissConfirmation() {
        if (!_state.value.busy) _state.update { it.copy(pending = null) }
    }

    fun confirm(onComplete: () -> Unit) {
        val current = _state.value
        val action = current.pending ?: return
        if (!current.unlocked || current.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                when (action) {
                    AdultConfirmation.RESET_PROFILE -> game.resetProfile()
                    AdultConfirmation.DELETE_ALL -> {
                        settings.updateSettings { AppSettings() }
                        game.resetProfile()
                    }
                    AdultConfirmation.DEMO -> game.resetToDemo()
                }
                _state.update { it.copy(busy = false, pending = null) }
                onComplete()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, pending = null, error = "Не удалось выполнить действие. Попробуйте ещё раз.") }
            }
        }
    }
}
