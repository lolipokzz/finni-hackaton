package ru.larpinovplay.finniapp.domain.adventure

import ru.larpinovplay.finniapp.domain.adventure.model.AdventureScene

/** Как ребёнок заплатил в сцене оплаты. */
sealed interface PaymentCheck {
    /** Ровно без сдачи. */
    data object Exact : PaymentCheck

    /** Хватает, лишнего нет; продавец вернёт [change]. */
    data class WithChange(val paid: Int, val change: Int) : PaymentCheck

    /** Не хватает [missing]. */
    data class NotEnough(val missing: Int) : PaymentCheck

    /** Хватило бы и без [piece]: лишнюю купюру или монету отдавать не нужно. */
    data class ExtraPiece(val piece: Int) : PaymentCheck

    val correct: Boolean get() = this is Exact || this is WithChange
}

/**
 * Проверка оплаты: денег хватает и ни одну купюру или монету нельзя убрать так, чтобы всё ещё хватало.
 * Так верны и «ровно 17», и «20 без сдачи в 3», и «одна купюра 50», но не «50 и ещё 10».
 */
fun checkPayment(price: Int, pieces: List<Int>): PaymentCheck {
    val paid = pieces.sum()
    if (paid < price) return PaymentCheck.NotEnough(price - paid)
    pieces.filter { paid - it >= price }.maxOrNull()?.let { return PaymentCheck.ExtraPiece(it) }
    return if (paid == price) PaymentCheck.Exact else PaymentCheck.WithChange(paid, paid - price)
}

/** Как ребёнок собрал корзину. */
sealed interface BasketCheck {
    /** Уложился: потратит [total], останется [left]. */
    data class Fits(val total: Int, val left: Int) : BasketCheck

    /** Не хватает [over] монет. */
    data class OverBudget(val over: Int) : BasketCheck

    /** Забыл обязательное: [names]. */
    data class MissingRequired(val names: List<String>) : BasketCheck

    val correct: Boolean get() = this is Fits
}

/** Корзина [chosen] (индексы товаров сцены) верна, если в ней всё обязательное и сумма не больше бюджета. */
fun checkBasket(scene: AdventureScene.Basket, chosen: Set<Int>): BasketCheck {
    // Сначала говорим про забытое главное, потом про деньги
    val missing = scene.items.filterIndexed { i, item -> item.required && i !in chosen }.map { it.name }
    if (missing.isNotEmpty()) return BasketCheck.MissingRequired(missing)
    val total = chosen.sumOf { scene.items[it].price }
    return if (total > scene.budget) BasketCheck.OverBudget(total - scene.budget) else BasketCheck.Fits(total, scene.budget - total)
}

/** Три разных положительных варианта сдачи, среди них верный [change]; по возрастанию, всегда в одном порядке. */
fun changeOptions(change: Int): List<Int> =
    listOf(change, change + 10, change - 5, change + 5, change + 1)
        .filter { it > 0 }
        .distinct()
        .take(3)
        .sorted()
