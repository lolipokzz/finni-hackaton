package ru.larpinovplay.finniapp.domain.pet.model

/**
 * Настроение 20..100; клампится при изменении. Ниже 20 не опускается: самое низкое состояние — «скучает»,
 * а не «грустный» (этика, docs/11-economy.md).
 */
data class PetMood(val value: Int) {

    val level: MoodLevel
        get() = when {
            value < CALM_FROM -> MoodLevel.BORED
            value < HAPPY_FROM -> MoodLevel.NEUTRAL
            else -> MoodLevel.HAPPY
        }

    operator fun plus(delta: Int): PetMood = PetMood((value + delta).coerceIn(MIN, MAX))

    companion object {
        const val MIN = 20
        const val MAX = 100
        const val CALM_FROM = 40
        const val HAPPY_FROM = 70
    }
}

/** Подписи: радостный ≥ 70, спокойный ≥ 40, ниже — скучает. */
enum class MoodLevel { BORED, NEUTRAL, HAPPY }
