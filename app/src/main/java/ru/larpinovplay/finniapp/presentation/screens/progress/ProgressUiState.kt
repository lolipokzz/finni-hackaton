package ru.larpinovplay.finniapp.presentation.screens.progress

import ru.larpinovplay.finniapp.domain.game.model.LedgerEntry
import ru.larpinovplay.finniapp.domain.game.model.TopicProgress
import ru.larpinovplay.finniapp.domain.game.model.WeekSummary
import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal
import ru.larpinovplay.finniapp.domain.pet.model.Pet

/**
 * Всё, что показывает раздел «Прогресс» (ТЗ 2.5.11).
 * Стадия, очки роста и имя приходят вместе с питомцем, а не копируются в состояние.
 */
data class ProgressUiState(
    val pet: Pet,
    val week: Int,
    val goal: SavingsGoal?,
    val savings: Int,
    val completedGoals: List<SavingsGoal>,
    val taskTopics: List<TopicProgress>,
    val lastWeek: WeekSummary?,
    val weeksCompleted: Int,
    val ledgerThisWeek: List<LedgerEntry>,
)
