package ru.larpinovplay.finniapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.larpinovplay.finniapp.data.content.defaultContent
import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetGrowthStage
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.task.evaluate
import ru.larpinovplay.finniapp.domain.task.model.TaskAnswer
import ru.larpinovplay.finniapp.domain.task.model.TaskPayload
import ru.larpinovplay.finniapp.domain.task.model.TaskTopic

/**
 * Минимальный объём демонстрационного контента, ТЗ 2.6. Если кто-то уберёт задание, товар или цель ниже минимума,
 * тест упадёт раньше, чем это заметят эксперты.
 */
class ContentMinimumsTest {

    private val content = defaultContent()

    @Test
    fun atLeastSixTasksCoveringAllThreeTopics() {
        assertTrue("заданий ${content.tasks.size}", content.tasks.size >= 6)
        val byTopic = content.tasks.groupingBy { it.topic }.eachCount()
        // Три темы ТЗ 2.5.8: планирование бюджета, сбережения, платежи и покупки — и в каждой больше одного задания
        TaskTopic.entries.forEach { topic ->
            assertTrue("в теме $topic заданий ${byTopic[topic] ?: 0}", (byTopic[topic] ?: 0) >= 2)
        }
        assertEquals(content.tasks.size, content.tasks.map { it.id }.toSet().size)
    }

    @Test
    fun choiceTasksHaveOneRightAnswerAndExplainEveryOption() {
        content.tasks.forEach { task ->
            val choice = task.payload as? TaskPayload.Choice ?: return@forEach
            assertEquals("верных вариантов в ${task.id}", 1, choice.options.count { it.correct })
            // Правильный и ошибочный варианты проходятся, и после любого есть объяснение (ТЗ 2.5.8, 2.6)
            choice.options.forEach { option ->
                val outcome = task.evaluate(TaskAnswer.Choice(option.id))
                assertEquals("${task.id}/${option.id}", option.correct, outcome.success)
                assertTrue("${task.id}/${option.id} без объяснения", outcome.explanation.isNotBlank())
            }
        }
    }

    @Test
    fun atLeastEightPurchasesOfBothTypes() {
        assertTrue("товаров ${content.shopItems.size}", content.shopItems.size >= 8)
        ShopCategory.entries.forEach { category ->
            assertTrue("нет товаров «$category»", content.shopItems.any { it.category == category })
        }
    }

    @Test
    fun atLeastThreeGoals() {
        assertTrue("целей ${content.goals.size}", content.goals.size >= 3)
    }

    @Test
    fun atLeastNinePetLooksAndThreeStages() {
        assertTrue("раскрасок ${PetColor.entries.size}", PetColor.entries.size >= 9)
        assertTrue("стадий ${PetGrowthStage.entries.size}", PetGrowthStage.entries.size >= 3)
    }
}
