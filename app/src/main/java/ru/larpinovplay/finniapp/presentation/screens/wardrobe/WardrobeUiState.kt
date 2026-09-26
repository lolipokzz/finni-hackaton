package ru.larpinovplay.finniapp.presentation.screens.wardrobe

import ru.larpinovplay.finniapp.domain.shop.model.ShopItem

data class WardrobeUiState(
    val petName: String,
    /** Можно ли одеть этого питомца: одежда есть не у всех моделей. */
    val supported: Boolean,
    val items: List<WardrobeItem>,
    /** Узлы надетых вещей для 3D-модели. */
    val accessories: Set<String>,
)

data class WardrobeItem(val item: ShopItem, val worn: Boolean)
