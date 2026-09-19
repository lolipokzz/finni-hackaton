package ru.larpinovplay.finniapp.presentation.screens.tasks

import ru.larpinovplay.finniapp.domain.task.model.TaskAnswer
import ru.larpinovplay.finniapp.domain.task.model.TaskOutcome

sealed interface TaskPlayAction {
    data class Submit(val answer: TaskAnswer) : TaskPlayAction
}

sealed interface TaskPlayEffect {
    /** Ответ принят. [outcome] == null — задание уже недоступно, показывать нечего. */
    data class Completed(val taskId: String, val outcome: TaskOutcome?) : TaskPlayEffect
}
