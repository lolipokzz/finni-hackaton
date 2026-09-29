package ru.larpinovplay.finniapp.presentation.screens.adult

sealed interface AdultAction {
    /** Ввод ответа на пример-барьер. */
    data class ChangeAnswer(val value: String) : AdultAction
    data object Unlock : AdultAction

    data class SetSound(val enabled: Boolean) : AdultAction
    data class SetAnimations(val enabled: Boolean) : AdultAction

    /** Демо-режим: добавить монет, чтобы проверить покупки и копилку без ожидания. Прогресс не ломает — без подтверждения. */
    data object AddDemoCoins : AdultAction

    /** Сброс, удаление или демо: сначала окно подтверждения, действие — только по [Confirm]. */
    data class Request(val confirmation: AdultConfirmation) : AdultAction
    data object DismissConfirmation : AdultAction
    data object Confirm : AdultAction
}

/** Разовые события для экрана. */
sealed interface AdultEvent {
    /** Подтверждённое действие выполнено: раздел закрывается. */
    data object Done : AdultEvent
}
