package ru.larpinovplay.finniapp.domain.task.model

/**
 * Уровень на карте заданий (справочник контента): короткие упражнения [tasks] одной темы. Проходится не всё: каждый раз
 * выбирается несколько случайных (GameRules.TASKS_PER_LEVEL).
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
