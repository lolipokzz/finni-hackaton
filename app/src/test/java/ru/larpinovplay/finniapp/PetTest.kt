package ru.larpinovplay.finniapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.larpinovplay.finniapp.domain.pet.model.MoodLevel
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetGrowthStage
import ru.larpinovplay.finniapp.domain.pet.model.PetLook
import ru.larpinovplay.finniapp.domain.pet.model.PetMood
import ru.larpinovplay.finniapp.domain.pet.model.PetSatiety
import ru.larpinovplay.finniapp.domain.pet.model.PetSpecies

class PetTest {

    private val pet = Pet.newborn("Финни", PetLook(PetSpecies.CAT, PetColor.MINT))

    @Test
    fun newbornIsBabyWithNeutralStats() {
        assertEquals(PetGrowthStage.BABY, pet.growthStage)
        assertEquals(0, pet.growthPoints)
        assertEquals(5, pet.pointsToNextStage)
        assertFalse(pet.isHungry)
        assertEquals(MoodLevel.NEUTRAL, pet.mood.level)
    }

    @Test
    fun satietyAndMoodAreClampedTo0To100() {
        assertEquals(100, pet.changeSatiety(+500).satiety.value)
        assertEquals(0, pet.changeSatiety(-500).satiety.value)
        assertEquals(100, pet.changeMood(+500).mood.value)
        assertEquals(0, pet.changeMood(-500).mood.value)
    }

    @Test
    fun petIsHungryBelowThirty() {
        assertTrue(PetSatiety(29).isHungry)
        assertFalse(PetSatiety(30).isHungry)
        assertTrue(pet.changeSatiety(-41).isHungry)
    }

    @Test
    fun moodLevelThresholdsAreFortyAndSeventy() {
        assertEquals(MoodLevel.SAD, PetMood(39).level)
        assertEquals(MoodLevel.NEUTRAL, PetMood(40).level)
        assertEquals(MoodLevel.NEUTRAL, PetMood(69).level)
        assertEquals(MoodLevel.HAPPY, PetMood(70).level)
    }

    @Test
    fun stageIsDerivedFromGrowthPoints() {
        assertEquals(PetGrowthStage.BABY, pet.grow(4).growthStage)
        assertEquals(PetGrowthStage.TEEN, pet.grow(5).growthStage)
        assertEquals(PetGrowthStage.TEEN, pet.grow(9).growthStage)
        assertEquals(PetGrowthStage.ADULT, pet.grow(10).growthStage)
        assertEquals(PetGrowthStage.ADULT, pet.grow(50).growthStage)
    }

    @Test
    fun pointsToNextStageShrinksAndEndsAtLastStage() {
        assertEquals(1, pet.grow(4).pointsToNextStage)
        assertEquals(5, pet.grow(5).pointsToNextStage)
        assertNull(pet.grow(10).pointsToNextStage)
    }

    @Test(expected = IllegalArgumentException::class)
    fun growthNeverGoesDown() {
        pet.grow(-1)
    }
}
