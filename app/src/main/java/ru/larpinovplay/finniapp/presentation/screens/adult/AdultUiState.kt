package ru.larpinovplay.finniapp.presentation.screens.adult

import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.game.model.TopicProgress
import ru.larpinovplay.finniapp.domain.settings.model.AppSettings

/** Действия, которые меняют прогресс заметно, и потому идут через окно подтверждения. */
enum class AdultConfirmation { RESET_PROFILE, DELETE_ALL, DEMO }

/** Что пошло не так; слова подбирает экран. */
enum class AdultError {
    /** Ответ на пример неверный, попытки ещё есть. */
    WRONG_ANSWER,

    /** Попытки кончились: пример заменён новым. */
    NEW_EXAMPLE,

    /** Сброс, удаление или демо не выполнились. */
    ACTION_FAILED,
}

/** Пример-барьер: два двузначных числа — просто для взрослого, сложно для 7-летнего (ТЗ 2.5.12). */
data class AdultExample(val first: Int, val second: Int) {
    val sum: Int get() = first + second
}

data class AdultUiState(
    val example: AdultExample,
    val answer: String = "",
    val attempts: Int = 0,
    val unlocked: Boolean = false,
    val error: AdultError? = null,
    val pending: AdultConfirmation? = null,
    val busy: Boolean = false,
    val snapshot: GameSnapshot? = null,
    val topics: List<TopicProgress> = emptyList(),
    val settings: AppSettings = AppSettings(),
)
