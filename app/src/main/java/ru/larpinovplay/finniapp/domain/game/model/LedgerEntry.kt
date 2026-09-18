package ru.larpinovplay.finniapp.domain.game.model

/** Запись журнала: баланс не меняется без записи (ТЗ 2.5.4). */
data class LedgerEntry(val week: Int, val reason: LedgerReason, val balanceDelta: Int, val savingsDelta: Int = 0)
