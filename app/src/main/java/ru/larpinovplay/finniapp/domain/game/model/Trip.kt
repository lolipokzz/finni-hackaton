package ru.larpinovplay.finniapp.domain.game.model

/**
 * Поездка питомца — достигнутая цель-поездка ([SavingsGoal.trip][ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal.trip]).
 * Питомец в поездке всю неделю [week], в которую цель достигнута; с новой неделей он снова дома.
 */
data class Trip(val goalId: String, val week: Int)
