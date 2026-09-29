package ru.larpinovplay.finniapp.presentation.screens.shop

import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem

data class ShopUiState(
    val balance: Int,
    val weekSatiety: Int,                         // сколько сытости куплено за неделю: дело «Финни сыт»
    val tab: ShopCategory,
    val items: List<ShopItem>,                    // товары выбранной вкладки
    val owned: Set<String> = emptySet(),          // id одежды, которая уже в гардеробе
    val budgets: Map<ShopCategory, CategoryBudget> = emptyMap(),   // план недели по категориям; пусто — плана нет
    val pending: ShopItem? = null,                // ждёт подтверждения
    val feedback: PurchaseFeedback? = null,
    val coach: Boolean = false,                   // обучение: Финни просит купить еду
) {
    /** Сколько на категорию запланировано и сколько уже потрачено за неделю. */
    data class CategoryBudget(val planned: Int, val spent: Int) {
        /** Меньше нуля — потрачено сверх плана. */
        val left: Int get() = planned - spent
    }
}

/** Что показать после подтверждения покупки. */
sealed interface PurchaseFeedback {
    /** [fromSavings] — сколько для покупки взято из копилки; 0 — только из кошелька. */
    data class Bought(val item: ShopItem, val balanceAfter: Int, val fromSavings: Int = 0) : PurchaseFeedback

    /**
     * Не хватает [missing]. Если в копилке хватает на недостающее, можно взять ровно его ([savings] — сколько там
     * сейчас, [goal] — на что копим, чтобы показать, насколько отодвинется цель).
     */
    data class NotEnough(
        val item: ShopItem,
        val missing: Int,
        val cheaper: List<ShopItem>,
        val savings: Int = 0,
        val goal: SavingsGoal? = null,
        val savingsAfter: Int = savings - missing,   // сколько останется в копилке, если взять недостающее
        val goalRemainingAfter: Int? = null,         // сколько тогда не хватит до цели; null — цели нет
        val weeksBefore: Int? = null,                // срок до цели сейчас, нед.; null — не считается
        val weeksAfter: Int? = null,                 // срок до цели, если взять недостающее (ТЗ 2.5.7)
    ) : PurchaseFeedback {
        val canTakeFromSavings: Boolean get() = savings >= missing
    }
}
