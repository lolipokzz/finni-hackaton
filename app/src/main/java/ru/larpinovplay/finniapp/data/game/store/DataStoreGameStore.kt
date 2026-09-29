package ru.larpinovplay.finniapp.data.game.store

import ru.larpinovplay.finniapp.domain.util.result.map
import ru.larpinovplay.finniapp.data.storage.onDisk
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.storage.StorageError
import ru.larpinovplay.finniapp.domain.util.result.EmptyResult
import ru.larpinovplay.finniapp.domain.util.result.Result
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * Игра в одном JSON-файле под управлением Jetpack DataStore: запись атомарна (временный файл и переименование), очередь
 * записей последовательная, при сбое прежний файл остаётся нетронутым.
 */
class DataStoreGameStore private constructor(
    private val dataStore: DataStore<GameSaveFile>,
    private val recovery: AtomicReference<StorageError?>,
    private val file: () -> File,
) : GameStore {

    override suspend fun load(): Result<GameSnapshot?, StorageError> =
        when (val read = onDisk(file, StorageError.READ_FAILED) { dataStore.data.first() }) {
            is Result.Error -> read
            is Result.Success -> recovery.getAndSet(null)?.let { Result.Error(it) } ?: Result.Success(read.data.game?.toDomain())
        }

    override suspend fun loadBeforeDemo(): Result<GameSnapshot?, StorageError> =
        onDisk(file, StorageError.READ_FAILED) { dataStore.data.first() }.map { it.beforeDemo?.toDomain() }

    // Сбой, замеченный при чтении перед записью, уже перезаписан: сообщать о нём поздно и не нужно
    override suspend fun save(snapshot: GameSnapshot): EmptyResult<StorageError> =
        onDisk(file, StorageError.WRITE_FAILED) { dataStore.updateData { it.copy(game = snapshot.toDto()) } }
            .map { recovery.set(null) }

    override suspend fun save(snapshot: GameSnapshot, beforeDemo: GameSnapshot?): EmptyResult<StorageError> =
        onDisk(file, StorageError.WRITE_FAILED) {
            dataStore.updateData { GameSaveFile(game = snapshot.toDto(), beforeDemo = beforeDemo?.toDto()) }
        }.map { recovery.set(null) }

    override suspend fun clear(): EmptyResult<StorageError> =
        onDisk(file, StorageError.WRITE_FAILED) { dataStore.updateData { GameSaveFile() } }
            .map { recovery.set(null) }

    companion object {

        fun create(scope: CoroutineScope, produceFile: () -> File): DataStoreGameStore =
            create(scope, produceFile, GameSaveSerializer)

        /** Отдельная точка для тестов: сериализатор, который умеет падать. */
        internal fun create(
            scope: CoroutineScope,
            produceFile: () -> File,
            serializer: Serializer<GameSaveFile>,
        ): DataStoreGameStore {
            val recovery = AtomicReference<StorageError?>(null)
            val dataStore = DataStoreFactory.create(
                serializer = serializer,
                corruptionHandler = ReplaceFileCorruptionHandler { exception ->
                    recovery.set(
                        if (exception.cause is IncompatibleSaveVersion) StorageError.INCOMPATIBLE_VERSION
                        else StorageError.CORRUPTED
                    )
                    GameSaveFile()
                },
                scope = scope,
                produceFile = produceFile,
            )
            return DataStoreGameStore(dataStore, recovery, produceFile)
        }
    }
}
