package ru.larpinovplay.finniapp.presentation.screens.home

sealed interface HomeAction {
    data class OpenSection(val section: HomeSection) : HomeAction
    data object FinishWeek : HomeAction
    data object PetTapped : HomeAction
    data class ShowInfo(val info: HomeInfo) : HomeAction
    data object DismissInfo : HomeAction
    data object DismissWeekSummary : HomeAction
}
