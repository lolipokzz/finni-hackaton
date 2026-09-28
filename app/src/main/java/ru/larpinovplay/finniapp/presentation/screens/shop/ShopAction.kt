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

    /** Не хватает монет: взять недостающее из копилки и купить. */
    data object BuyWithSavings : ShopAction

    /** «Пропустить обучение»: подсказок больше не будет. */
    data object SkipTutorial : ShopAction
}
