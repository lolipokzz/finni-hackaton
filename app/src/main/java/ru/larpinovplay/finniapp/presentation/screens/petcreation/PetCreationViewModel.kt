package ru.larpinovplay.finniapp.presentation.screens.petcreation

import ru.larpinovplay.finniapp.domain.pet.model.PetColor
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
            is PetCreationAction.NameChanged -> changeName(action.name)
            is PetCreationAction.ColorSelected -> selectColor(action.color)
            PetCreationAction.NextStep -> nextStep()
            PetCreationAction.PreviousStep -> previousStep()
            PetCreationAction.CreatePetClicked -> createPet()
            PetCreationAction.RetryLoadClicked -> loadPet()
        }
    }

    private fun changeName(name: String) = updateCreation { it.copy(name = name) }

    private fun selectColor(color: PetColor) = updateCreation { it.copy(color = color) }

    private fun previousStep() = updateCreation { it.copy(step = CreationStep.entries[(it.step.ordinal - 1).coerceAtLeast(0)]) }

    /**
     * Ищет сохранённую игру, а затем следит за ней: есть игра — главный экран, нет (в том числе после
     * сброса профиля из родительского раздела, см. AdultViewModel) — создание питомца.
     */
    private fun loadPet() {
        viewModelScope.launch {
            _state.value = PetCreationUiState.Loading
            val notice = when (val loaded = game.load()) {
                is Result.Success -> null
                is Result.Error -> when (loaded.error) {
                    // Сохранение уже сброшено: игру не вернуть, начинаем заново, но говорим об этом
                    StorageError.CORRUPTED, StorageError.INCOMPATIBLE_VERSION -> loaded.error
                    StorageError.READ_FAILED, StorageError.WRITE_FAILED, StorageError.NO_SPACE, StorageError.NO_ACCESS -> {
                        _state.value = PetCreationUiState.LoadFailed(loaded.error)
                        return@launch
                    }
                }
            }
            game.snapshot.collect { snapshot ->
                _state.value = when {
                    snapshot != null -> PetCreationUiState.Loaded
                    // Игра уже сброшена и форма создания открыта: не затирать то, что человек успел ввести
                    _state.value is PetCreationUiState.Creation -> _state.value
                    else -> PetCreationUiState.Creation(notice = notice)
                }
            }
        }
    }

    private inline fun updateCreation(transform: (PetCreationUiState.Creation) -> PetCreationUiState.Creation) {
        val current = _state.value as? PetCreationUiState.Creation ?: return
        _state.value = transform(current)
    }

    /** Дальше — только с именем; после последнего урока питомец создаётся. */
    private fun nextStep() {
        val creation = _state.value as? PetCreationUiState.Creation ?: return
        if (creation.step == CreationStep.NAME && creation.name.isBlank()) return
        val next = CreationStep.entries.getOrNull(creation.step.ordinal + 1)
        if (next == null) createPet() else _state.value = creation.copy(step = next)
    }

    private fun createPet() {
        val creation = _state.value as? PetCreationUiState.Creation ?: return
        val color = creation.color
        if (creation.name.isBlank()) return

        viewModelScope.launch {
            _state.value = creation.copy(isCreating = true)
            _state.value = when (val created = game.createPet(Pet.newborn(name = creation.name.trim(), look = PetLook(color)), withTutorial = true)) {
                is Result.Success -> PetCreationUiState.Loaded
                is Result.Error -> creation.copy(isCreating = false, notice = created.error)   // игру не записали: остаёмся на форме
            }
        }
    }
}
