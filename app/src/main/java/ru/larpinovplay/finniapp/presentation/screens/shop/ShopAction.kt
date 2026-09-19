package ru.larpinovplay.finniapp.presentation.screens.shop

import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem

sealed interface ShopAction {
    data class TabSelected(val category: ShopCategory) : ShopAction
    data class BuyClicked(val item: ShopItem) : ShopAction
    data object ConfirmPurchase : ShopAction
    data object DismissPending : ShopAction
    data object DismissFeedback : ShopAction
    data class PickCheaper(val item: ShopItem) : ShopAction
}
