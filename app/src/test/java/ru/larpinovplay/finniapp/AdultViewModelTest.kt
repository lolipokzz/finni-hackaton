package ru.larpinovplay.finniapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.random.Random
import ru.larpinovplay.finniapp.data.content.defaultContent
import ru.larpinovplay.finniapp.data.game.GameRepositoryImpl
import ru.larpinovplay.finniapp.data.game.store.InMemoryGameStore
import ru.larpinovplay.finniapp.data.settings.InMemorySettingsRepository
import ru.larpinovplay.finniapp.domain.game.engine.GameRules
import ru.larpinovplay.finniapp.domain.game.model.BudgetPlan
import ru.larpinovplay.finniapp.domain.game.model.FinishWeekResult
import ru.larpinovplay.finniapp.domain.pet.model.*
import ru.larpinovplay.finniapp.domain.util.result.dataOrNull
import ru.larpinovplay.finniapp.domain.settings.model.AppSettings
import ru.larpinovplay.finniapp.presentation.screens.adult.*
import ru.larpinovplay.finniapp.presentation.screens.petcreation.*

@OptIn(ExperimentalCoroutinesApi::class)
class AdultViewModelTest {
    private val game = GameRepositoryImpl(InMemoryGameStore())
    private val settings = InMemorySettingsRepository()
    private val pet = Pet.newborn("Кот", PetLook(PetColor.MINT))
    private fun adult() = AdultViewModel(game, settings, defaultContent())
    private fun unlock(vm: AdultViewModel) {
        vm.onAction(AdultAction.ChangeAnswer((vm.state.value.example.sum).toString()))
        vm.onAction(AdultAction.Unlock)
    }

    @Before fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun resetMain() = Dispatchers.resetMain()

    @Test fun lockedSectionCannotChangeDataOrSettings() = runTest {
        game.createPet(pet)
        val vm = adult()
        vm.onAction(AdultAction.Request(AdultConfirmation.DELETE_ALL))
        vm.onAction(AdultAction.Confirm)
        vm.onAction(AdultAction.SetSound(false))
        assertNull(vm.state.value.pending)
        assertNull(withTimeoutOrNull(1_000) { vm.events.first() })   // закрываться нечему
        assertEquals(pet, game.snapshot.value?.pet)
        assertTrue(settings.observeSettings().first().soundEnabled)
    }

    @Test fun wrongAnswersDoNotUnlockAndThreeAttemptsRefreshChallenge() {
        val vm = adult()
        repeat(3) { vm.onAction(AdultAction.ChangeAnswer("0")); vm.onAction(AdultAction.Unlock) }
        assertFalse(vm.state.value.unlocked)
        assertEquals(0, vm.state.value.attempts)
        assertTrue(vm.state.value.example.sum <= 99)
        unlock(vm)
        assertTrue(vm.state.value.unlocked)
        assertFalse(adult().state.value.unlocked)
    }

    @Test fun cancelledConfirmationDoesNotResetProfile() = runTest {
        game.createPet(pet)
        val vm = adult(); unlock(vm)
        vm.onAction(AdultAction.Request(AdultConfirmation.RESET_PROFILE))
        vm.onAction(AdultAction.DismissConfirmation)
        vm.onAction(AdultAction.Confirm)
        assertEquals(pet, game.snapshot.value?.pet)
        assertNull(withTimeoutOrNull(1_000) { vm.events.first() })
    }

    @Test fun resetPreservesSettingsAndReturnsToCreation() = runTest {
        game.createPet(pet)
        settings.updateSettings { it.copy(soundEnabled = false) }
        val room = PetCreationViewModel(game)
        assertEquals(PetCreationUiState.Loaded, room.state.value)
        val vm = adult(); unlock(vm)
        vm.onAction(AdultAction.Request(AdultConfirmation.RESET_PROFILE))
        vm.onAction(AdultAction.Confirm)
        assertEquals(AdultEvent.Done, vm.events.first())   // раздел закрывается
        assertNull(game.snapshot.value)
        assertTrue(room.state.value is PetCreationUiState.Creation)
        assertFalse(settings.observeSettings().first().soundEnabled)
        game.createPet(pet)
        assertEquals(PetCreationUiState.Loaded, room.state.value)
    }

    @Test fun deleteAlsoRestoresSettings() = runTest {
        game.createPet(pet)
        settings.updateSettings { AppSettings(false, false, false) }
        val vm = adult(); unlock(vm)
        vm.onAction(AdultAction.Request(AdultConfirmation.DELETE_ALL))
        vm.onAction(AdultAction.Confirm)
        assertNull(game.snapshot.value)
        assertEquals(AppSettings(), settings.observeSettings().first())
    }

    @Test fun demoCanBeRepeatedAndStartsWithCleanProgress() = runTest {
        game.createPet(pet)
        val vm = adult(); unlock(vm)
        repeat(2) {
            vm.onAction(AdultAction.Request(AdultConfirmation.DEMO))
            vm.onAction(AdultAction.Confirm)
            val snapshot = checkNotNull(game.snapshot.value)
            assertTrue(snapshot.state.demoMode)
            assertEquals(GameRules.START_BALANCE, snapshot.state.balance)
            assertEquals(1, snapshot.state.week)
            assertEquals(0, snapshot.state.savings)
            assertTrue(snapshot.state.history.isEmpty())
            assertTrue(snapshot.state.levelResults.isEmpty())
            assertEquals(PetGrowthStage.BABY, snapshot.pet.growthStage)
            game.confirmPlan(BudgetPlan(optional = snapshot.state.balance))
            // В демо неделю можно закончить в тот же день
            assertTrue(game.finishWeek().dataOrNull() is FinishWeekResult.Finished)
            assertTrue(checkNotNull(game.snapshot.value).state.demoMode)
        }
    }

    @Test fun wrongAnswersAreExplainedAndThirdOneBringsNewExample() {
        val vm = AdultViewModel(game, settings, defaultContent(), Random(seed = 7))
        vm.onAction(AdultAction.ChangeAnswer("0")); vm.onAction(AdultAction.Unlock)
        assertEquals(AdultError.WRONG_ANSWER, vm.state.value.error)
        repeat(2) { vm.onAction(AdultAction.ChangeAnswer("0")); vm.onAction(AdultAction.Unlock) }
        assertEquals(AdultError.NEW_EXAMPLE, vm.state.value.error)
        assertEquals(0, vm.state.value.attempts)
        vm.onAction(AdultAction.ChangeAnswer(vm.state.value.example.sum.toString())); vm.onAction(AdultAction.Unlock)
        assertTrue(vm.state.value.unlocked)
        assertNull(vm.state.value.error)
    }
}
