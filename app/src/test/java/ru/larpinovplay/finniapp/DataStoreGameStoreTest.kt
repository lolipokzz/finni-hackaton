package ru.larpinovplay.finniapp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.larpinovplay.finniapp.data.game.store.DataStoreGameStore
import ru.larpinovplay.finniapp.domain.storage.StorageError
import ru.larpinovplay.finniapp.domain.util.result.Result
import java.io.File

/** DataStoreGameStore на настоящем файле: как сбои диска и плохих данных превращаются в [StorageError]. */
class DataStoreGameStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()
    private val flaky = FlakyGameSerializer()

    private val file: File get() = File(tmp.root, "game.json")

    private fun newScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scopes += it }

    private fun openStore(scope: CoroutineScope = newScope()) =
        DataStoreGameStore.create(scope, { file }, flaky)

    /** На один файл допустим один DataStore, поэтому «перезапуск» сначала останавливает прежний. */
    private suspend fun restart(): DataStoreGameStore {
        scopes.forEach { it.coroutineContext[Job]!!.cancelAndJoin() }
        return openStore()
    }

    @After
    fun stopScopes() = runBlocking {
        scopes.forEach { it.coroutineContext[Job]?.cancelAndJoin() }
    }

    @Test
    fun emptyStoreHasNoGame() = runBlocking {
        assertEquals(Result.Success(null), openStore().load())
    }

    @Test
    fun savedGameIsLoadedBack() = runBlocking {
        val store = openStore()
        val game = SampleGames.rich()

        assertTrue(store.save(game) is Result.Success)

        assertEquals(Result.Success(game), store.load())
    }

    @Test
    fun savedGameSurvivesRestart() = runBlocking {
        val scope = newScope()
        val game = SampleGames.rich()
        DataStoreGameStore.create(scope, { file }, flaky).save(game)

        val restarted = restart()

        assertEquals(Result.Success(game), restarted.load())
    }

    @Test
    fun lastSaveWins() = runBlocking {
        val store = openStore()
        store.save(SampleGames.rich())
        val newer = SampleGames.richer()

        store.save(newer)

        assertEquals(Result.Success(newer), store.load())
    }

    @Test
    fun unknownFieldsFromNewerBuildAreIgnored() = runBlocking {
        val scope = newScope()
        val game = SampleGames.rich()
        DataStoreGameStore.create(scope, { file }, flaky).save(game)
        scope.coroutineContext[Job]!!.cancelAndJoin()
        file.writeText(file.readText().replaceFirst("{", "{\"addedLater\":42,"))

        assertEquals(Result.Success(game), openStore().load())
    }

    // ---------- Повреждённые и чужие данные ----------

    @Test
    fun corruptedFileIsResetAndReportedOnce() = runBlocking {
        file.writeText("{ это не json")
        val store = openStore()

        assertEquals(Result.Error(StorageError.CORRUPTED), store.load())
        assertEquals(Result.Success(null), store.load())   // сброшено: во второй раз просто нет игры
    }

    @Test
    fun storeIsUsableAfterCorruptionIsHandled() = runBlocking {
        file.writeText(String(CharArray(3)))   // файл из нулевых байтов, как после сбоя питания
        val store = openStore()
        store.load()
        val game = SampleGames.rich()

        assertTrue(store.save(game) is Result.Success)

        assertEquals(Result.Success(game), store.load())
    }

    @Test
    fun validJsonWithoutVersionIsCorrupted() = runBlocking {
        file.writeText("{\"foo\": 1}")

        assertEquals(Result.Error(StorageError.CORRUPTED), openStore().load())
    }

    @Test
    fun incompatibleVersionIsReportedAndReset() = runBlocking {
        file.writeText("{\"version\": 999, \"game\": null}")
        val store = openStore()

        assertEquals(Result.Error(StorageError.INCOMPATIBLE_VERSION), store.load())
        assertEquals(Result.Success(null), store.load())
    }

    @Test
    fun incompatibleVersionIsNotMistakenForCorruptionEvenIfFormatChanged() = runBlocking {
        // Другая версия могла поменять поля так, что старый разбор не сработал бы: версия читается первой
        file.writeText("{\"version\": 2, \"game\": {\"totallyNewShape\": true}}")

        assertEquals(Result.Error(StorageError.INCOMPATIBLE_VERSION), openStore().load())
    }

    @Test
    fun saveBeforeLoadDoesNotLeaveStaleCorruptionError() = runBlocking {
        file.writeText("{ мусор")
        val store = openStore()
        val game = SampleGames.rich()

        assertTrue(store.save(game) is Result.Success)

        // Игра записана поверх повреждённого файла: сообщать о старой поломке уже нечего
        assertEquals(Result.Success(game), store.load())
    }

    // ---------- Сбои диска ----------

    @Test
    fun writeFailureIsReportedAndKeepsPreviousSave() = runBlocking {
        val scope = newScope()
        val store = DataStoreGameStore.create(scope, { file }, flaky)
        val first = SampleGames.rich()
        store.save(first)

        flaky.failWrites = true
        val result = store.save(SampleGames.richer())

        assertEquals(Result.Error(StorageError.WRITE_FAILED), result)
        assertEquals(Result.Success(first), store.load())
        flaky.failWrites = false
        assertEquals(Result.Success(first), restart().load())   // и на диске тоже прежняя игра
    }

    @Test
    fun savingWorksAgainOnceDiskRecovers() = runBlocking {
        val store = openStore()
        flaky.failWrites = true
        store.save(SampleGames.rich())
        flaky.failWrites = false
        val game = SampleGames.richer()

        assertTrue(store.save(game) is Result.Success)

        assertEquals(Result.Success(game), store.load())
    }

    @Test
    fun readFailureIsReportedAndKeepsTheSave() = runBlocking {
        val scope = newScope()
        val game = SampleGames.rich()
        DataStoreGameStore.create(scope, { file }, flaky).save(game)
        flaky.failReads = true

        val store = restart()

        assertEquals(Result.Error(StorageError.READ_FAILED), store.load())
        flaky.failReads = false
        assertEquals(Result.Success(game), restart().load())   // файл цел: сбой был временным
    }
}
