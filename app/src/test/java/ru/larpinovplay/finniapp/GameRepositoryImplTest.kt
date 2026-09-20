package ru.larpinovplay.finniapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import ru.larpinovplay.finniapp.data.content.defaultContent
import ru.larpinovplay.finniapp.data.game.GameRepositoryImpl
import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.game.model.PurchaseResult
import ru.larpinovplay.finniapp.domain.game.repository.requireSnapshot
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.storage.StorageError
import ru.larpinovplay.finniapp.domain.util.result.Result
import ru.larpinovplay.finniapp.domain.util.result.dataOrNull

/**
 * Репозиторий связывает правила игры и хранилище: правила проверяет GameEngineTest, диск подменён
 * [FakeGameStore]. Главное здесь порядок «сначала запись, потом публикация» и то, что сбой хранения не портит игру.
 */
class GameRepositoryImplTest {

    private val food = defaultContent().shopItems.first { it.category == ShopCategory.MANDATORY }
    private val newborn = SampleGames.newborn
    private val store = FakeGameStore()
    private val game = GameRepositoryImpl(store, startBalance = 100)

    @Test
    fun gameDoesNotExistUntilPetIsCreated() {
        assertNull(game.snapshot.value)
    }

    @Test
    fun creatingPetStartsGameAndSavesIt() = runBlocking {
        val result = game.createPet(newborn)

        assertTrue(result is Result.Success)
        val snapshot = game.requireSnapshot()
        assertEquals(newborn, snapshot.pet)
        assertEquals(100, snapshot.state.balance)
        assertEquals(1, snapshot.state.week)
        assertEquals(listOf(snapshot), store.saves)
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

        val result = game.buy(food).dataOrNull()

        assertTrue(result is PurchaseResult.Success)
        val snapshot = game.requireSnapshot()
        assertEquals(100 - food.price, snapshot.state.balance)
        assertEquals(newborn.changeSatiety(food.satiety).changeMood(food.mood), snapshot.pet)
        assertEquals(snapshot, store.saved)   // на диске ровно то, что видят экраны
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
    fun ruleRejectionKeepsSnapshotAndWritesNothing() = runBlocking {
        val poor = GameRepositoryImpl(store, startBalance = 1)
        poor.createPet(newborn)
        val before = poor.requireSnapshot()
        val savesBefore = store.saves.size

        val result = poor.buy(food)

        assertEquals(Result.Success(PurchaseResult.NotEnough(missing = food.price - 1)), result)
        assertEquals(before, poor.requireSnapshot())
        assertEquals(savesBefore, store.saves.size)
    }

    @Test
    fun finishingWeekStoresGrownPetAndAdvancesWeek() = runBlocking {
        game.createPet(newborn)
        game.buy(food)

        val summary = game.finishWeek().dataOrNull()

        assertEquals(1, checkNotNull(summary).score)
        val snapshot = game.requireSnapshot()
        assertEquals(2, snapshot.state.week)
        assertEquals(1, snapshot.pet.growthPoints)
        assertEquals(snapshot, store.saved)
    }

    // ---------- Сбои хранения ----------

    @Test
    fun failedSaveLeavesSnapshotUnchangedAndReportsError() = runBlocking {
        game.createPet(newborn)
        val before = game.requireSnapshot()
        store.saveFailure = StorageError.WRITE_FAILED

        val result = game.buy(food)

        assertEquals(Result.Error(StorageError.WRITE_FAILED), result)
        assertEquals(before, game.requireSnapshot())   // покупка не применилась ни в памяти, ни на диске
        assertEquals(before, store.saved)
    }

    @Test
    fun commandsWorkAgainOnceStoreRecovers() = runBlocking {
        game.createPet(newborn)
        store.saveFailure = StorageError.WRITE_FAILED
        game.buy(food)
        store.saveFailure = null

        val result = game.buy(food).dataOrNull()

        assertTrue(result is PurchaseResult.Success)
        assertEquals(100 - food.price, game.requireSnapshot().state.balance)   // ровно одна покупка, не две
    }

    @Test
    fun failedSaveOfNewPetLeavesNoGame() = runBlocking {
        store.saveFailure = StorageError.WRITE_FAILED

        val result = game.createPet(newborn)

        assertEquals(Result.Error(StorageError.WRITE_FAILED), result)
        assertNull(game.snapshot.value)
        assertTrue(store.saves.isEmpty())
    }

    // ---------- Загрузка ----------

    @Test
    fun loadPublishesSavedGame() = runBlocking {
        val saved = SampleGames.rich()
        val restarted = GameRepositoryImpl(FakeGameStore(saved))

        val result = restarted.load()

        assertEquals(Result.Success(saved), result)
        assertEquals(saved, restarted.snapshot.value)
    }

    @Test
    fun loadWithoutSaveReportsNoGame() = runBlocking {
        val result = game.load()

        assertEquals(Result.Success(null), result)
        assertNull(game.snapshot.value)
    }

    @Test
    fun loadFailureLeavesNoGameAndReportsReason() = runBlocking {
        store.saved = SampleGames.rich()
        store.loadFailure = StorageError.CORRUPTED

        val result = game.load()

        assertEquals(Result.Error(StorageError.CORRUPTED), result)
        assertNull(game.snapshot.value)
    }

    @Test
    fun gameContinuesAfterRestart() = runBlocking {
        game.createPet(newborn)
        game.buy(food)
        val restarted = GameRepositoryImpl(store)   // тот же «диск», новый экземпляр — как после перезапуска

        restarted.load()
        val summary = restarted.finishWeek().dataOrNull()

        assertEquals(1, checkNotNull(summary).score)
        assertEquals(2, restarted.requireSnapshot().state.week)
    }

    private suspend fun expectIllegalState(block: suspend () -> Unit) {
        try {
            block()
            fail("Ожидался IllegalStateException")
        } catch (_: IllegalStateException) {
        }
    }
}
