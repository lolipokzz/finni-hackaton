package ru.larpinovplay.finniapp.presentation.screens.home

import ru.larpinovplay.finniapp.domain.game.model.BudgetDirection

sealed interface HomeAction {
    data class OpenSection(val section: HomeSection) : HomeAction
    data object FinishWeek : HomeAction
    data object PetTapped : HomeAction
    data class ShowInfo(val info: HomeInfo) : HomeAction
    data object DismissInfo : HomeAction
    data object DismissWeekSummary : HomeAction
    data object ShowDeeds : HomeAction
    data object DismissDeeds : HomeAction

    /** «+» или «−» у строки плана. */
    data class ChangePlan(val direction: BudgetDirection, val increase: Boolean) : HomeAction
    data object ConfirmPlan : HomeAction
}
