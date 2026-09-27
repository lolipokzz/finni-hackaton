package ru.larpinovplay.finniapp.presentation.screens.wardrobe

import ru.larpinovplay.finniapp.domain.shop.model.ShopItem

data class WardrobeUiState(
    val petName: String,
    val items: List<WardrobeItem>,
    /** Узлы надетых вещей для 3D-модели. */
    val accessories: Set<String>,
)

data class WardrobeItem(val item: ShopItem, val worn: Boolean)
