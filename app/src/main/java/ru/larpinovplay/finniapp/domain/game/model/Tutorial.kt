package ru.larpinovplay.finniapp.domain.game.model

import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory

/** Шаг обучения на главном экране — первая неделя нового питомца. */
enum class TutorialStep { PLAN, TASKS, GOAL, SHOP, DEEDS }

/**
 * Текущий шаг обучения; null — обучения нет или оно закончено. Шаг не хранится, а следует из игры: первый по порядку,
 * который ещё не сделан и не пропущен.
 */
val GameState.tutorialStep: TutorialStep?
    get() = if (!tutorial) null else TutorialStep.entries.firstOrNull { it !in tutorialSkipped && !it.doneIn(this) }

/** Шаг, который нельзя пропустить. */
val TutorialStep.required: Boolean get() = this == TutorialStep.PLAN || this == TutorialStep.TASKS

private fun TutorialStep.doneIn(game: GameState): Boolean = when (this) {
    TutorialStep.PLAN -> game.phase != PeriodPhase.PLANNING
    TutorialStep.TASKS -> game.levelResults.isNotEmpty()   // пройден хоть один уровень — с ошибками или без, неважно
    TutorialStep.GOAL -> game.goal != null
    TutorialStep.SHOP -> game.purchases.any { it.category == ShopCategory.MANDATORY }
    TutorialStep.DEEDS -> false                            // последний шаг закрывает окно «Обучение пройдено»
}
