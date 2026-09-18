package ru.larpinovplay.finniapp.domain.game.model

import ru.larpinovplay.finniapp.domain.shop.model.ShopItem

sealed interface PurchaseResult {
    data class Success(val item: ShopItem, val balanceAfter: Int) : PurchaseResult
    data class NotEnough(val missing: Int) : PurchaseResult
}

sealed interface DepositResult {
    data object Success : DepositResult

    /** Сумма не больше нуля или больше баланса. */
    data class Rejected(val balance: Int) : DepositResult
}
