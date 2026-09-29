package ru.larpinovplay.finniapp.domain.game.engine

import ru.larpinovplay.finniapp.domain.pet.model.PetGrowthStage
import ru.larpinovplay.finniapp.domain.task.model.Level

/**
 * Числа игровой экономики (docs/11-economy.md, перенос в docs/04-rules-and-formulas.md).
 * Всё считается от дохода недели: цены и награды — доли от него. Позже переедут в EconomyConfig контента.
 */
object GameRules {
    const val START_BALANCE = 50

    /** Звёзды за уровень: без ошибок — [MAX_STARS], одна ошибка — на звезду меньше, больше — одна. Пройти можно всегда. */
    const val MAX_STARS = 3
    fun levelStars(mistakes: Int): Int = when (mistakes) {
        0 -> MAX_STARS
        1 -> MAX_STARS - 1
        else -> 1
    }

    /** Сколько упражнений уровня проходит ребёнок: их выбирают случайно из всех упражнений уровня. */
    const val TASKS_PER_LEVEL = 4
    fun levelTasks(level: Level): Int = minOf(TASKS_PER_LEVEL, level.tasks.size)

    /** Золотое испытание: столько секунд на каждое упражнение уровня. */
    const val CHALLENGE_SECONDS_PER_TASK = 30
    fun challengeSeconds(level: Level): Int = levelTasks(level) * CHALLENGE_SECONDS_PER_TASK

    /** Карманные за неделю по стадии питомца: рост — награда за дела недели, поэтому он меняет и доход. */
    fun weekIncome(stage: PetGrowthStage): Int = when (stage) {
        PetGrowthStage.BABY -> 50
        PetGrowthStage.TEEN -> 60
        PetGrowthStage.ADULT -> 70
    }

    /** На столько падает сытость за неделю. Столько же сытости нужно купить, чтобы Финни был сыт всю неделю. */
    const val WEEKLY_HUNGER = 35

    /** На столько падает настроение за неделю. От дел не зависит: растёт только от радостей. */
    const val WEEKLY_MOOD_DECAY = 15

    /** Вещи из гардероба и достигнутые цели радуют каждую неделю, но всё вместе меньше недельного падения. */
    const val LASTING_MOOD_PER_ITEM = 3
    const val LASTING_MOOD_CAP = 9

    /** Настроение сразу после покупки цели; дальше она радует как вещь навсегда. */
    const val GOAL_MOOD_BONUS = 10

    /** Бонус копилки за неделю, в которую отложено (за вычетом снятого) не меньше [SAVINGS_BONUS_MIN]. */
    const val SAVINGS_BONUS = 5
    const val SAVINGS_BONUS_MIN = 10

    /** Шаг кнопок «+» и «−» в плане недели. */
    const val PLAN_STEP = 5
}
