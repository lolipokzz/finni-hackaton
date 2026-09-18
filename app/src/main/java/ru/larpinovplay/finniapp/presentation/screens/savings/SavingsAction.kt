package ru.larpinovplay.finniapp.presentation.screens.savings

import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal

sealed interface SavingsAction {
    data class GoalClicked(val goal: SavingsGoal) : SavingsAction
    data object ConfirmSwitch : SavingsAction
    data object DismissSwitch : SavingsAction
    data class Deposit(val amount: Int) : SavingsAction
    data object ReachGoalClicked : SavingsAction
    data object DismissReached : SavingsAction
}
