package ru.larpinovplay.finniapp

import ru.larpinovplay.finniapp.domain.game.model.TutorialStep
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
        // Игра посреди обучения: пропущенные шаги тоже сохраняются
        val game = SampleGames.rich().let { it.copy(state = it.state.copy(tutorial = true, tutorialSkipped = setOf(TutorialStep.TASKS))) }

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

    /** Игра ребёнка, отложенная на время демо, лежит в том же файле: её не трогают обычные записи и перезапуск. */
    @Test
    fun gameBeforeDemoSurvivesSavesAndRestartUntilDemoEnds() = runBlocking {
        val childsGame = SampleGames.rich()
        val demo = SampleGames.richer().let { it.copy(state = it.state.copy(demoMode = true)) }
        val store = openStore()

        store.save(demo, beforeDemo = childsGame)
        store.save(demo.copy(state = demo.state.copy(balance = 7)))   // обычный ход в демо

        val restarted = restart()
        assertEquals(Result.Success(childsGame), restarted.loadBeforeDemo())
        assertEquals(7, (restarted.load() as Result.Success).data?.state?.balance)

        restarted.save(childsGame, beforeDemo = null)   // выход из демо
        assertEquals(Result.Success(childsGame), restarted.load())
        assertEquals(Result.Success(null), restarted.loadBeforeDemo())
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

    /** Раньше питомец мог быть кроликом; теперь он всегда кот, а прогресс старого сохранения остаётся. */
    @Test
    fun oldBunnySaveLoadsAsCat() = runBlocking {
        val scope = newScope()
        val game = SampleGames.rich()
        DataStoreGameStore.create(scope, { file }, flaky).save(game)
        scope.coroutineContext[Job]!!.cancelAndJoin()
        file.writeText(file.readText().replaceFirst("\"pet\":{", "\"pet\":{\"species\":\"BUNNY\","))
        assertTrue(file.readText().contains("\"species\":\"BUNNY\""))

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
        file.writeText("{\"version\": 99, \"game\": {\"totallyNewShape\": true}}")

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

    // ---------- Причины сбоя: что лечится само, а что должен исправить пользователь ----------

    @Test
    fun noSpaceIsReportedAtOnceWithoutRetries() = runBlocking {
        val store = openStore()
        flaky.failWrites = true
        flaky.writeFailure = "write failed: ENOSPC (No space left on device)"

        assertEquals(Result.Error(StorageError.NO_SPACE), store.save(SampleGames.rich()))
        assertEquals(1, flaky.writeAttempts)   // повторять бесполезно: место само не появится
    }

    @Test
    fun noAccessIsReportedAtOnce() = runBlocking {
        val store = openStore()
        flaky.failWrites = true
        flaky.writeFailure = "open failed: EACCES (Permission denied)"

        assertEquals(Result.Error(StorageError.NO_ACCESS), store.save(SampleGames.rich()))
    }

    @Test
    fun unknownFailureIsRetriedBeforeGivingUp() = runBlocking {
        val store = openStore()
        flaky.failWrites = true

        assertEquals(Result.Error(StorageError.WRITE_FAILED), store.save(SampleGames.rich()))
        assertEquals(3, flaky.writeAttempts)   // первая попытка и два повтора
    }

    @Test
    fun missingFolderIsCreatedAgain() = runBlocking {
        val folder = File(tmp.root, "saves")
        val store = DataStoreGameStore.create(newScope(), { File(folder, "game.json") }, flaky)
        assertTrue(store.save(SampleGames.rich()) is Result.Success)
        folder.deleteRecursively()   // кто-то удалил папку сохранений

        assertTrue(store.save(SampleGames.richer()) is Result.Success)
    }
}
