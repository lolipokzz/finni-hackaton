package ru.larpinovplay.finniapp.presentation.screens.shop

import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem

data class ShopUiState(
    val balance: Int,
    val foodCovered: Boolean,
    val tab: ShopCategory,
    val items: List<ShopItem>,                    // товары выбранной вкладки
    val pending: ShopItem? = null,                // ждёт подтверждения
    val feedback: PurchaseFeedback? = null,
)

/** Что показать после подтверждения покупки. */
sealed interface PurchaseFeedback {
    data class Bought(val item: ShopItem, val balanceAfter: Int) : PurchaseFeedback
    data class NotEnough(val item: ShopItem, val missing: Int, val cheaper: List<ShopItem>) : PurchaseFeedback
}
