package ru.larpinovplay.finniapp.presentation.screens.petcreation

import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetSpecies
import ru.larpinovplay.finniapp.domain.storage.StorageError

/**
 * Состояние первого экрана приложения: сначала ищем сохранённую игру, дальше либо создаём питомца, либо открываем игру.
 */
sealed interface PetCreationUiState {

    data object Loading : PetCreationUiState

    /** Питомец уже есть; сам он живёт в репозитории, а не здесь. */
    data object Loaded : PetCreationUiState

    data class Creation(
        val name: String = "",
        val species: PetSpecies? = null,
        val color: PetColor? = null,
        val isCreating: Boolean = false,
        /** Почему начинаем заново или не удалось создать: сохранение повреждено, не записалось. */
        val notice: StorageError? = null,
    ) : PetCreationUiState

    /** Сохранённую игру не удалось прочитать; сама она цела, можно повторить. */
    data class LoadFailed(val error: StorageError) : PetCreationUiState
}
