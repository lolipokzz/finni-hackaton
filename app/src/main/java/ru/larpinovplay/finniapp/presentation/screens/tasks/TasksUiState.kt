package ru.larpinovplay.finniapp.presentation.screens.tasks

import ru.larpinovplay.finniapp.domain.adventure.model.Adventure
import ru.larpinovplay.finniapp.domain.game.model.TaskStatus
import ru.larpinovplay.finniapp.domain.task.model.Task

data class TasksUiState(
    val items: List<TaskItem>,
    val doneThisWeek: Int,
    val perWeek: Int,
    val adventure: Adventure? = null,        // приключение, которое ждёт на этой неделе
    val adventureDone: Boolean = false,      // приключение этой недели уже пройдено
)

/** Задание и его статус на этой неделе. */
data class TaskItem(val task: Task, val status: TaskStatus)
