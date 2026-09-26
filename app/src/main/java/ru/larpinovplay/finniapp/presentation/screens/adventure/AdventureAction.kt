package ru.larpinovplay.finniapp.presentation.screens.adventure

sealed interface AdventureAction {
    /** Переложить купюру или монету [index] кошелька на прилавок или обратно. */
    data class TogglePiece(val index: Int) : AdventureAction
    data object Pay : AdventureAction

    /** После неверной оплаты или корзины: убрать разбор и поправить. */
    data object Retry : AdventureAction
    data class ToggleBasketItem(val index: Int) : AdventureAction
    data object CheckBasket : AdventureAction
    data class ChooseOption(val optionId: String) : AdventureAction
    data class ChooseChange(val amount: Int) : AdventureAction
    data object Next : AdventureAction
}
