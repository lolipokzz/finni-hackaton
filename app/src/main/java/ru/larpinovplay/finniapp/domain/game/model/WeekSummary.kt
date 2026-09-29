package ru.larpinovplay.finniapp.domain.game.model

import ru.larpinovplay.finniapp.domain.pet.model.PetGrowthStage

/**
 * Итоги закрытой недели: дела недели, план против факта и что стало с питомцем. Объяснение словами строит слой
 * представления.
 */
data class WeekSummary(
    val week: Int,
    val plan: BudgetPlan,
    val deeds: WeekDeeds,              // сколько сделано — столько шагов роста
    val spentMandatory: Int,
    val spentOptional: Int,
    val saved: Int,                    // отложено за вычетом снятого; может быть меньше нуля
    val withdrawn: Int,                // сколько за неделю забрано из копилки
    val savingsBonus: Int,             // бонус копилки за неделю, пришедший в копилку
    val moodDelta: Int,
    val lastingMood: Int = 0,          // сколько настроения за неделю дали одежда и достигнутые цели
    val stageBefore: PetGrowthStage,
    val stageAfter: PetGrowthStage,
    val stepsToNextStage: Int?,        // сколько шагов осталось после этой недели; null — стадия последняя
    val nextIncome: Int,               // карманные деньги, пришедшие на новую неделю
) {
    /** Питомец перешёл на следующую стадию. */
    val grew: Boolean get() = stageAfter != stageBefore

    /** Сколько шагов роста дала неделя. */
    val steps: Int get() = deeds.steps

    /** Сколько по факту ушло в [direction]: потрачено или отложено. */
    fun fact(direction: BudgetDirection): Int = when (direction) {
        BudgetDirection.MANDATORY -> spentMandatory
        BudgetDirection.OPTIONAL -> spentOptional
        BudgetDirection.SAVINGS -> saved
    }

    /** Необязательного куплено больше плана. */
    val optionalOverPlan: Boolean get() = spentOptional > plan.optional

    /** Отложено меньше обещанного: при переводе по плану так бывает, только если что-то забрали. */
    val savingsUnderPlan: Boolean get() = saved < plan.savings
}
