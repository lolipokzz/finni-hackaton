package ru.larpinovplay.finniapp.domain.pet.model

/**
 * Стадии роста и шаги, с которых они начинаются. Шаг — одно сделанное дело недели, их 4 за неделю: при всех делах
 * подросток на 2-й неделе, взрослый на 4-й.
 */
enum class PetGrowthStage(val order: Int, val minGrowthPoints: Int) {
    BABY(order = 0, minGrowthPoints = 0),
    TEEN(order = 1, minGrowthPoints = 8),
    ADULT(order = 2, minGrowthPoints = 16);

    /** Следующая стадия; null — эта последняя. */
    val next: PetGrowthStage? get() = entries.getOrNull(order + 1)

    companion object {
        /** Последняя стадия, у которой `minGrowthPoints <= points`. */
        fun forPoints(points: Int): PetGrowthStage = entries.last { it.minGrowthPoints <= points }
    }
}
