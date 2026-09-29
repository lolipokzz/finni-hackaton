package ru.larpinovplay.finniapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.larpinovplay.finniapp.data.content.defaultContent
import ru.larpinovplay.finniapp.domain.game.engine.GameRules
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

    private val tasks = content.levels.flatMap { it.tasks }

    @Test
    fun atLeastSixTasksCoveringAllThreeTopics() {
        assertTrue("заданий ${tasks.size}", tasks.size >= 6)
        val byTopic = content.levels.groupBy { it.topic }.mapValues { (_, levels) -> levels.sumOf { it.tasks.size } }
        // Три темы ТЗ 2.5.8: планирование бюджета, сбережения, платежи и покупки — и в каждой больше одного задания
        TaskTopic.entries.forEach { topic ->
            assertTrue("в теме $topic заданий ${byTopic[topic] ?: 0}", (byTopic[topic] ?: 0) >= 2)
        }
        assertEquals(tasks.size, tasks.map { it.id }.toSet().size)
        assertEquals(content.levels.size, content.levels.map { it.id }.toSet().size)
    }

    /** Карта: пять недель подряд (демо ТЗ 2.6), в каждой по уровню на тему. */
    @Test
    fun everyWeekHasALevelPerTopicOfFourToFiveTasks() {
        val weeks = content.levels.groupBy { it.week }
        assertEquals((1..5).toList(), weeks.keys.sorted())
        weeks.forEach { (week, levels) ->
            assertEquals("темы недели $week", TaskTopic.entries.toSet(), levels.map { it.topic }.toSet())
        }
        // Заданий в уровне больше, чем проходит ребёнок: каждый раз выбираются случайные
        content.levels.forEach { assertTrue("${it.id}: ${it.tasks.size} заданий", it.tasks.size > GameRules.TASKS_PER_LEVEL) }
    }

    /** У раскладки и списка покупок есть верное решение, а у каждого задания — объяснение (ТЗ 2.5.8). */
    @Test
    fun everyTaskCanBeSolvedAndIsExplained() {
        tasks.forEach { task ->
            assertTrue("${task.id} без объяснений", task.explanationSuccess.isNotBlank() && task.explanationMistake.isNotBlank())
            when (val p = task.payload) {
                is TaskPayload.Allocate -> {
                    val optional = p.total - p.mandatoryMin - p.savingsMin
                    assertTrue("${task.id}: минимумы больше суммы", optional >= 0)
                    val right = TaskAnswer.Allocation(mapOf("mandatory" to p.mandatoryMin, "optional" to optional, "savings" to p.savingsMin))
                    assertTrue(task.id, task.evaluate(right).success)
                    assertFalse(task.id, task.evaluate(TaskAnswer.Allocation(mapOf("optional" to p.total))).success)
                }
                is TaskPayload.ShopList -> {
                    val mandatory = p.items.filter { it.mandatory }.map { it.id }.toSet()
                    assertTrue(task.id, task.evaluate(TaskAnswer.Selection(mandatory)).success)
                    assertFalse(task.id, task.evaluate(TaskAnswer.Selection(p.items.map { it.id }.toSet())).success)
                }
                is TaskPayload.Choice -> Unit   // ниже
            }
        }
    }

    @Test
    fun choiceTasksHaveOneRightAnswerAndExplainEveryOption() {
        tasks.forEach { task ->
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
