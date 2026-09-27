package ru.larpinovplay.finniapp.domain.game.model

import ru.larpinovplay.finniapp.domain.pet.model.PetGrowthStage

/**
 * Итоги закрытой недели (ТЗ 2.5.9, 2.5.10): план против факта и три звезды роста. Объяснение словами
 * строит слой представления.
 *
 * Звёзды: забота ([foodCovered]), копилка ([savedSomething]), план ([planKept]).
 */
data class WeekSummary(
    val week: Int,
    val plan: BudgetPlan,
    val foodCovered: Boolean,          // звезда «Забота»: питомец накормлен
    val savedSomething: Boolean,       // звезда «Копилка»: отложено больше, чем забрано
    val planKept: Boolean,             // звезда «План»: необязательное не больше плана, в копилке осталось не меньше обещанного
    val spentMandatory: Int,
    val spentOptional: Int,
    val saved: Int,                    // отложено за вычетом снятого; может быть меньше нуля
    val withdrawn: Int,                // сколько за неделю забрано из копилки
    val score: Int,                    // 0..3, по звезде за критерий
    val moodDelta: Int,
    val stageBefore: PetGrowthStage,
    val stageAfter: PetGrowthStage,
    val nextIncome: Int,               // карманные деньги, пришедшие на новую неделю
) {
    /** Питомец перешёл на следующую стадию. */
    val grew: Boolean get() = stageAfter != stageBefore

    /** Сколько по факту ушло в [direction]: потрачено или отложено. */
    fun fact(direction: BudgetDirection): Int = when (direction) {
        BudgetDirection.MANDATORY -> spentMandatory
        BudgetDirection.OPTIONAL -> spentOptional
        BudgetDirection.SAVINGS -> saved
    }

    /** Желаемого куплено больше плана. */
    val optionalOverPlan: Boolean get() = spentOptional > plan.optional

    /** Отложено меньше обещанного: при переводе по плану так бывает, только если что-то забрали. */
    val savingsUnderPlan: Boolean get() = saved < plan.savings

    companion object {
        const val MAX_SCORE = 3
    }
}
