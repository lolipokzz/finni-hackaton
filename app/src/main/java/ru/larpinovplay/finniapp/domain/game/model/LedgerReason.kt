package ru.larpinovplay.finniapp.domain.game.model

/**
 * Почему изменился баланс. Только факт, без текста: как его назвать, решает слой представления.
 * Названия товара, цели и задания — данные контента, а не переводимые фразы.
 */
sealed interface LedgerReason {
    data object StartCoins : LedgerReason
    data class Purchase(val itemName: String) : LedgerReason
    data object Deposit : LedgerReason
    data class GoalReached(val goalName: String) : LedgerReason
    data class TaskReward(val taskTitle: String) : LedgerReason
    data object WeekIncome : LedgerReason
}
