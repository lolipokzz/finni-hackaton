package ru.larpinovplay.finniapp.domain.shop.model

/** Товар магазина (справочник контента, только чтение). Картинка и подписи категорий — дело UI. */
data class ShopItem(
    val id: String,
    val name: String,
    val price: Int,
    val category: ShopCategory,
    val satiety: Int = 0,         // эффект на сытость
    val mood: Int = 0,            // эффект на настроение
    val hint: String,             // короткое объяснение для ребёнка
)
