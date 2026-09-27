package ru.larpinovplay.finniapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.larpinovplay.finniapp.domain.adventure.BasketCheck
import ru.larpinovplay.finniapp.domain.adventure.PaymentCheck
import ru.larpinovplay.finniapp.domain.adventure.changeOptions
import ru.larpinovplay.finniapp.domain.adventure.checkBasket
import ru.larpinovplay.finniapp.domain.adventure.checkPayment
import ru.larpinovplay.finniapp.domain.adventure.model.AdventureScene

/** Оплата в приключении: хватает и нет лишнего — верно, как бы ребёнок ни сложил сумму. */
class AdventureRulesTest {

    @Test
    fun exactSumIsCorrectWithoutChange() {
        assertEquals(PaymentCheck.Exact, checkPayment(17, listOf(10, 5, 2)))
    }

    @Test
    fun biggerBillWithoutExtraPiecesGivesChange() {
        assertEquals(PaymentCheck.WithChange(paid = 20, change = 3), checkPayment(17, listOf(10, 10)))
        assertEquals(PaymentCheck.WithChange(paid = 50, change = 33), checkPayment(17, listOf(50)))
    }

    @Test
    fun notEnoughSaysHowMuchIsMissing() {
        assertEquals(PaymentCheck.NotEnough(missing = 2), checkPayment(17, listOf(10, 5)))
    }

    @Test
    fun pieceThatIsNotNeededIsNamed() {
        // 10 + 5 + 2 + 2 = 19: хватит и без одной двойки
        assertEquals(PaymentCheck.ExtraPiece(2), checkPayment(17, listOf(10, 5, 2, 2)))
        // 50 + 10: хватит и без десятки, а без полтинника нет
        assertEquals(PaymentCheck.ExtraPiece(10), checkPayment(17, listOf(50, 10)))
    }

    @Test
    fun changeOptionsAreThreeDifferentPositiveNumbersWithTheRightOne() {
        listOf(1, 3, 5, 33).forEach { change ->
            val options = changeOptions(change)
            assertEquals(3, options.size)
            assertEquals(options.sorted(), options)
            assertEquals(options.distinct(), options)
            assertTrue(options.all { it > 0 })
            assertTrue(change in options)
        }
    }

    private val park = AdventureScene.Basket(
        text = "", budget = 30, hint = "",
        items = listOf(
            AdventureScene.Basket.Item("Карусель", 15, required = true),
            AdventureScene.Basket.Item("Мороженое", 12),
            AdventureScene.Basket.Item("Шарик", 8),
        ),
    )

    @Test
    fun basketFitsWhenRequiredIsInAndBudgetHolds() {
        assertEquals(BasketCheck.Fits(total = 27, left = 3), checkBasket(park, setOf(0, 1)))
    }

    @Test
    fun basketOverBudgetSaysByHowMuch() {
        assertEquals(BasketCheck.OverBudget(over = 5), checkBasket(park, setOf(0, 1, 2)))
    }

    @Test
    fun forgottenRequiredItemIsNamedFirst() {
        assertEquals(BasketCheck.MissingRequired(listOf("Карусель")), checkBasket(park, setOf(1, 2)))
    }
}
