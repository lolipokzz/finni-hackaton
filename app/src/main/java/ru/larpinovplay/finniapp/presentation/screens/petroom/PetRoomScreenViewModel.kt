package ru.larpinovplay.finniapp.presentation.screens.petroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetGrowthStage
import ru.larpinovplay.finniapp.domain.pet.model.PetLook
import ru.larpinovplay.finniapp.domain.pet.model.PetMood
import ru.larpinovplay.finniapp.domain.pet.repository.PetRepository
import ru.larpinovplay.finniapp.domain.pet.model.PetSpecies

class PetRoomScreenViewModel(
    private val repository: PetRepository
) : ViewModel() {

    private val _state = MutableStateFlow<PetRoomState>(PetRoomState.Loading)
    val state = _state.asStateFlow()

    init {
        loadPet()
    }

    fun onAction(action: PetRoomScreenAction) {
        when (action) {
            is PetRoomScreenAction.NameChanged -> updateCreation { it.copy(name = action.name) }
            is PetRoomScreenAction.SpeciesSelected -> updateCreation { it.copy(species = action.species) }
            is PetRoomScreenAction.ColorSelected -> updateCreation { it.copy(color = action.color) }
            PetRoomScreenAction.CreatePetClicked -> createPet()
        }
    }

    private fun loadPet() {
        viewModelScope.launch {
            _state.value = PetRoomState.Loading
            val pet = repository.getPet()
            _state.value = if (pet != null) PetRoomState.Loaded(pet) else PetRoomState.Creation()
        }
    }

    private inline fun updateCreation(transform: (PetRoomState.Creation) -> PetRoomState.Creation) {
        val current = _state.value as? PetRoomState.Creation ?: return
        _state.value = transform(current)
    }

    private fun createPet() {
        val creation = _state.value as? PetRoomState.Creation ?: return
        val species = creation.species ?: return
        val color = creation.color ?: return
        if (creation.name.isBlank()) return

        viewModelScope.launch {
            _state.value = creation.copy(isCreating = true)
            val pet = repository.createPet(
                Pet(
                    name = creation.name,
                    look = PetLook(species, color),
                    mood = PetMood(value = 50),
                    growthStage = PetGrowthStage.BABY,
                    growthProgress = 0
                )
            )
            _state.value = PetRoomState.Loaded(pet)
        }
    }
}

sealed interface PetRoomState {

    data object Loading : PetRoomState

    data class Loaded(
        val pet: Pet
    ) : PetRoomState

    data class Creation(
        val name: String = "",
        val species: PetSpecies? = null,
        val color: PetColor? = null,
        val isCreating: Boolean = false
    ) : PetRoomState
}

sealed interface PetRoomScreenAction {

    data class NameChanged(val name: String) : PetRoomScreenAction

    data class SpeciesSelected(val species: PetSpecies) : PetRoomScreenAction

    data class ColorSelected(val color: PetColor) : PetRoomScreenAction

    data object CreatePetClicked : PetRoomScreenAction
}
