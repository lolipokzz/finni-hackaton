package ru.larpinovplay.finniapp.presentation.screens.shop

import androidx.annotation.DrawableRes
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem

/** Картинка товара по его id из справочника. */
val ShopItem.icon: Int
    @DrawableRes get() = when (id) {
        "fruits" -> R.drawable.ic_fruits
        "vegetables" -> R.drawable.ic_vegetables
        "meat" -> R.drawable.ic_meat
        "lemonade" -> R.drawable.ic_lemonade
        "chips" -> R.drawable.ic_chips
        else -> R.drawable.ic_cart
    }

/** Текст эффекта для карточки и подтверждения: «Сытость +30, настроение +5». */
val ShopItem.effectText: String
    get() = listOfNotNull(
        satiety.takeIf { it != 0 }?.let { "Сытость ${it.signed()}" },
        mood.takeIf { it != 0 }?.let { "Настроение ${it.signed()}" },
    ).joinToString(", ")

val ShopCategory.title: String
    get() = when (this) {
        ShopCategory.MANDATORY -> "Обязательные"
        ShopCategory.OPTIONAL -> "Необязательные"
    }

private fun Int.signed() = if (this > 0) "+$this" else "$this"
