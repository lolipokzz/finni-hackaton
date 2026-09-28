package ru.larpinovplay.finniapp.presentation.screens.settings

/** Каждое действие меняет одну настройку: остальные берутся из сохранённых, а не из того, что видел экран. */
sealed interface SettingsAction {
    data class SetSound(val enabled: Boolean) : SettingsAction
    data class SetAnimations(val enabled: Boolean) : SettingsAction
    data class SetTips(val enabled: Boolean) : SettingsAction
}
