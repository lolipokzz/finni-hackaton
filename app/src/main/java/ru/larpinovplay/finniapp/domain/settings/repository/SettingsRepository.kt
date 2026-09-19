package ru.larpinovplay.finniapp.domain.settings.repository

import kotlinx.coroutines.flow.Flow
import ru.larpinovplay.finniapp.domain.settings.model.AppSettings
import ru.larpinovplay.finniapp.domain.storage.StorageError
import ru.larpinovplay.finniapp.domain.util.result.EmptyResult

interface SettingsRepository {

    /**
     * Текущие настройки; до первого сохранения и при невозможности прочитать файл это значения по умолчанию.
     * Эмитит после каждого изменения.
     */
    fun observeSettings(): Flow<AppSettings>

    /**
     * Применяет [transform] к текущим настройкам и сохраняет результат.
     * При [StorageError.WRITE_FAILED] настройки не меняются.
     */
    suspend fun updateSettings(transform: (AppSettings) -> AppSettings): EmptyResult<StorageError>
}
