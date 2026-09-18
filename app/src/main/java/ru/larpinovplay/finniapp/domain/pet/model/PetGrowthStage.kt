package ru.larpinovplay.finniapp.domain.pet.model

/** Стадии роста и очки, с которых они начинаются (docs/04-rules-and-formulas.md, «Рост питомца»). */
enum class PetGrowthStage(val order: Int, val minGrowthPoints: Int) {
    BABY(order = 0, minGrowthPoints = 0),
    TEEN(order = 1, minGrowthPoints = 5),
    ADULT(order = 2, minGrowthPoints = 10);

    /** Следующая стадия; null — эта последняя. */
    val next: PetGrowthStage? get() = entries.getOrNull(order + 1)

    companion object {
        /** Последняя стадия, у которой `minGrowthPoints <= points`. */
        fun forPoints(points: Int): PetGrowthStage = entries.last { it.minGrowthPoints <= points }
    }
}
