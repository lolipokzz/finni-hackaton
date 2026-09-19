package ru.larpinovplay.finniapp

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.larpinovplay.finniapp.data.settings.InMemorySettingsRepository
import ru.larpinovplay.finniapp.domain.settings.model.AppSettings

class InMemorySettingsRepositoryTest {

    private val repository = InMemorySettingsRepository()

    @Test
    fun startsWithDefaults() = runBlocking {
        assertEquals(AppSettings(), repository.observeSettings().first())
    }

    @Test
    fun updateAppliesTransformAndKeepsOtherFields() = runBlocking {
        repository.updateSettings { it.copy(animationsEnabled = false) }

        val settings = repository.observeSettings().first()

        assertEquals(AppSettings(animationsEnabled = false), settings)
    }
}
