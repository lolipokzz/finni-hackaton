package ru.larpinovplay.finniapp.presentation.game

import androidx.annotation.DrawableRes
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.game.model.BudgetDirection

/** Подпись направления бюджета: те же слова, что на вкладках магазина, чтобы план было легко найти в магазине. */
val BudgetDirection.label: String
    get() = when (this) {
        BudgetDirection.MANDATORY -> "Обязательное"
        BudgetDirection.OPTIONAL -> "Необязательное"
        BudgetDirection.SAVINGS -> "Копилка"
    }

@get:DrawableRes
val BudgetDirection.icon: Int
    get() = when (this) {
        BudgetDirection.MANDATORY -> R.drawable.ic_apple
        BudgetDirection.OPTIONAL -> R.drawable.ic_cart
        BudgetDirection.SAVINGS -> R.drawable.ic_pig
    }
