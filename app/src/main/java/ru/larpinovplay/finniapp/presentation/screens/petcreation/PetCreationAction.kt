package ru.larpinovplay.finniapp.presentation.screens.petcreation

import ru.larpinovplay.finniapp.domain.pet.model.PetColor

sealed interface PetCreationAction {

    data class NameChanged(val name: String) : PetCreationAction

    data class ColorSelected(val color: PetColor) : PetCreationAction

    /** Следующий шаг знакомства; на последнем — то же, что [CreatePetClicked]. */
    data object NextStep : PetCreationAction

    data object PreviousStep : PetCreationAction

    /** Создать питомца сейчас: в конце знакомства или «Пропустить» на уроках. */
    data object CreatePetClicked : PetCreationAction

    data object RetryLoadClicked : PetCreationAction
}
