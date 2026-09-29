package ru.larpinovplay.finniapp.domain.game.engine

import ru.larpinovplay.finniapp.domain.pet.model.PetGrowthStage

/**
 * Числа игровой экономики (docs/11-economy.md, перенос в docs/04-rules-and-formulas.md).
 * Всё считается от дохода недели: цены и награды — доли от него. Позже переедут в EconomyConfig контента.
 */
object GameRules {
    const val START_BALANCE = 50
    const val TASKS_PER_WEEK = 2

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

    /** Сколько монет добавляет кнопка «+ монеты» в демо-режиме. */
    const val DEMO_COINS = 50
}
