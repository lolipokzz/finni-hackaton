package ru.larpinovplay.finniapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.larpinovplay.finniapp.data.game.store.GameSaveFile
import ru.larpinovplay.finniapp.data.game.store.toDomain
import ru.larpinovplay.finniapp.data.game.store.toDto
import ru.larpinovplay.finniapp.data.storage.StorageJson
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
                LedgerReason.GoalReached::class, LedgerReason.TaskReward::class, LedgerReason.WeekIncome::class,
            ),
            reasons,
        )
        assertTrue(game.state.purchases.isNotEmpty())
        assertTrue(game.state.completedGoals.isNotEmpty())
        assertTrue(game.state.taskResults.isNotEmpty())
        assertTrue(game.state.history.isNotEmpty())
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

        listOf("start_coins", "purchase", "deposit", "goal_reached", "task_reward", "week_income").forEach { name ->
            assertTrue("В файле нет причины $name", json.contains("\"$name\""))
        }
    }

    @Test
    fun gameWithoutSaveIsEmptyFile() {
        val json = StorageJson.encodeToString(GameSaveFile.serializer(), GameSaveFile())

        assertEquals(null, StorageJson.decodeFromString(GameSaveFile.serializer(), json).game)
    }
}
