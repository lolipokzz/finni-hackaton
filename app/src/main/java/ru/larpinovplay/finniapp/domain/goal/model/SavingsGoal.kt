package ru.larpinovplay.finniapp.domain.goal.model

/**
 * Финансовая цель (ТЗ 2.5.7): дорогая вещь с понятной ценой, на которую ребёнок копит.
 * Справочник контента, только чтение. Картинка — дело UI.
 */
data class SavingsGoal(
    val id: String,
    val name: String,
    val cost: Int,
    val hint: String,
)
