package ru.larpinovplay.finniapp.domain.game.model

import ru.larpinovplay.finniapp.domain.task.model.TaskTopic

/** Сколько заданий темы выполнено. */
data class TopicProgress(val topic: TaskTopic, val total: Int, val done: Int)
