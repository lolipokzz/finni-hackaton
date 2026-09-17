package ru.larpinovplay.finniapp.presentation.screens.savings

import androidx.annotation.DrawableRes
import ru.larpinovplay.finniapp.R

/**
 * Финансовая цель (ТЗ 2.5.7): дорогая вещь с понятной ценой, на которую ребёнок копит.
 * Пока список в коде; по документации (docs/05-content-model.md) переедет в assets/content/goals.json.
 */
data class SavingsGoal(
    val id: String,
    val name: String,
    val cost: Int,
    @DrawableRes val icon: Int,
    val hint: String,
)

val savingsGoals: List<SavingsGoal> = listOf(
    SavingsGoal("room", "Ремонт в комнате", 150, R.drawable.ic_goal_room, "Новая комната для Финни. Долго, но того стоит"),
    SavingsGoal("bike", "Велосипед", 120, R.drawable.ic_goal_bike, "Финни будет кататься по парку"),
    SavingsGoal("console", "Игровая приставка", 200, R.drawable.ic_goal_console, "Самая дорогая цель. Нужно терпение"),
    SavingsGoal("house", "Большой домик", 90, R.drawable.ic_goal_house, "Уютный домик, накопить можно быстро"),
)
