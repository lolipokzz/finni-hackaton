package ru.larpinovplay.finniapp.domain.game.model

import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory

/** Направление бюджета из ТЗ: нужное, желаемое, накопления. */
enum class BudgetDirection { MANDATORY, OPTIONAL, SAVINGS }

/** Строка плана, из которой оплачиваются товары этой категории. */
val ShopCategory.budgetDirection: BudgetDirection
    get() = when (this) {
        ShopCategory.MANDATORY -> BudgetDirection.MANDATORY
        ShopCategory.OPTIONAL -> BudgetDirection.OPTIONAL
    }

/**
 * План недели: сколько монет ребёнок решил отдать на каждое [BudgetDirection]. Это не отдельные кошельки,
 * а намерение: покупки идут из общего баланса, а в итогах план сравнивается с фактом.
 */
data class BudgetPlan(val mandatory: Int = 0, val optional: Int = 0, val savings: Int = 0) {

    val total: Int get() = mandatory + optional + savings

    operator fun get(direction: BudgetDirection): Int = when (direction) {
        BudgetDirection.MANDATORY -> mandatory
        BudgetDirection.OPTIONAL -> optional
        BudgetDirection.SAVINGS -> savings
    }

    fun with(direction: BudgetDirection, amount: Int): BudgetPlan = when (direction) {
        BudgetDirection.MANDATORY -> copy(mandatory = amount)
        BudgetDirection.OPTIONAL -> copy(optional = amount)
        BudgetDirection.SAVINGS -> copy(savings = amount)
    }
}
