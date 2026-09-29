package ru.larpinovplay.finniapp.domain.game.model

import ru.larpinovplay.finniapp.domain.game.engine.GameRules
import ru.larpinovplay.finniapp.domain.pet.model.MoodLevel
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory

/** Четыре дела недели. Каждое сделанное дело — шаг роста Финни. */
data class WeekDeeds(
    val fed: Boolean,              // куплено еды на недельную сытость
    val notBored: Boolean,         // настроение не ниже «спокойного»
    val savingsOnPlan: Boolean,    // отложено больше нуля и не меньше обещанного
    val spendingOnPlan: Boolean,   // необязательного куплено не больше плана
) {
    operator fun get(deed: Deed): Boolean = when (deed) {
        Deed.FED -> fed
        Deed.NOT_BORED -> notBored
        Deed.SAVINGS_ON_PLAN -> savingsOnPlan
        Deed.SPENDING_ON_PLAN -> spendingOnPlan
    }

    /** Сколько дел сделано: столько шагов роста даст неделя. */
    val steps: Int get() = listOf(fed, notBored, savingsOnPlan, spendingOnPlan).count { it }

    val all: Boolean get() = steps == MAX_STEPS

    companion object {
        const val MAX_STEPS = 4
    }
}

/** Дело недели: по нему перебирают дела экраны, чтобы порядок и состав везде совпадали. */
enum class Deed { FED, NOT_BORED, SAVINGS_ON_PLAN, SPENDING_ON_PLAN }

/** Сколько сытости куплено за эту неделю. */
val GameState.weekSatiety: Int get() = purchases.sumOf { it.satiety }

/**
 * Дела недели сейчас. Пока план не подтверждён, дела плана не сделаны. Перерасход на обязательное
 * дела не отнимает: ребёнок не должен бояться накормить питомца.
 */
val GameSnapshot.weekDeeds: WeekDeeds
    get() {
        val plan = state.plan
        val saved = state.savedThisWeek
        return WeekDeeds(
            fed = state.weekSatiety >= GameRules.WEEKLY_HUNGER,
            notBored = pet.mood.level != MoodLevel.BORED,
            savingsOnPlan = plan != null && saved > 0 && saved >= plan.savings,
            spendingOnPlan = plan != null && state.spentThisWeek(ShopCategory.OPTIONAL) <= plan.optional,
        )
    }
