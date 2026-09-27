package ru.larpinovplay.finniapp.domain.shop.model

/**
 * Товар магазина (справочник контента, только чтение). Картинка и подписи категорий — дело UI.
 * Вещь с [slot] — одежда: покупается один раз и остаётся в гардеробе навсегда, надевается на это место.
 * Вещь с [decor] — для комнаты (кровать, велосипед): покупается один раз и навсегда появляется в комнате питомца.
 */
data class ShopItem(
    val id: String,
    val name: String,
    val price: Int,
    val category: ShopCategory,
    val satiety: Int = 0,         // эффект на сытость
    val mood: Int = 0,            // эффект на настроение
    val hint: String,             // короткое объяснение для ребёнка
    val slot: WearableSlot? = null,
    val decor: Boolean = false,
) {
    val isWearable: Boolean get() = slot != null

    /** Покупается один раз и остаётся навсегда: одежда и вещи для комнаты. */
    val isPermanent: Boolean get() = isWearable || decor
}
