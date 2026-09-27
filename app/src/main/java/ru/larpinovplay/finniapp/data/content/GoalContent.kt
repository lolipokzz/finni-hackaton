package ru.larpinovplay.finniapp.data.content

import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal

/**
 * Цели копилки — лестница от 1,2 до 3 недельных доходов (docs/11-economy.md, раздел 6): первая достигается
 * за 3 недели сбалансированного плана. Пока список в коде; по документации (docs/05-content-model.md) переедет в assets/content/goals.json. */
internal val defaultGoals: List<SavingsGoal> = listOf(
    SavingsGoal("house", "Большой домик", 60, "Уютный домик. Первая мечта: накопить можно за 3 недели"),
    SavingsGoal("bike", "Велосипед", 90, "Финни будет кататься по парку"),
    SavingsGoal("room", "Ремонт в комнате", 120, "Новая комната для Финни. Долго, но того стоит"),
    SavingsGoal("console", "Игровая приставка", 150, "Самая дорогая цель. Нужно терпение"),
)
