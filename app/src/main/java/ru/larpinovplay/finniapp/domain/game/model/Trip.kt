package ru.larpinovplay.finniapp.domain.game.model

import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal

/**
 * Поездка питомца — достигнутая цель-поездка ([SavingsGoal.trip]).
 * Питомец в поездке всю неделю [week], в которую цель достигнута; с новой неделей он снова дома.
 */
data class Trip(val goalId: String, val week: Int)
