package ru.larpinovplay.finniapp.domain.pet.model

/** Настроение 0..100; клампится при изменении. */
data class PetMood(val value: Int) {

    val level: MoodLevel
        get() = when {
            value < CALM_FROM -> MoodLevel.SAD
            value < HAPPY_FROM -> MoodLevel.NEUTRAL
            else -> MoodLevel.HAPPY
        }

    operator fun plus(delta: Int): PetMood = PetMood((value + delta).coerceIn(MIN, MAX))

    companion object {
        const val MIN = 0
        const val MAX = 100
        const val CALM_FROM = 40
        const val HAPPY_FROM = 70
    }
}

enum class MoodLevel { SAD, NEUTRAL, HAPPY }
