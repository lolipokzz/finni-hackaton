package ru.larpinovplay.finniapp.presentation.screens.adventure

import ru.larpinovplay.finniapp.domain.adventure.BasketCheck
import ru.larpinovplay.finniapp.domain.adventure.PaymentCheck
import ru.larpinovplay.finniapp.domain.adventure.model.Adventure
import ru.larpinovplay.finniapp.domain.adventure.model.AdventureScene
import ru.larpinovplay.finniapp.domain.game.model.AdventureResult

/** Прохождение приключения: какой шаг открыт, что ребёнок выложил на прилавок и как ответил. */
data class AdventureUiState(
    val adventure: Adventure,
    val sceneIndex: Int = 0,
    val onCounter: List<Int> = emptyList(),   // индексы купюр и монет кошелька, выложенных на прилавок
    val paid: Int? = null,                    // сколько дали в последней оплате: от этого считается сдача
    val inBasket: Set<Int> = emptySet(),      // индексы товаров, выбранных в сцене корзины
    val chosenOptions: Set<String> = emptySet(),   // id вариантов, выбранных за приключение: от них зависит сюжет
    val check: SceneCheck? = null,            // разбор ответа на этом шаге; null — ещё не отвечал
    val mistakes: Int = 0,
    val finish: Finish? = null,               // приключение закончено: показываем итог
) {
    val scene: AdventureScene? get() = adventure.scenes.getOrNull(sceneIndex)

    /** Сколько сейчас на прилавке в сцене оплаты. */
    val counterSum: Int get() = (scene as? AdventureScene.Pay)?.let { pay -> onCounter.sumOf { pay.wallet[it] } } ?: 0

    /** Сколько стоит выбранное в сцене корзины. */
    val basketSum: Int get() = (scene as? AdventureScene.Basket)?.let { basket -> inBasket.sumOf { basket.items[it].price } } ?: 0

    /**
     * Можно идти дальше. Оплату и корзину нужно довести до верного (их можно поправить), а на вопрос и сдачу
     * хватит любого ответа: разбор уже показан, и сюжет идёт дальше.
     */
    val canGoNext: Boolean
        get() = when (scene) {
            is AdventureScene.Story -> true
            is AdventureScene.Pay -> (check as? SceneCheck.Payment)?.result?.correct == true
            is AdventureScene.Basket -> (check as? SceneCheck.Basket)?.result?.correct == true
            is AdventureScene.Change, is AdventureScene.Choice -> check != null
            null -> false
        }

    /** Разбор ответа. */
    sealed interface SceneCheck {
        data class Payment(val result: PaymentCheck) : SceneCheck

        data class Basket(val result: BasketCheck) : SceneCheck

        data class Choice(val option: AdventureScene.Choice.Option) : SceneCheck

        /** [chosen] — что выбрал ребёнок; null — заплатил без сдачи, спрашивать нечего. */
        data class Change(val chosen: Int?, val correct: Int, val paid: Int, val price: Int) : SceneCheck {
            val right: Boolean get() = chosen == null || chosen == correct
        }
    }

    /** Итог; [result] == null — приключение уже было пройдено, награды нет. */
    data class Finish(val result: AdventureResult?)
}
