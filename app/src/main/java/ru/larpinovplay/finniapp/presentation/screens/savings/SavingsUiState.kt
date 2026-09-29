package ru.larpinovplay.finniapp.presentation.screens.savings

import ru.larpinovplay.finniapp.domain.game.engine.GameRules
import ru.larpinovplay.finniapp.domain.game.model.DepositResult
import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal

/** Данные для экрана копилки; собираются из состояния игры. */
data class SavingsUiState(
    val balance: Int,
    val savings: Int,
    val goal: SavingsGoal?,
    val goals: List<SavingsGoal>,   // из чего выбирать
    val weeksToGoal: Int?,          // null — срок ещё нельзя посчитать
    val goalRemaining: Int?,        // сколько не хватает до цели; null — цели нет
    val completedGoalIds: Set<String>,
    val switchTo: SavingsGoal? = null,      // смена цели ждёт подтверждения
    val reached: SavingsGoal? = null,       // цель только что достигнута: показываем праздник
    val depositError: DepositResult.Rejected? = null,   // последняя попытка отложить не удалась
    val withdraw: Withdraw? = null,         // открыто окно «Забрать из копилки»
    val coach: Boolean = false,             // обучение: Финни просит выбрать цель
    val depositPicked: Int = DEFAULT_DEPOSIT,   // сколько ребёнок выбрал отложить кнопками «−»/«+»
) {
    /** Больше кошелька не отложить. */
    val depositMax: Int get() = balance.coerceAtLeast(0)

    /** Меньше шага не предлагаем, но если в кошельке меньше шага (1–4 монеты), отложить можно и их. */
    val depositMin: Int get() = minOf(GameRules.PLAN_STEP, depositMax)

    /** Сколько отложит кнопка «Отложить»: выбранное, но не больше кошелька (он мог опустеть после выбора). */
    val depositAmount: Int get() = depositPicked.coerceIn(depositMin, depositMax)
    val canDeposit: Boolean get() = depositAmount > 0
    val canDepositLess: Boolean get() = depositAmount > depositMin
    val canDepositMore: Boolean get() = depositAmount < depositMax

    /**
     * Окно снятия: сколько забрать и что от этого изменится (docs: «экран подтверждения показывает последствия»).
     * [weeksBefore] и [weeksAfter] — срок до цели; null — цели нет или срок пока не посчитать.
     */
    data class Withdraw(
        val amount: Int,
        val savingsBefore: Int,
        val weeksBefore: Int?,
        val weeksAfter: Int?,
        val remainingBefore: Int?,   // сколько не хватает до цели сейчас и после снятия; null — цели нет
        val remainingAfter: Int?,
    ) {
        val savingsAfter: Int get() = savingsBefore - amount
    }
}

/** Сколько предлагаем отложить, пока ребёнок не выбрал сам. */
private const val DEFAULT_DEPOSIT = 10
