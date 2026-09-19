package ru.larpinovplay.finniapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import ru.larpinovplay.finniapp.data.content.defaultContent
import ru.larpinovplay.finniapp.data.game.InMemoryGameRepository
import ru.larpinovplay.finniapp.data.settings.InMemorySettingsRepository
import ru.larpinovplay.finniapp.domain.pet.model.*
import ru.larpinovplay.finniapp.domain.settings.model.AppSettings
import ru.larpinovplay.finniapp.presentation.screens.adult.*
import ru.larpinovplay.finniapp.presentation.screens.petroom.*

@OptIn(ExperimentalCoroutinesApi::class)
class AdultViewModelTest {
    private val game = InMemoryGameRepository()
    private val settings = InMemorySettingsRepository()
    private val pet = Pet.newborn("Кот", PetLook(PetSpecies.CAT, PetColor.MINT))
    private fun adult() = AdultViewModel(game, settings, defaultContent())
    private fun unlock(vm: AdultViewModel) {
        vm.changeAnswer((vm.state.value.first + vm.state.value.second).toString())
        vm.unlock()
    }

    @Before fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun resetMain() = Dispatchers.resetMain()

    @Test fun lockedSectionCannotChangeDataOrSettings() = runTest {
        game.createPet(pet)
        val vm = adult()
        vm.request(AdultConfirmation.DELETE_ALL)
        vm.confirm { fail("Locked operation completed") }
        vm.setSound(false)
        assertNull(vm.state.value.pending)
        assertEquals(pet, game.snapshot.value?.pet)
        assertTrue(settings.observeSettings().first().soundEnabled)
    }

    @Test fun wrongAnswersDoNotUnlockAndThreeAttemptsRefreshChallenge() {
        val vm = adult()
        repeat(3) { vm.changeAnswer("0"); vm.unlock() }
        assertFalse(vm.state.value.unlocked)
        assertEquals(0, vm.state.value.attempts)
        assertTrue(vm.state.value.first + vm.state.value.second <= 99)
        unlock(vm)
        assertTrue(vm.state.value.unlocked)
        assertFalse(adult().state.value.unlocked)
    }

    @Test fun cancelledConfirmationDoesNotResetProfile() = runTest {
        game.createPet(pet)
        val vm = adult(); unlock(vm)
        vm.request(AdultConfirmation.RESET_PROFILE)
        vm.dismissConfirmation()
        vm.confirm { fail("Cancelled operation completed") }
        assertEquals(pet, game.snapshot.value?.pet)
    }

    @Test fun resetPreservesSettingsAndReturnsToCreation() = runTest {
        game.createPet(pet)
        settings.updateSettings { it.copy(soundEnabled = false) }
        val room = PetRoomScreenViewModel(game)
        assertEquals(PetRoomState.Loaded, room.state.value)
        val vm = adult(); unlock(vm)
        vm.request(AdultConfirmation.RESET_PROFILE)
        vm.confirm {}
        assertNull(game.snapshot.value)
        assertTrue(room.state.value is PetRoomState.Creation)
        assertFalse(settings.observeSettings().first().soundEnabled)
        game.createPet(pet)
        assertEquals(PetRoomState.Loaded, room.state.value)
    }

    @Test fun deleteAlsoRestoresSettings() = runTest {
        game.createPet(pet)
        settings.updateSettings { AppSettings(false, false, false) }
        val vm = adult(); unlock(vm)
        vm.request(AdultConfirmation.DELETE_ALL)
        vm.confirm {}
        assertNull(game.snapshot.value)
        assertEquals(AppSettings(), settings.observeSettings().first())
    }

    @Test fun demoCanBeRepeatedAndStartsWithCleanProgress() = runTest {
        game.createPet(pet)
        val vm = adult(); unlock(vm)
        repeat(2) {
            vm.request(AdultConfirmation.DEMO)
            vm.confirm {}
            val snapshot = checkNotNull(game.snapshot.value)
            assertTrue(snapshot.state.demoMode)
            assertEquals(100, snapshot.state.balance)
            assertEquals(1, snapshot.state.week)
            assertEquals(0, snapshot.state.savings)
            assertTrue(snapshot.state.history.isEmpty())
            assertTrue(snapshot.state.taskResults.isEmpty())
            assertEquals(PetGrowthStage.BABY, snapshot.pet.growthStage)
            game.finishWeek()
            assertTrue(checkNotNull(game.snapshot.value).state.demoMode)
        }
    }
}
