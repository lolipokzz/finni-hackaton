package ru.larpinovplay.finniapp.domain.pet.model

/** Сытость 0..100; клампится при изменении. */
data class PetSatiety(val value: Int) {

    /** Ниже порога питомец голоден и грустит. */
    val isHungry: Boolean get() = value <= HUNGRY_AT

    operator fun plus(delta: Int): PetSatiety = PetSatiety((value + delta).coerceIn(MIN, MAX))

    companion object {
        const val MIN = 0
        const val MAX = 100
        /** Сытость, с которой питомец голоден (включительно): эмоция, реплика и подсказки — от одного порога. */
        const val HUNGRY_AT = 30
    }
}
