package ru.larpinovplay.finniapp.presentation.screens.tasks

import ru.larpinovplay.finniapp.domain.game.model.TaskStatus
import ru.larpinovplay.finniapp.domain.task.model.Task

data class TasksUiState(
    val items: List<TaskItem>,
    val doneThisWeek: Int,
    val perWeek: Int,
)

/** Задание и его статус на этой неделе. */
data class TaskItem(val task: Task, val status: TaskStatus)
