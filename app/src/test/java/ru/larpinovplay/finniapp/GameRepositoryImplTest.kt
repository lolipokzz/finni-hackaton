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
import ru.larpinovplay.finniapp.domain.game.engine.GameRules
import ru.larpinovplay.finniapp.domain.game.model.BudgetPlan
import ru.larpinovplay.finniapp.domain.game.model.FinishBlock
import ru.larpinovplay.finniapp.domain.game.model.FinishWeekResult
import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.game.model.PurchaseResult
import ru.larpinovplay.finniapp.domain.game.repository.requireSnapshot
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.storage.StorageError
import ru.larpinovplay.finniapp.domain.util.result.Result
import ru.larpinovplay.finniapp.domain.util.result.dataOrNull

/** Репозиторий связывает правила игры и хранилище: правила проверяет GameEngineTest, диск подменён [FakeGameStore]. */
class GameRepositoryImplTest {

    private val food = defaultContent().shopItems.first { it.category == ShopCategory.MANDATORY }
    private val newborn = SampleGames.newborn
    private val store = FakeGameStore()
    private val clock = TestClock()
    private val game = GameRepositoryImpl(store, startBalance = 100, clock = clock)
    private val plan = BudgetPlan(mandatory = food.price, optional = 100 - food.price)

    /** Раздел взрослого обещает демо с [GameRules.START_BALANCE] монет на первой неделе — так и есть. */
    @Test
    fun demoStartsWithStartBalanceOnFirstWeek() = runBlocking {
        val game = GameRepositoryImpl(FakeGameStore(), clock = clock)

        game.resetToDemo()

        val state = game.requireSnapshot().state
        assertTrue(state.demoMode)
        assertEquals(GameRules.START_BALANCE, state.balance)
        assertEquals(1, state.week)
        assertEquals("Финни Демо", game.requireSnapshot().pet.name)
    }

    /**
     * Демо не стирает игру ребёнка: она откладывается, переживает перезапуск и повторный сброс демо,
     * а после выхода возвращается такой, какой была. Монеты в демо добавляются, в обычной игре — нет.
     */
    @Test
    fun demoKeepsChildsGameAndExitBringsItBack() = runBlocking {
        game.createPet(newborn)
        game.confirmPlan(plan)
        game.buy(food)
        val childsGame = game.requireSnapshot()
        assertEquals(Result.Success(false), game.addDemoCoins(GameRules.DEMO_COINS))   // не демо: монет не даём

        game.resetToDemo()
        assertTrue(game.requireSnapshot().state.demoMode)
        assertEquals(childsGame, game.gameBeforeDemo.value)
        assertEquals(childsGame, store.beforeDemo)

        assertEquals(Result.Success(true), game.addDemoCoins(GameRules.DEMO_COINS))
        assertEquals(100 + GameRules.DEMO_COINS, game.requireSnapshot().state.balance)   // у этого репозитория старт — 100
        game.resetToDemo()   // повторный сброс демо откладывает всё ту же игру ребёнка, а не прежнее демо
        assertEquals(childsGame, store.beforeDemo)

        // Перезапуск посреди демо: отложенная игра читается с диска
        val restarted = GameRepositoryImpl(store, startBalance = 100, clock = clock)
        restarted.load()
        assertTrue(restarted.requireSnapshot().state.demoMode)
        assertEquals(childsGame, restarted.gameBeforeDemo.value)

        assertTrue(restarted.exitDemo() is Result.Success)
        assertEquals(childsGame, restarted.requireSnapshot())
        assertNull(restarted.gameBeforeDemo.value)
        assertEquals(childsGame, store.saved)
        assertNull(store.beforeDemo)
    }

    /** Демо без игры ребёнка (например, включено из старого сохранения): выход ведёт к созданию питомца. */
    @Test
    fun exitDemoWithoutSavedGameLeavesNoGame() = runBlocking {
        game.resetToDemo()
        assertNull(game.gameBeforeDemo.value)

        game.exitDemo()

        assertNull(game.snapshot.value)
        assertNull(store.saved)
    }

    /** Сбой записи на входе в демо: игра ребёнка остаётся текущей и не теряется. */
    @Test
    fun failedDemoStartKeepsChildsGame() = runBlocking {
        game.createPet(newborn)
        val childsGame = game.requireSnapshot()
        store.saveFailure = StorageError.WRITE_FAILED

        val result = game.resetToDemo()

        assertTrue(result is Result.Error)
        assertEquals(childsGame, game.requireSnapshot())
        assertNull(game.gameBeforeDemo.value)
    }

    @Test
    fun resetProfileAlsoForgetsGameBeforeDemo() = runBlocking {
        game.createPet(newborn)
        game.resetToDemo()

        game.resetProfile()

        assertNull(game.snapshot.value)
        assertNull(game.gameBeforeDemo.value)
        assertNull(store.beforeDemo)
    }

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
        game.confirmPlan(plan)

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
        game.confirmPlan(plan)
        game.buy(food)
        collector.cancel()

        // null → игра создана → план → куплено. Промежуточного «монеты списаны, питомец не накормлен» нет
        assertEquals(4, seen.size)
        seen.filterNotNull().forEach { snapshot ->
            val bought = snapshot.state.purchases.isNotEmpty()
            assertEquals(bought, snapshot.pet.satiety.value > newborn.satiety.value)
        }
    }

    @Test
    fun ruleRejectionKeepsSnapshotAndWritesNothing() = runBlocking {
        val poor = GameRepositoryImpl(store, startBalance = 1)
        poor.createPet(newborn)
        poor.confirmPlan(BudgetPlan(optional = 1))
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
        game.confirmPlan(plan)
        game.buy(food)
        clock.nextDay()

        val result = game.finishWeek().dataOrNull()

        assertEquals(2, (result as FinishWeekResult.Finished).summary.steps)   // не скучает и траты по плану
        val snapshot = game.requireSnapshot()
        assertEquals(2, snapshot.state.week)
        assertEquals(2, snapshot.pet.growthPoints)
        assertEquals(clock.date, snapshot.state.periodStartedOn)
        assertEquals(snapshot, store.saved)
    }

    /** До плана недели магазин закрыт: монеты сначала раскладывают, потом тратят. */
    @Test
    fun buyingBeforePlanIsRejected() = runBlocking {
        game.createPet(newborn)

        assertEquals(PurchaseResult.NoPlan, game.buy(food).dataOrNull())
        assertEquals(100, game.requireSnapshot().state.balance)

        game.confirmPlan(plan)
        assertTrue(game.buy(food).dataOrNull() is PurchaseResult.Success)
    }

    /**
     * Тупика нет: ребёнок не купил еды, спустил всё на необязательное — и всё равно может закончить неделю голодным
     * (уровни и приключение монет не требуют). Новая неделя приносит карманные, на них и покупается еда.
     */
    @Test
    fun hungryAndBrokeChildCanStillFinishWeek() = runBlocking {
        val content = defaultContent()
        val full = GameRepositoryImpl(FakeGameStore(), clock = clock, adventures = content.adventures, levels = content.levels)
        full.createPet(newborn)
        val balance = full.requireSnapshot().state.balance
        full.confirmPlan(BudgetPlan(optional = balance))
        val treats = content.shopItems.filter { it.category == ShopCategory.OPTIONAL && !it.isWearable }
        while (true) {
            val left = full.requireSnapshot().state.balance
            val treat = treats.firstOrNull { it.price <= left } ?: break
            full.buy(treat)
        }
        content.levels.filter { it.week == 1 }.forEach { full.completeLevel(it, mistakes = 0) }
        full.completeAdventure(content.adventures.first(), mistakes = 0)
        clock.nextDay()

        val result = full.finishWeek().dataOrNull()

        assertTrue(result is FinishWeekResult.Finished)
        assertTrue(!(result as FinishWeekResult.Finished).summary.deeds.fed)   // голодным — можно, дело «сыт» просто не засчитано
        assertTrue(full.requireSnapshot().state.balance > 0)                   // карманные новой недели пришли
    }

    @Test
    fun weekStartedTodayIsNotFinishedAndNotSaved() = runBlocking {
        game.createPet(newborn)
        assertEquals(FinishBlock.PLAN_NOT_CONFIRMED, game.finishBlock())
        game.confirmPlan(plan)
        val before = game.requireSnapshot()
        val savesBefore = store.saves.size

        val result = game.finishWeek()

        assertEquals(Result.Success(FinishWeekResult.Blocked(FinishBlock.SAME_DAY)), result)
        assertEquals(FinishBlock.SAME_DAY, game.finishBlock())
        assertEquals(before, game.requireSnapshot())
        assertEquals(savesBefore, store.saves.size)
        clock.nextDay()
        assertNull(game.finishBlock())
    }

    // ---------- Сбои хранения ----------

    @Test
    fun failedSaveLeavesSnapshotUnchangedAndReportsError() = runBlocking {
        game.createPet(newborn)
        game.confirmPlan(plan)
        val before = game.requireSnapshot()
        store.saveFailure = StorageError.WRITE_FAILED

        val result = game.buy(food)

        assertEquals(Result.Error(StorageError.WRITE_FAILED), result)   // ошибка уходит в ViewModel
        assertEquals(before, game.requireSnapshot())   // покупка не применилась ни в памяти, ни на диске
        assertEquals(before, store.saved)
    }

    @Test
    fun commandsWorkAgainOnceStoreRecovers() = runBlocking {
        game.createPet(newborn)
        game.confirmPlan(plan)
        store.saveFailure = StorageError.WRITE_FAILED
        game.buy(food)
        store.saveFailure = null

        val result = game.buy(food).dataOrNull()

        assertTrue(result is PurchaseResult.Success)
        assertEquals(100 - food.price, game.requireSnapshot().state.balance)   // ровно одна покупка, не две
        assertEquals(game.requireSnapshot(), store.saved)
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
        game.confirmPlan(plan)
        game.buy(food)
        val restarted = GameRepositoryImpl(store, clock = clock)   // тот же «диск», новый экземпляр — как после перезапуска
        clock.nextDay()

        restarted.load()
        val result = restarted.finishWeek().dataOrNull()

        assertEquals(2, (result as FinishWeekResult.Finished).summary.steps)
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
