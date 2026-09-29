package ru.larpinovplay.finniapp.presentation.screens.petcreation

import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.storage.StorageError

/**
 * Состояние первого экрана приложения: сначала ищем сохранённую игру, дальше либо создаём питомца, либо открываем игру.
 */
sealed interface PetCreationUiState {

    data object Loading : PetCreationUiState

    /** Питомец уже есть; сам он живёт в репозитории, а не здесь. */
    data object Loaded : PetCreationUiState

    /**
     * Знакомство с Финни: [step] — где ребёнок сейчас. Раскраска сразу видна на 3D-питомце, поэтому она выбрана
     * с самого начала; имя по умолчанию — «Финни», его можно оставить.
     */
    data class Creation(
        val step: CreationStep = CreationStep.HELLO,
        val name: String = DEFAULT_NAME,
        val color: PetColor = PetColor.CORAL,
        val isCreating: Boolean = false,
        /** Почему начинаем заново или не удалось создать: сохранение повреждено, не записалось. */
        val notice: StorageError? = null,
    ) : PetCreationUiState

    /** Сохранённую игру не удалось прочитать; сама она цела, можно повторить. */
    data class LoadFailed(val error: StorageError) : PetCreationUiState
}

/**
 * Шаги знакомства: Финни здоровается, ребёнок выбирает раскраску и имя, потом три коротких урока —
 * карманные и план, дела недели, цель. Уроки можно пропустить.
 */
enum class CreationStep {
    HELLO, COLOR, NAME, PLAN, GROW, DREAM;

    val isLesson: Boolean get() = this == PLAN || this == GROW || this == DREAM
}

const val DEFAULT_NAME = "Финни"
