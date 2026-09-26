package ru.larpinovplay.finniapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.larpinovplay.finniapp.data.content.defaultContent
import ru.larpinovplay.finniapp.domain.adventure.BasketCheck
import ru.larpinovplay.finniapp.domain.adventure.checkBasket
import ru.larpinovplay.finniapp.domain.adventure.model.AdventureScene

/**
 * Приключения — данные, и новое добавляется без кода. Этот тест ловит ошибки в самих данных:
 * вопрос без верного ответа, сдачу без оплаты, ссылку сюжета на несуществующий вариант.
 */
class AdventureContentTest {

    private val adventures = defaultContent().adventures

    @Test
    fun adventuresHaveUniqueIdsAndFairRewards() {
        assertEquals(adventures.size, adventures.map { it.id }.toSet().size)
        adventures.forEach {
            assertTrue(it.id, it.scenes.isNotEmpty())
            assertTrue(it.id, it.rewardOnMistake in 1..it.reward)   // ошибка уменьшает награду, но не обнуляет
        }
    }

    @Test
    fun everyQuestionHasARightAnswerAndOptionIdsAreUnique() {
        adventures.forEach { adventure ->
            val choices = adventure.scenes.filterIsInstance<AdventureScene.Choice>()
            choices.forEach { choice ->
                assertTrue("${adventure.id}: ${choice.text}", choice.options.size >= 2)
                assertTrue("${adventure.id}: ${choice.text}", choice.options.any { it.correct })
            }
            val ids = choices.flatMap { c -> c.options.map { it.id } }
            assertEquals(adventure.id, ids.size, ids.toSet().size)
        }
    }

    @Test
    fun storyVariantsFollowEarlierChoices() {
        adventures.forEach { adventure ->
            adventure.scenes.forEachIndexed { index, scene ->
                if (scene !is AdventureScene.Story) return@forEachIndexed
                val earlier = adventure.scenes.take(index).filterIsInstance<AdventureScene.Choice>().flatMap { c -> c.options.map { it.id } }
                scene.variants.keys.forEach { key -> assertTrue("${adventure.id}: $key", key in earlier) }
            }
        }
    }

    @Test
    fun changeComesAfterPaymentForTheSamePrice() {
        adventures.forEach { adventure ->
            adventure.scenes.forEachIndexed { index, scene ->
                if (scene !is AdventureScene.Change) return@forEachIndexed
                val pay = adventure.scenes.take(index).lastOrNull { it is AdventureScene.Pay } as? AdventureScene.Pay
                assertEquals(adventure.id, scene.price, pay?.price)
            }
        }
    }

    @Test
    fun paymentsAndBasketsCanBeSolved() {
        adventures.flatMap { it.scenes }.forEach { scene ->
            when (scene) {
                is AdventureScene.Pay -> assertTrue(scene.text, scene.wallet.sum() >= scene.price)
                is AdventureScene.Basket -> {
                    val required = scene.items.indices.filter { scene.items[it].required }.toSet()
                    assertTrue(scene.text, checkBasket(scene, required) is BasketCheck.Fits)
                }
                else -> Unit
            }
        }
    }
}
