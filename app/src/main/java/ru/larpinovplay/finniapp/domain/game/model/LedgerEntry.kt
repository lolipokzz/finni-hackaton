package ru.larpinovplay.finniapp.domain.game.model

/** Запись журнала: баланс не меняется без записи. */
data class LedgerEntry(val week: Int, val reason: LedgerReason, val balanceDelta: Int, val savingsDelta: Int = 0)
