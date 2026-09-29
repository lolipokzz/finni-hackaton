package ru.larpinovplay.finniapp.presentation.screens.tasks

import ru.larpinovplay.finniapp.domain.task.model.TaskAnswer

sealed interface LevelPlayAction {
    /** «Проверить»: ответ на текущее упражнение. */
    data class Submit(val answer: TaskAnswer) : LevelPlayAction

    /** «Дальше»: следующее упражнение или итог уровня. */
    data object Next : LevelPlayAction

    /** Прошла секунда испытания; экран шлёт её, только пока открыт и виден. */
    data object Tick : LevelPlayAction
}
