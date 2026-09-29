package ru.larpinovplay.finniapp.domain.goal.model

import ru.larpinovplay.finniapp.domain.game.model.Trip

/**
 * Финансовая цель: дорогая вещь с понятной ценой, на которую ребёнок копит. Справочник контента, только чтение.
 * Картинка — дело UI.
 */
data class SavingsGoal(
    val id: String,
    val name: String,
    val cost: Int,
    val hint: String,
    val trip: Boolean = false,
)
