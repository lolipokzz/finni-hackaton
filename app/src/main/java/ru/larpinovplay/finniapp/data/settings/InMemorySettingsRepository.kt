package ru.larpinovplay.finniapp.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ru.larpinovplay.finniapp.domain.settings.model.AppSettings
import ru.larpinovplay.finniapp.domain.settings.repository.SettingsRepository

/** Хранит настройки в памяти процесса. Заменится на репозиторий с постоянным хранилищем. */
class InMemorySettingsRepository : SettingsRepository {

    private val _settings = MutableStateFlow(AppSettings())

    override fun observeSettings(): Flow<AppSettings> = _settings.asStateFlow()

    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        _settings.update(transform)
    }
}
