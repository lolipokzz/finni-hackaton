package ru.larpinovplay.finniapp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.larpinovplay.finniapp.data.settings.DataStoreSettingsRepository
import ru.larpinovplay.finniapp.domain.settings.model.AppSettings
import ru.larpinovplay.finniapp.domain.storage.StorageError
import ru.larpinovplay.finniapp.domain.util.result.EmptyDataSuccess
import ru.larpinovplay.finniapp.domain.util.result.Result
import java.io.File

/** DataStoreSettingsRepository на настоящем файле. */
class DataStoreSettingsRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()
    private val flaky = FlakySettingsSerializer()

    private val file: File get() = File(tmp.root, "settings.json")

    private fun newScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scopes += it }

    private fun open(scope: CoroutineScope = newScope()) = DataStoreSettingsRepository.create(scope, { file }, flaky)

    private suspend fun restart(): DataStoreSettingsRepository {
        scopes.forEach { it.coroutineContext[Job]!!.cancelAndJoin() }
        return open()
    }

    @After
    fun stopScopes() = runBlocking {
        scopes.forEach { it.coroutineContext[Job]?.cancelAndJoin() }
    }

    @Test
    fun startsWithDefaults() = runBlocking {
        assertEquals(AppSettings(), open().observeSettings().first())
    }

    @Test
    fun updateAppliesTransformAndKeepsOtherFields() = runBlocking {
        val settings = open()

        val result = settings.updateSettings { it.copy(animationsEnabled = false) }

        assertEquals(EmptyDataSuccess, result)
        assertEquals(AppSettings(animationsEnabled = false), settings.observeSettings().first())
    }

    @Test
    fun updatedSettingsSurviveRestart() = runBlocking {
        val scope = newScope()
        open(scope).updateSettings { it.copy(soundEnabled = false, tipsEnabled = false) }

        val restarted = restart()

        assertEquals(AppSettings(soundEnabled = false, tipsEnabled = false), restarted.observeSettings().first())
    }

    @Test
    fun corruptedFileFallsBackToDefaultsAndStaysUsable() = runBlocking {
        file.writeText("{ мусор")
        val settings = open()

        assertEquals(AppSettings(), settings.observeSettings().first())
        settings.updateSettings { it.copy(soundEnabled = false) }
        assertEquals(AppSettings(soundEnabled = false), settings.observeSettings().first())
    }

    @Test
    fun missingAndUnknownFieldsAreTolerated() = runBlocking {
        // Файл от другой сборки: не хватает поля, есть лишнее
        file.writeText("{\"soundEnabled\": false, \"someFutureFlag\": true}")

        assertEquals(AppSettings(soundEnabled = false), open().observeSettings().first())
    }

    @Test
    fun writeFailureIsReportedAndSettingsStayTheSame() = runBlocking {
        val settings = open()
        settings.updateSettings { it.copy(animationsEnabled = false) }
        flaky.failWrites = true

        val result = settings.updateSettings { it.copy(soundEnabled = false) }

        assertEquals(Result.Error(StorageError.WRITE_FAILED), result)
        assertEquals(AppSettings(animationsEnabled = false), settings.observeSettings().first())
    }

    @Test
    fun unreadableFileGivesDefaultsInsteadOfFailing() = runBlocking {
        val scope = newScope()
        open(scope).updateSettings { it.copy(soundEnabled = false) }
        flaky.failReads = true

        val restarted = restart()

        assertEquals(AppSettings(), restarted.observeSettings().first())
    }
}
