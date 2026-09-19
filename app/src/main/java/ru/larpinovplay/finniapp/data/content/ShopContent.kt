package ru.larpinovplay.finniapp.data.content

import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem

/** Товары магазина. Пока список в коде; по документации (docs/05-content-model.md) переедет в assets/content/items.json. */
internal val defaultShopItems: List<ShopItem> = listOf(
    ShopItem("fruits", "Фрукты", 15, ShopCategory.MANDATORY, satiety = 30, mood = 5, hint = "Полезно и вкусно. Нужно каждую неделю"),
    ShopItem("vegetables", "Овощи", 10, ShopCategory.MANDATORY, satiety = 25, hint = "Самая недорогая еда, но питомец сыт"),
    ShopItem("meat", "Мясо", 25, ShopCategory.MANDATORY, satiety = 45, mood = 5, hint = "Сытнее всего, но и дороже"),
    ShopItem("lemonade", "Лимонад", 10, ShopCategory.OPTIONAL, mood = 10, hint = "Радует, но можно и без него"),
    ShopItem("chips", "Чипсы", 15, ShopCategory.OPTIONAL, mood = 15, hint = "Приятно, но это не еда на неделю"),
)
