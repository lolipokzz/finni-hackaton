package ru.larpinovplay.finniapp.domain.pet.model

/**
 * Питомец: единственный источник правды о его состоянии. Экраны показывают его как есть и
 * не копируют поля в свои состояния, а игра меняет его вместе со своим состоянием ([GameSnapshot][ru.larpinovplay.finniapp.domain.game.model.GameSnapshot]).
 *
 * Неизменяемый: методы изменения возвращают нового питомца. Стадия роста не хранится, а выводится
 * из очков роста, поэтому не может разойтись с ними (docs/04-rules-and-formulas.md, «Рост питомца»).
 */
data class Pet(
    val name: String,
    val look: PetLook,
    val satiety: PetSatiety,           // обратимо: падает каждую неделю, растёт от еды
    val mood: PetMood,                 // обратимо, меняется после каждого решения
    val growthPoints: Int,             // необратимо, только растёт: копится за недели
) {
    val growthStage: PetGrowthStage get() = PetGrowthStage.forPoints(growthPoints)

    /** Сколько очков до следующей стадии; null — стадия последняя. */
    val pointsToNextStage: Int? get() = growthStage.next?.let { it.minGrowthPoints - growthPoints }

    val isHungry: Boolean get() = satiety.isHungry

    fun changeSatiety(delta: Int): Pet = copy(satiety = satiety + delta)

    fun changeMood(delta: Int): Pet = copy(mood = mood + delta)

    fun grow(points: Int): Pet {
        require(points >= 0) { "Очки роста только растут: $points" }
        return copy(growthPoints = growthPoints + points)
    }

    companion object {
        private const val START_SATIETY = 70
        private const val START_MOOD = 50

        /** Только что созданный питомец: малыш с обычной сытостью и настроением. */
        fun newborn(name: String, look: PetLook): Pet = Pet(
            name = name,
            look = look,
            satiety = PetSatiety(START_SATIETY),
            mood = PetMood(START_MOOD),
            growthPoints = 0,
        )
    }
}
