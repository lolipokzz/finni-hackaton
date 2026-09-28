package ru.larpinovplay.finniapp.presentation.screens.savings

import ru.larpinovplay.finniapp.domain.game.model.DepositResult
import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal

/** Данные для экрана копилки; собираются из состояния игры. */
data class SavingsUiState(
    val balance: Int,
    val savings: Int,
    val goal: SavingsGoal?,
    val goals: List<SavingsGoal>,   // из чего выбирать
    val weeksToGoal: Int?,          // null — срок ещё нельзя посчитать
    val completedGoalIds: Set<String>,
    val switchTo: SavingsGoal? = null,      // смена цели ждёт подтверждения
    val reached: SavingsGoal? = null,       // цель только что достигнута: показываем праздник
    val depositError: DepositResult.Rejected? = null,   // последняя попытка отложить не удалась
    val withdraw: Withdraw? = null,         // открыто окно «Забрать из копилки»
    val coach: Boolean = false,             // обучение: Финни просит выбрать мечту
) {
    /**
     * Окно снятия: сколько забрать и что от этого изменится (docs: «экран подтверждения показывает последствия»).
     * [weeksBefore] и [weeksAfter] — срок до цели; null — цели нет или срок пока не посчитать.
     */
    data class Withdraw(
        val amount: Int,
        val savingsBefore: Int,
        val weeksBefore: Int?,
        val weeksAfter: Int?,
    ) {
        val savingsAfter: Int get() = savingsBefore - amount
    }
}
