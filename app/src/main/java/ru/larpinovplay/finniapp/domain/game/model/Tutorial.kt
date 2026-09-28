package ru.larpinovplay.finniapp.domain.game.model

import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory

/**
 * Шаг обучения на главном экране — первая неделя нового питомца. Ребёнок делает настоящие действия,
 * а Финни подсказывает: составить план, выбрать мечту, купить еду, посмотреть дела недели.
 */
enum class TutorialStep { PLAN, GOAL, SHOP, DEEDS }

/**
 * Текущий шаг обучения; null — обучения нет или оно закончено. Шаг не хранится, а следует из игры:
 * ребёнок может делать дела в любом порядке, и подсказка всегда про то, что ещё не сделано.
 */
val GameState.tutorialStep: TutorialStep?
    get() = when {
        !tutorial -> null
        phase == PeriodPhase.PLANNING -> TutorialStep.PLAN
        goal == null -> TutorialStep.GOAL
        purchases.none { it.category == ShopCategory.MANDATORY } -> TutorialStep.SHOP
        else -> TutorialStep.DEEDS
    }
