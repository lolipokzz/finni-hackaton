package ru.larpinovplay.finniapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.larpinovplay.finniapp.data.game.store.GameSaveFile
import ru.larpinovplay.finniapp.data.game.store.toDomain
import ru.larpinovplay.finniapp.data.game.store.toDto
import ru.larpinovplay.finniapp.data.storage.StorageJson
import ru.larpinovplay.finniapp.domain.game.engine.GameEngine
import ru.larpinovplay.finniapp.domain.game.engine.GameRules
import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.game.model.LedgerReason

/** Формат файла: снимок игры проходит домен → DTO → JSON → DTO → домен без потерь. */
class GameSaveMapperTest {

    private val game = SampleGames.rich()

    @Test
    fun sampleGameCoversEveryKindOfLedgerEntry() {
        // Если пример перестанет содержать какой-то вид записи, тест ниже перестанет его проверять
        val reasons = game.state.ledger.map { it.reason::class }.toSet()

        assertEquals(
            setOf(
                LedgerReason.StartCoins::class, LedgerReason.Purchase::class, LedgerReason.Deposit::class,
                LedgerReason.PlannedDeposit::class, LedgerReason.Withdraw::class, LedgerReason.AdventureReward::class,
                LedgerReason.SavingsBonus::class,
                LedgerReason.GoalReached::class, LedgerReason.TaskReward::class, LedgerReason.WeekIncome::class,
            ),
            reasons,
        )
        assertTrue(game.state.purchases.isNotEmpty())
        assertTrue(game.state.wardrobe.isNotEmpty())
        assertTrue(game.pet.outfit.isNotEmpty())
        assertTrue(game.state.completedGoals.isNotEmpty())
        assertTrue(game.state.trip != null)
        assertTrue(game.state.levelResults.isNotEmpty())
        assertTrue(game.state.goldLevels.isNotEmpty())
        assertTrue(game.state.history.isNotEmpty())
        assertTrue(game.state.adventureResults.isNotEmpty())
    }

    @Test
    fun mappingToDtoAndBackKeepsTheGame() {
        assertEquals(game, game.toDto().toDomain())
    }

    @Test
    fun gameSurvivesJsonRoundTrip() {
        val json = StorageJson.encodeToString(GameSaveFile.serializer(), GameSaveFile(game = game.toDto()))

        val restored = StorageJson.decodeFromString(GameSaveFile.serializer(), json)

        assertEquals(game, restored.game?.toDomain())
    }

    @Test
    fun ledgerReasonNamesInFileAreStable() {
        val json = StorageJson.encodeToString(GameSaveFile.serializer(), GameSaveFile(game = game.toDto()))

        listOf("start_coins", "purchase", "deposit", "planned_deposit", "withdraw", "savings_bonus", "adventure_reward", "goal_reached", "task_reward", "week_income").forEach { name ->
            assertTrue("В файле нет причины $name", json.contains("\"$name\""))
        }
    }

    /** Демо-игра с начисленными монетами и отложенная на время демо игра ребёнка лежат в одном файле. */
    @Test
    fun demoGameAndGameBeforeDemoSurviveJsonRoundTrip() {
        val demoStart = GameSnapshot(GameEngine.newGame(SampleGames.DAY_1).copy(demoMode = true), SampleGames.newborn)
        val demo = GameEngine.addDemoCoins(demoStart, GameRules.DEMO_COINS).game
        val json = StorageJson.encodeToString(GameSaveFile.serializer(), GameSaveFile(game = demo.toDto(), beforeDemo = game.toDto()))

        val restored = StorageJson.decodeFromString(GameSaveFile.serializer(), json)

        assertTrue("В файле нет причины demo_coins", json.contains("\"demo_coins\""))
        assertEquals(demo, restored.game?.toDomain())
        assertEquals(game, restored.beforeDemo?.toDomain())
    }

    @Test
    fun gameWithoutSaveIsEmptyFile() {
        val json = StorageJson.encodeToString(GameSaveFile.serializer(), GameSaveFile())

        assertEquals(null, StorageJson.decodeFromString(GameSaveFile.serializer(), json).game)
    }
}
