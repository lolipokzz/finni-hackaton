package ru.larpinovplay.finniapp.domain.game.model

import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory

/**
 * Шаг обучения на главном экране — первая неделя нового питомца. Ребёнок делает настоящие действия,
 * а Финни подсказывает: составить план, выбрать мечту, купить еду, решить задание, посмотреть дела недели.
 */
enum class TutorialStep { PLAN, GOAL, SHOP, TASKS, DEEDS }

/**
 * Текущий шаг обучения; null — обучения нет или оно закончено. Шаг не хранится, а следует из игры:
 * первый по порядку, который ещё не сделан и не пропущен. Ребёнок может делать дела в любом порядке,
 * и подсказка всегда про то, что ещё не сделано. План не пропустить: без него неделя не начнётся.
 */
val GameState.tutorialStep: TutorialStep?
    get() = if (!tutorial) null else TutorialStep.entries.firstOrNull { it !in tutorialSkipped && !it.doneIn(this) }

private fun TutorialStep.doneIn(game: GameState): Boolean = when (this) {
    TutorialStep.PLAN -> game.phase != PeriodPhase.PLANNING
    TutorialStep.GOAL -> game.goal != null
    TutorialStep.SHOP -> game.purchases.any { it.category == ShopCategory.MANDATORY }
    TutorialStep.TASKS -> game.taskResults.isNotEmpty()   // решено хоть одно задание — верно или нет, неважно
    TutorialStep.DEEDS -> false                           // последний шаг закрывает окно «Обучение пройдено»
}
