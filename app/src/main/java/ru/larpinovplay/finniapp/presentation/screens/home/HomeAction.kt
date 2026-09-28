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

    /** «Пропустить обучение»: подсказок больше не будет. */
    data object SkipTutorial : HomeAction

    /** «+» или «−» у строки плана. */
    data class ChangePlan(val direction: BudgetDirection, val increase: Boolean) : HomeAction

    /** Ползунок у строки плана: сумма, куда его дотянули. */
    data class SetPlan(val direction: BudgetDirection, val amount: Int) : HomeAction
    data object ConfirmPlan : HomeAction

    /** Кнопка «План» и окно плана: открыть и закрыть, не подтверждая. */
    data object OpenPlan : HomeAction
    data object ClosePlan : HomeAction
}
