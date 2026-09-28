package ru.larpinovplay.finniapp.presentation.screens.savings

import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal

sealed interface SavingsAction {
    data class GoalClicked(val goal: SavingsGoal) : SavingsAction
    data object ConfirmSwitch : SavingsAction
    data object DismissSwitch : SavingsAction
    data class Deposit(val amount: Int) : SavingsAction
    data object ReachGoalClicked : SavingsAction
    data object DismissReached : SavingsAction

    // Забрать из копилки: окно с суммой и последствиями
    data object WithdrawClicked : SavingsAction
    data class ChangeWithdraw(val increase: Boolean) : SavingsAction
    data object ConfirmWithdraw : SavingsAction
    data object DismissWithdraw : SavingsAction

    /** «Пропустить обучение»: подсказок больше не будет. */
    data object SkipTutorial : SavingsAction
}
