package ru.larpinovplay.finniapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import ru.larpinovplay.finniapp.data.content.defaultContent
import ru.larpinovplay.finniapp.data.game.InMemoryGameRepository
import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.game.model.PurchaseResult
import ru.larpinovplay.finniapp.domain.game.repository.requireSnapshot
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetLook
import ru.larpinovplay.finniapp.domain.pet.model.PetSpecies
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory

/** Репозиторий только хранит игру вместе с питомцем: правила проверяет GameEngineTest. */
class InMemoryGameRepositoryTest {

    private val food = defaultContent().shopItems.first { it.category == ShopCategory.MANDATORY }
    private val newborn = Pet.newborn("Финни", PetLook(PetSpecies.BUNNY, PetColor.CORAL))
    private val game = InMemoryGameRepository(startBalance = 100)

    @Test
    fun gameDoesNotExistUntilPetIsCreated() {
        assertNull(game.snapshot.value)
    }

    @Test
    fun creatingPetStartsGameWithThatPet() = runBlocking {
        game.createPet(newborn)

        val snapshot = game.requireSnapshot()
        assertEquals(newborn, snapshot.pet)
        assertEquals(100, snapshot.state.balance)
        assertEquals(1, snapshot.state.week)
    }

    @Test
    fun secondPetIsRejected() = runBlocking {
        game.createPet(newborn)

        expectIllegalState { game.createPet(newborn) }
    }

    @Test
    fun commandBeforePetIsRejected() = runBlocking {
        expectIllegalState { game.buy(food) }
    }

    @Test
    fun buyingChangesGameAndFeedsPetInOneSnapshot() = runBlocking {
        game.createPet(newborn)

        val result = game.buy(food)

        assertTrue(result is PurchaseResult.Success)
        val snapshot = game.requireSnapshot()
        assertEquals(100 - food.price, snapshot.state.balance)
        assertEquals(newborn.changeSatiety(food.satiety).changeMood(food.mood), snapshot.pet)
    }

    @Test
    fun observersNeverSeeGameAndPetOutOfStep() = runBlocking {
        val seen = mutableListOf<GameSnapshot?>()
        val collector = launch(Dispatchers.Unconfined) { game.snapshot.toList(seen) }

        game.createPet(newborn)
        game.buy(food)
        collector.cancel()

        // null → игра создана → куплено. Промежуточного «монеты списаны, питомец не накормлен» нет
        assertEquals(3, seen.size)
        seen.filterNotNull().forEach { snapshot ->
            val bought = snapshot.state.purchases.isNotEmpty()
            assertEquals(bought, snapshot.pet.satiety.value > newborn.satiety.value)
        }
    }

    @Test
    fun rejectedCommandLeavesSnapshotUntouched() = runBlocking {
        val poor = InMemoryGameRepository(startBalance = 1)
        poor.createPet(newborn)
        val before = poor.requireSnapshot()

        val result = poor.buy(food)

        assertEquals(PurchaseResult.NotEnough(missing = food.price - 1), result)
        assertEquals(before, poor.requireSnapshot())
    }

    @Test
    fun finishingWeekStoresGrownPetAndAdvancesWeek() = runBlocking {
        game.createPet(newborn)
        game.buy(food)

        val summary = game.finishWeek()

        assertEquals(1, summary.score)
        val snapshot = game.requireSnapshot()
        assertEquals(2, snapshot.state.week)
        assertEquals(1, snapshot.pet.growthPoints)
        assertNotNull(snapshot.state.history.lastOrNull())
    }

    private suspend fun expectIllegalState(block: suspend () -> Unit) {
        try {
            block()
            fail("Ожидался IllegalStateException")
        } catch (_: IllegalStateException) {
        }
    }
}
