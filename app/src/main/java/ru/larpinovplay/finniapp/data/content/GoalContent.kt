package ru.larpinovplay.finniapp.data.content

import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal

/** Цели копилки. Пока список в коде; по документации (docs/05-content-model.md) переедет в assets/content/goals.json. */
internal val defaultGoals: List<SavingsGoal> = listOf(
    SavingsGoal("room", "Ремонт в комнате", 150, "Новая комната для Финни. Долго, но того стоит"),
    SavingsGoal("bike", "Велосипед", 120, "Финни будет кататься по парку"),
    SavingsGoal("console", "Игровая приставка", 200, "Самая дорогая цель. Нужно терпение"),
    SavingsGoal("house", "Большой домик", 90, "Уютный домик, накопить можно быстро"),
)
