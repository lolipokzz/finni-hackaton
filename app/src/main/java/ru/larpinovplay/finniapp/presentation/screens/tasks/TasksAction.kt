package ru.larpinovplay.finniapp.presentation.screens.tasks

sealed interface TasksAction {
    /** «Пропустить шаг»: Финни переходит к следующей подсказке обучения. */
    data object SkipTutorialStep : TasksAction
}
