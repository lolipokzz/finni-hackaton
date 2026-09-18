package ru.larpinovplay.finniapp.domain.game.model

import ru.larpinovplay.finniapp.domain.pet.model.PetGrowthStage

/** Итоги закрытой недели (ТЗ 2.5.9, 2.5.10): что изменилось. Объяснение словами строит слой представления. */
data class WeekSummary(
    val week: Int,
    val foodCovered: Boolean,          // критерий A
    val savedSomething: Boolean,       // критерий C
    val spentMandatory: Int,
    val spentOptional: Int,
    val saved: Int,
    val score: Int,                    // 0..2
    val moodDelta: Int,
    val stageBefore: PetGrowthStage,
    val stageAfter: PetGrowthStage,
) {
    /** Питомец перешёл на следующую стадию. */
    val grew: Boolean get() = stageAfter != stageBefore
}
