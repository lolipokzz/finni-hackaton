package ru.larpinovplay.finniapp.domain.game.model

import ru.larpinovplay.finniapp.domain.shop.model.ShopItem

sealed interface PurchaseResult {
    data class Success(val item: ShopItem, val balanceAfter: Int) : PurchaseResult
    data class NotEnough(val missing: Int) : PurchaseResult

    /** Одежда уже в гардеробе или вещь уже в комнате: их покупают один раз. */
    data object AlreadyOwned : PurchaseResult
}

sealed interface DepositResult {
    data object Success : DepositResult

    /** Сумма не больше нуля или больше баланса. */
    data class Rejected(val balance: Int) : DepositResult
}

sealed interface WithdrawResult {
    data object Success : WithdrawResult

    /** Сумма не больше нуля или больше, чем в копилке. */
    data class Rejected(val savings: Int) : WithdrawResult
}

sealed interface ConfirmPlanResult {
    data object Success : ConfirmPlanResult

    /** План уже подтверждён, в нём есть минус или он не делит ровно [budget] монет. */
    data class Rejected(val budget: Int) : ConfirmPlanResult
}

/** Почему неделю пока нельзя закончить. Это не ошибка ребёнка, а подсказка, что сделать сначала. */
enum class FinishBlock {
    /** Сначала нужно составить план. */
    PLAN_NOT_CONFIRMED,

    /** Приключение недели ещё не пройдено. Важно пройти, а не ответить без ошибок. */
    ADVENTURE_NOT_PLAYED,

    /** Неделя началась сегодня; следующая — завтра. В демо-режиме не действует. */
    SAME_DAY,
}

sealed interface FinishWeekResult {
    data class Finished(val summary: WeekSummary) : FinishWeekResult
    data class Blocked(val reason: FinishBlock) : FinishWeekResult
}
