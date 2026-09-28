package ru.larpinovplay.finniapp.data.content

import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal

/**
 * Цели копилки — лестница от 1,2 до 2,4 недельных доходов (docs/11-economy.md, раздел 6): первая достигается
 * за 3 недели сбалансированного плана. Каждая цель видна в комнате Финни (RoomBackground). Пока список в коде; по документации (docs/05-content-model.md) переедет в assets/content/goals.json. */
internal val defaultGoals: List<SavingsGoal> = listOf(
    SavingsGoal("bed", "Кроватка", 60, "Мягкая лежанка в комнате Финни. Первая цель: накопить можно за 3 недели"),
    SavingsGoal("bike", "Велосипед", 90, "Появится в комнате. Финни будет кататься по парку"),
    // Поездка: неделю Финни живёт на пляже, потом возвращается с пляжным мячом и ракушкой
    SavingsGoal("sea", "Поездка на море", 110, "Целая неделя на пляже, а домой — с сувенирами", trip = true),
    SavingsGoal("room", "Ремонт в комнате", 120, "Новые обои, пол и мебель в комнате Финни. Долго, но того стоит"),
)
