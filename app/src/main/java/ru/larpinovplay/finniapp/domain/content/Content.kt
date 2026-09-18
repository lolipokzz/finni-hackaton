package ru.larpinovplay.finniapp.domain.content

import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem
import ru.larpinovplay.finniapp.domain.task.model.Task

/**
 * Справочники игры: неизменяемый набор, который загружается один раз (docs/05-content-model.md).
 * Игровое состояние ссылается на них, но не владеет.
 */
data class Content(
    val shopItems: List<ShopItem>,
    val goals: List<SavingsGoal>,
    val tasks: List<Task>,
)
