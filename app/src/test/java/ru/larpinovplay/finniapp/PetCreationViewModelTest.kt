package ru.larpinovplay.finniapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.larpinovplay.finniapp.data.game.GameRepositoryImpl
import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetSpecies
import ru.larpinovplay.finniapp.domain.storage.StorageError
import ru.larpinovplay.finniapp.presentation.screens.petcreation.PetCreationAction
import ru.larpinovplay.finniapp.presentation.screens.petcreation.PetCreationViewModel
import ru.larpinovplay.finniapp.presentation.screens.petcreation.PetCreationUiState

/**
 * Запуск приложения: что делает экран создания питомца с сохранением. Репозиторий настоящий, хранилище
 * подменено, поэтому проверяется вся цепочка «сохранение → состояние экрана».
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PetCreationViewModelTest {

    private val store = FakeGameStore()

    @Before
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun resetMain() = Dispatchers.resetMain()

    private fun viewModel() = PetCreationViewModel(GameRepositoryImpl(store))

    private fun PetCreationViewModel.createPet(name: String = "Финни") {
        onAction(PetCreationAction.SpeciesSelected(PetSpecies.BUNNY))
        onAction(PetCreationAction.ColorSelected(PetColor.CORAL))
        onAction(PetCreationAction.NameChanged(name))
        onAction(PetCreationAction.CreatePetClicked)
    }

    @Test
    fun savedGameOpensMainScreenWithoutCreation() {
        store.saved = SampleGames.rich()

        assertEquals(PetCreationUiState.Loaded, viewModel().state.value)
    }

    @Test
    fun noSaveStartsCreation() {
        assertEquals(PetCreationUiState.Creation(), viewModel().state.value)
    }

    @Test
    fun petCreatedInOneLaunchIsRestoredInTheNext() {
        viewModel().createPet()

        // Новый экземпляр репозитория и экрана над тем же «диском» — как после перезапуска приложения
        val restarted = viewModel()

        assertEquals(PetCreationUiState.Loaded, restarted.state.value)
        assertEquals(SampleGames.newborn, store.saved?.pet)
    }

    @Test
    fun corruptedSaveStartsCreationWithNotice() {
        store.loadFailure = StorageError.CORRUPTED

        val state = viewModel().state.value

        assertEquals(PetCreationUiState.Creation(notice = StorageError.CORRUPTED), state)
    }

    @Test
    fun incompatibleSaveStartsCreationWithNotice() {
        store.loadFailure = StorageError.INCOMPATIBLE_VERSION

        assertEquals(PetCreationUiState.Creation(notice = StorageError.INCOMPATIBLE_VERSION), viewModel().state.value)
    }

    @Test
    fun unreadableSaveOffersRetryAndDoesNotStartNewGame() {
        store.saved = SampleGames.rich()
        store.loadFailure = StorageError.READ_FAILED
        val viewModel = viewModel()

        assertEquals(PetCreationUiState.LoadFailed(StorageError.READ_FAILED), viewModel.state.value)

        store.loadFailure = null   // диск снова читается
        viewModel.onAction(PetCreationAction.RetryLoadClicked)

        assertEquals(PetCreationUiState.Loaded, viewModel.state.value)
    }

    @Test
    fun failedSaveOfNewPetKeepsFormAndShowsNotice() {
        val viewModel = viewModel()
        store.saveFailure = StorageError.WRITE_FAILED

        viewModel.createPet()

        val state = viewModel.state.value as PetCreationUiState.Creation
        assertEquals(StorageError.WRITE_FAILED, state.notice)
        assertEquals("Финни", state.name)   // введённое не потеряно
        assertTrue(!state.isCreating)
        assertNull(store.saved)
    }

    @Test
    fun creationSucceedsAfterDiskRecovers() {
        val viewModel = viewModel()
        store.saveFailure = StorageError.WRITE_FAILED
        viewModel.createPet()
        store.saveFailure = null

        viewModel.onAction(PetCreationAction.CreatePetClicked)

        assertEquals(PetCreationUiState.Loaded, viewModel.state.value)
    }
}
