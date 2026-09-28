package ru.larpinovplay.finniapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ru.larpinovplay.finniapp.data.settings.InMemorySettingsRepository
import ru.larpinovplay.finniapp.domain.settings.model.AppSettings
import ru.larpinovplay.finniapp.presentation.screens.settings.SettingsAction
import ru.larpinovplay.finniapp.presentation.screens.settings.SettingsViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @Before fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun resetMain() = Dispatchers.resetMain()

    @Test
    fun voiceRepeatIsTurnedOnAndOffInSettingsWithoutTouchingOthers() = runTest {
        val repository = InMemorySettingsRepository()
        val viewModel = SettingsViewModel(repository)
        repository.updateSettings { it.copy(soundEnabled = false) }

        viewModel.onAction(SettingsAction.SetVoiceRepeat(true))
        assertEquals(AppSettings(soundEnabled = false, voiceRepeatEnabled = true), repository.observeSettings().first())
        assertEquals(true, viewModel.state.value.voiceRepeatEnabled)

        viewModel.onAction(SettingsAction.SetVoiceRepeat(false))
        assertEquals(AppSettings(soundEnabled = false), repository.observeSettings().first())
    }
}
