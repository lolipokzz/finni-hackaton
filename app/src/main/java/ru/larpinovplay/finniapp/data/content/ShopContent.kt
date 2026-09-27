package ru.larpinovplay.finniapp.data.content

import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem
import ru.larpinovplay.finniapp.domain.shop.model.WearableSlot

/** Товары магазина. Пока список в коде; по документации (docs/05-content-model.md) переедет в assets/content/items.json. */
internal val defaultShopItems: List<ShopItem> = listOf(
    ShopItem("fruits", "Фрукты", 15, ShopCategory.MANDATORY, satiety = 30, mood = 5, hint = "Полезно и вкусно. Нужно каждую неделю"),
    ShopItem("vegetables", "Овощи", 10, ShopCategory.MANDATORY, satiety = 25, hint = "Самая недорогая еда, но питомец сыт"),
    ShopItem("meat", "Мясо", 25, ShopCategory.MANDATORY, satiety = 45, mood = 5, hint = "Сытнее всего, но и дороже"),
    ShopItem("lemonade", "Лимонад", 10, ShopCategory.OPTIONAL, mood = 10, hint = "Радует, но можно и без него"),
    ShopItem("chips", "Чипсы", 15, ShopCategory.OPTIONAL, mood = 15, hint = "Приятно, но это не еда на неделю"),
    // Одежда: покупается один раз и остаётся в гардеробе. Дороже недельной еды — на неё стоит накопить
    ShopItem("cap", "Кепка", 40, ShopCategory.OPTIONAL, mood = 10, hint = "Останется навсегда. Надень в гардеробе", slot = WearableSlot.HEAD),
    ShopItem("glasses", "Очки", 35, ShopCategory.OPTIONAL, mood = 10, hint = "Останутся навсегда. Надень в гардеробе", slot = WearableSlot.EYES),
    ShopItem("bowtie", "Бабочка", 30, ShopCategory.OPTIONAL, mood = 10, hint = "Останется навсегда. Надень в гардеробе", slot = WearableSlot.NECK),
    // Вещи для комнаты: покупаются один раз и сразу появляются в комнате. Самые дорогие — копить на них пару недель
    ShopItem("bed", "Кроватка", 50, ShopCategory.OPTIONAL, mood = 15, hint = "Мягкая лежанка появится в комнате навсегда", decor = true),
    ShopItem("bike", "Велосипед", 80, ShopCategory.OPTIONAL, mood = 20, hint = "Появится в комнате навсегда. На него стоит накопить", decor = true),
)
