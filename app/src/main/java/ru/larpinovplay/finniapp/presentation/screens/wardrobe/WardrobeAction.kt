package ru.larpinovplay.finniapp.presentation.screens.wardrobe

import ru.larpinovplay.finniapp.domain.shop.model.ShopItem

sealed interface WardrobeAction {
    /** Надетую вещь снять, ненадетую надеть (прежняя вещь с того же места снимается сама). */
    data class Toggle(val item: ShopItem) : WardrobeAction
}
