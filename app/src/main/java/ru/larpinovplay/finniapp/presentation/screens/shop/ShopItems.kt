package ru.larpinovplay.finniapp.presentation.screens.shop

import androidx.annotation.DrawableRes
import ru.larpinovplay.finniapp.R

/** Категория расходов, как в ТЗ: обязательное и необязательное. */
enum class ShopCategory(val title: String) {
    MANDATORY("Обязательные"),
    OPTIONAL("Необязательные"),
}

/**
 * Товар магазина. Пока список захардкожен здесь; по документации (docs/05-content-model.md)
 * он переедет в assets/content/items.json, а этот класс станет UI-моделью.
 */
data class ShopItem(
    val id: String,
    val name: String,
    val price: Int,
    val category: ShopCategory,
    @DrawableRes val icon: Int,
    val satiety: Int = 0,         // эффект на сытость
    val mood: Int = 0,            // эффект на настроение
    val hint: String,             // короткое объяснение для ребёнка
) {
    /** Текст эффекта для карточки и подтверждения: «Сытость +30, настроение +5». */
    val effectText: String
        get() = listOfNotNull(
            satiety.takeIf { it != 0 }?.let { "Сытость ${it.signed()}" },
            mood.takeIf { it != 0 }?.let { "Настроение ${it.signed()}" },
        ).joinToString(", ")
}

private fun Int.signed() = if (this > 0) "+$this" else "$this"

val shopItems: List<ShopItem> = listOf(
    ShopItem("fruits", "Фрукты", 15, ShopCategory.MANDATORY, R.drawable.ic_fruits, satiety = 30, mood = 5, hint = "Полезно и вкусно. Нужно каждую неделю"),
    ShopItem("vegetables", "Овощи", 10, ShopCategory.MANDATORY, R.drawable.ic_vegetables, satiety = 25, hint = "Самая недорогая еда, но питомец сыт"),
    ShopItem("meat", "Мясо", 25, ShopCategory.MANDATORY, R.drawable.ic_meat, satiety = 45, mood = 5, hint = "Сытнее всего, но и дороже"),
    ShopItem("lemonade", "Лимонад", 10, ShopCategory.OPTIONAL, R.drawable.ic_lemonade, mood = 10, hint = "Радует, но можно и без него"),
    ShopItem("chips", "Чипсы", 15, ShopCategory.OPTIONAL, R.drawable.ic_chips, mood = 15, hint = "Приятно, но это не еда на неделю"),
)
