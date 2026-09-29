package ru.larpinovplay.finniapp.domain.task.model

/**
 * Уровень на карте заданий (справочник контента): короткие упражнения [tasks] одной темы. Проходится не всё:
 * каждый раз выбирается несколько случайных (GameRules.TASKS_PER_LEVEL).
 * Открывается на игровой неделе [week]; чем дальше неделя, тем сложнее упражнения.
 * Награда — за первое прохождение: [reward] без ошибок, [rewardOnMistake] с ошибками.
 */
data class Level(
    val id: String,
    val topic: TaskTopic,
    val title: String,
    val week: Int,
    val reward: Int,
    val rewardOnMistake: Int,
    val tasks: List<Task>,
)
