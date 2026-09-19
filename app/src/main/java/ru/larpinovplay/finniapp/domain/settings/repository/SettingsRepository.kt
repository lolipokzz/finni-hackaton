package ru.larpinovplay.finniapp.domain.settings.repository

import kotlinx.coroutines.flow.Flow
import ru.larpinovplay.finniapp.domain.settings.model.AppSettings

/**
 * Единый источник настроек: экран настроек их меняет, остальные экраны (например, главный)
 * подписываются и реагируют сразу.
 */
interface SettingsRepository {

    /** Текущие настройки; до первого сохранения — значения по умолчанию. Эмитит после каждого изменения. */
    fun observeSettings(): Flow<AppSettings>

    /** Применяет [transform] к текущим настройкам и сохраняет результат. */
    suspend fun updateSettings(transform: (AppSettings) -> AppSettings)
}
