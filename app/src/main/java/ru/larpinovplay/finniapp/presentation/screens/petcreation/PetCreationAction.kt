package ru.larpinovplay.finniapp.presentation.screens.petcreation

import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetSpecies

sealed interface PetCreationAction {

    data class NameChanged(val name: String) : PetCreationAction

    data class SpeciesSelected(val species: PetSpecies) : PetCreationAction

    data class ColorSelected(val color: PetColor) : PetCreationAction

    data object CreatePetClicked : PetCreationAction

    data object RetryLoadClicked : PetCreationAction
}
