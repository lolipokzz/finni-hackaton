package ru.larpinovplay.finniapp.domain.game.model

/** Как ребёнок прошёл задание: на какой неделе, верно ли, сколько монет получил. */
data class TaskResult(val taskId: String, val week: Int, val success: Boolean, val reward: Int)
