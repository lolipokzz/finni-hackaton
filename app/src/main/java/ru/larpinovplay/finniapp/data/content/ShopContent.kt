package ru.larpinovplay.finniapp.data.content

import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem
import ru.larpinovplay.finniapp.domain.shop.model.WearableSlot

/** Товары магазина. Пока список в коде; по документации (docs/05-content-model.md) переедет в assets/content/items.json. */
internal val defaultShopItems: List<ShopItem> = listOf(
    ShopItem("fruits", "Фрукты", 15, ShopCategory.MANDATORY, satiety = 30, mood = 5, hint = "Полезно и вкусно. Нужно каждую неделю"),
    ShopItem("vegetables", "Овощи", 10, ShopCategory.MANDATORY, satiety = 25, hint = "Самая недорогая еда. На неделю нужно две порции"),
    ShopItem("meat", "Мясо", 25, ShopCategory.MANDATORY, satiety = 45, mood = 10, hint = "Любимая еда Финни: дороже, зато радует"),
    ShopItem("lemonade", "Лимонад", 10, ShopCategory.OPTIONAL, mood = 10, hint = "Радует, но можно и без него"),
    ShopItem("chips", "Чипсы", 15, ShopCategory.OPTIONAL, mood = 15, hint = "Приятно, но это не еда на неделю"),
    // Одежда: покупается один раз и остаётся в гардеробе. Сразу радует на полцены и дальше +3 в неделю.
    // Дороже строки «необязательное» за неделю — на неё копят 2–3 недели (docs/11-economy.md, раздел 5)
    ShopItem("cap", "Кепка", 40, ShopCategory.OPTIONAL, mood = 20, hint = "Останется навсегда. Надень в гардеробе", slot = WearableSlot.HEAD),
    ShopItem("glasses", "Очки", 35, ShopCategory.OPTIONAL, mood = 15, hint = "Останутся навсегда. Надень в гардеробе", slot = WearableSlot.EYES),
    ShopItem("bowtie", "Бабочка", 30, ShopCategory.OPTIONAL, mood = 15, hint = "Останется навсегда. Надень в гардеробе", slot = WearableSlot.NECK),
)
