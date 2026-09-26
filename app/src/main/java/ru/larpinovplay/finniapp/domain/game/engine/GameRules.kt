package ru.larpinovplay.finniapp.domain.game.engine

/** Числа игровой экономики (docs/04-rules-and-formulas.md). Позже переедут в EconomyConfig контента. */
object GameRules {
    const val START_BALANCE = 50
    const val WEEK_INCOME = 50
    const val TASKS_PER_WEEK = 2
    const val WEEKLY_HUNGER = 35
    const val GOAL_MOOD_BONUS = 30

    /** Шаг кнопок «+» и «−» в плане недели. */
    const val PLAN_STEP = 5

    /** Настроение за неделю: базовое снижение плюс прибавка по числу звёзд (0..3). */
    const val WEEKLY_MOOD_DECAY = 10
    val MOOD_DELTA_BY_SCORE = listOf(-15, 0, 10, 20)
}
