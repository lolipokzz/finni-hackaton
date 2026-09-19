package ru.larpinovplay.finniapp.domain.pet.model

/** Сытость 0..100; клампится при изменении. */
data class PetSatiety(val value: Int) {

    /** Ниже порога питомец голоден и грустит (docs/04-rules-and-formulas.md). */
    val isHungry: Boolean get() = value < HUNGRY_BELOW

    operator fun plus(delta: Int): PetSatiety = PetSatiety((value + delta).coerceIn(MIN, MAX))

    companion object {
        const val MIN = 0
        const val MAX = 100
        const val HUNGRY_BELOW = 30
    }
}
