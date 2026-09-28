package ru.larpinovplay.finniapp.presentation.screens.settings

import ru.larpinovplay.finniapp.presentation.storage.orSnackbar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.larpinovplay.finniapp.domain.settings.model.AppSettings
import ru.larpinovplay.finniapp.domain.settings.repository.SettingsRepository

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    private val _state = MutableStateFlow(AppSettings())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeSettings().collect { _state.value = it }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SetSound -> update { it.copy(soundEnabled = action.enabled) }
            is SettingsAction.SetAnimations -> update { it.copy(animationsEnabled = action.enabled) }
            is SettingsAction.SetTips -> update { it.copy(tipsEnabled = action.enabled) }
        }
    }

    /** Меняет одно поле поверх сохранённых настроек: параллельное изменение другого поля не затрётся. */
    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repository.updateSettings(transform).orSnackbar { update(transform) } }
    }
}
