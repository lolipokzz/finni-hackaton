package ru.larpinovplay.finniapp.data.settings

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import ru.larpinovplay.finniapp.domain.settings.model.AppSettings
import ru.larpinovplay.finniapp.domain.settings.repository.SettingsRepository
import ru.larpinovplay.finniapp.domain.storage.StorageError
import ru.larpinovplay.finniapp.domain.util.result.EmptyDataSuccess
import ru.larpinovplay.finniapp.domain.util.result.EmptyResult
import ru.larpinovplay.finniapp.domain.util.result.Result
import java.io.File
import java.io.IOException

/**
 * Настройки в отдельном файле DataStore, не в файле игры: сброс игры при смене формата не должен трогать
 * звук и подсказки.
 *
 * Сбои читаются мягко: настройки не стоят прерванной игры, поэтому повреждённый файл заменяется значениями по
 * умолчанию, а неудачное чтение после двух повторов даёт те же значения. Сбой записи возвращается как
 * [StorageError.WRITE_FAILED]: экран может сказать, что настройка не сохранилась.
 *
 * Экран записи не ждёт: новая настройка видна в [observeSettings] сразу, пока DataStore пишет файл на IO.
 * Не записалась — переключатель возвращается к тому, что на диске.
 */
class DataStoreSettingsRepository internal constructor(
    private val dataStore: DataStore<SettingsSaveFile>,
) : SettingsRepository {

    private val saved: Flow<AppSettings> = dataStore.data
        .retry(READ_RETRIES) { it is IOException }
        .catch { if (it is IOException) emit(SettingsSaveFile()) else throw it }
        .map { it.toDomain() }

    /** Настройка, которую экран уже показывает, а диск ещё пишет; null — диск догнал. */
    private val shown = MutableStateFlow<AppSettings?>(null)

    override fun observeSettings(): Flow<AppSettings> =
        combine(saved, shown) { onDisk, unsaved -> unsaved ?: onDisk }.distinctUntilChanged()

    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings): EmptyResult<StorageError> {
        val next = transform(shown.value ?: saved.first())
        shown.value = next
        return try {
            dataStore.updateData { transform(it.toDomain()).toDto() }
            EmptyDataSuccess
        } catch (e: IOException) {
            Result.Error(StorageError.WRITE_FAILED)
        } finally {
            // Показанное больше не нужно: либо оно уже на диске, либо не записалось. Более новое не трогаем
            shown.compareAndSet(next, null)
        }
    }

    companion object {

        private const val READ_RETRIES = 2L

        /**
         * @param scope область, в которой DataStore выполняет ввод-вывод; живёт столько же, сколько приложение
         * @param produceFile файл настроек; в приложении это `context.dataStoreFile("settings.json")`
         */
        fun create(scope: CoroutineScope, produceFile: () -> File): DataStoreSettingsRepository =
            create(scope, produceFile, SettingsSaveSerializer)

        /** Отдельная точка для тестов: сериализатор, который умеет падать. */
        internal fun create(
            scope: CoroutineScope,
            produceFile: () -> File,
            serializer: Serializer<SettingsSaveFile>,
        ): DataStoreSettingsRepository = DataStoreSettingsRepository(
            DataStoreFactory.create(
                serializer = serializer,
                corruptionHandler = ReplaceFileCorruptionHandler { SettingsSaveFile() },
                scope = scope,
                produceFile = produceFile,
            )
        )
    }
}
