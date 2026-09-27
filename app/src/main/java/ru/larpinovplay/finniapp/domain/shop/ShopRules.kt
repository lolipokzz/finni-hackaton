package ru.larpinovplay.finniapp.domain.shop

import ru.larpinovplay.finniapp.domain.shop.model.ShopItem

/**
 * Сколько монет минимум нужно, чтобы купить еды на [satiety] сытости из [items] (повторять товар можно).
 * Это цена нужд недели: её подсказывает план, а не цена самой дешёвой еды, которой на неделю не хватит.
 * null — из этих товаров столько сытости не набрать.
 */
fun cheapestFoodFor(satiety: Int, items: List<ShopItem>): Int? {
    val food = items.filter { it.satiety > 0 }
    if (food.isEmpty()) return null
    // cost[s] — дешевле всего набрать не меньше s сытости
    val cost = IntArray(satiety + 1) { Int.MAX_VALUE }
    cost[0] = 0
    for (need in 1..satiety) {
        for (item in food) {
            val rest = cost[maxOf(need - item.satiety, 0)]
            if (rest != Int.MAX_VALUE) cost[need] = minOf(cost[need], rest + item.price)
        }
    }
    return cost[satiety].takeIf { it != Int.MAX_VALUE }
}
