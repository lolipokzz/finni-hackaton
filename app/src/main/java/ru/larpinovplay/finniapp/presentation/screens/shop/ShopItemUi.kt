package ru.larpinovplay.finniapp.presentation.screens.shop

import androidx.annotation.DrawableRes
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem
import ru.larpinovplay.finniapp.domain.shop.model.WearableSlot

/** Картинка товара по его id из справочника. */
val ShopItem.icon: Int
    @DrawableRes get() = when (id) {
        "fruits" -> R.drawable.ic_fruits
        "vegetables" -> R.drawable.ic_vegetables
        "meat" -> R.drawable.ic_meat
        "lemonade" -> R.drawable.ic_lemonade
        "chips" -> R.drawable.ic_chips
        "soap" -> R.drawable.ic_soap
        "shampoo" -> R.drawable.ic_shampoo
        "brush" -> R.drawable.ic_brush
        "cap" -> R.drawable.ic_cap
        "glasses" -> R.drawable.ic_glasses
        "bowtie" -> R.drawable.ic_bowtie
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
        // Те же слова, что в плане недели (PRODUCT.md: три слова решений одинаковы везде)
        ShopCategory.MANDATORY -> "Обязательное"
        ShopCategory.OPTIONAL -> "Необязательное"
    }

val WearableSlot.title: String
    get() = when (this) {
        WearableSlot.HEAD -> "На голову"
        WearableSlot.EYES -> "На глаза"
        WearableSlot.NECK -> "На шею"
    }

private fun Int.signed() = if (this > 0) "+$this" else "$this"
