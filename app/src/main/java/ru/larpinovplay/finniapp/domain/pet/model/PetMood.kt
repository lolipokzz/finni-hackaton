package ru.larpinovplay.finniapp.domain.pet.model

data class PetMood(val value: Int) {   // хранится 0..100, клампится при изменении
    val level: MoodLevel get() = when {
        value < 30 -> MoodLevel.SAD
        value < 70 -> MoodLevel.NEUTRAL
        else       -> MoodLevel.HAPPY
    }
}
enum class MoodLevel { SAD, NEUTRAL, HAPPY }