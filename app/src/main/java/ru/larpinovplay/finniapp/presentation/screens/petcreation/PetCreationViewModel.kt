package ru.larpinovplay.finniapp.presentation.screens.petcreation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.larpinovplay.finniapp.domain.game.repository.GameRepository
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.pet.model.PetLook
import ru.larpinovplay.finniapp.domain.storage.StorageError
import ru.larpinovplay.finniapp.domain.util.result.Result

class PetCreationViewModel(
    private val game: GameRepository
) : ViewModel() {

    private val _state = MutableStateFlow<PetCreationUiState>(PetCreationUiState.Loading)
    val state = _state.asStateFlow()

    init {
        loadPet()
    }

    fun onAction(action: PetCreationAction) {
        when (action) {
            is PetCreationAction.NameChanged -> updateCreation { it.copy(name = action.name) }
            is PetCreationAction.SpeciesSelected -> updateCreation { it.copy(species = action.species) }
            is PetCreationAction.ColorSelected -> updateCreation { it.copy(color = action.color) }
            PetCreationAction.CreatePetClicked -> createPet()
            PetCreationAction.RetryLoadClicked -> loadPet()
        }
    }

    /** Ищет сохранённую игру: есть — сразу главный экран, нет — создание питомца, не открылось — повтор. */
    private fun loadPet() {
        viewModelScope.launch {
            _state.value = PetCreationUiState.Loading
            if (game.snapshot.value != null) {   // ViewModel создан заново, а игра в памяти уже загружена
                _state.value = PetCreationUiState.Loaded
                return@launch
            }
            _state.value = when (val loaded = game.load()) {
                is Result.Success -> if (loaded.data != null) PetCreationUiState.Loaded else PetCreationUiState.Creation()
                is Result.Error -> when (loaded.error) {
                    // Сохранение уже сброшено: игру не вернуть, начинаем заново, но говорим об этом
                    StorageError.CORRUPTED, StorageError.INCOMPATIBLE_VERSION -> PetCreationUiState.Creation(notice = loaded.error)
                    StorageError.READ_FAILED, StorageError.WRITE_FAILED -> PetCreationUiState.LoadFailed(loaded.error)
                }
            }
        }
    }

    private inline fun updateCreation(transform: (PetCreationUiState.Creation) -> PetCreationUiState.Creation) {
        val current = _state.value as? PetCreationUiState.Creation ?: return
        _state.value = transform(current)
    }

    private fun createPet() {
        val creation = _state.value as? PetCreationUiState.Creation ?: return
        val species = creation.species ?: return
        val color = creation.color ?: return
        if (creation.name.isBlank()) return

        viewModelScope.launch {
            _state.value = creation.copy(isCreating = true)
            _state.value = when (val created = game.createPet(Pet.newborn(name = creation.name, look = PetLook(species, color)))) {
                is Result.Success -> PetCreationUiState.Loaded
                is Result.Error -> creation.copy(isCreating = false, notice = created.error)   // игру не записали: остаёмся на форме
            }
        }
    }
}
