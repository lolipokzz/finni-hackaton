package ru.larpinovplay.finniapp.data.game.store

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.storage.StorageError
import ru.larpinovplay.finniapp.domain.util.result.EmptyDataSuccess
import ru.larpinovplay.finniapp.domain.util.result.EmptyResult
import ru.larpinovplay.finniapp.domain.util.result.Result
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

/**
 * Игра в одном JSON-файле под управлением Jetpack DataStore: запись атомарна (временный файл и переименование),
 * очередь записей последовательная, при сбое прежний файл остаётся нетронутым.
 *
 * Как сбои становятся [StorageError]:
 * - `IOException` при чтении → [StorageError.READ_FAILED], при записи → [StorageError.WRITE_FAILED];
 * - повреждённый файл или чужая версия формата: обработчик заменяет файл пустым сохранением, а [load] один раз
 *   сообщает [StorageError.CORRUPTED] или [StorageError.INCOMPATIBLE_VERSION], иначе игрок молча потерял бы игру;
 * - всё остальное (баги, `CancellationException`) не перехватывается.
 *
 * На один файл в процессе допустим только один экземпляр DataStore, поэтому и хранилище создаётся один раз
 * (в Koin это `single`).
 */
class DataStoreGameStore private constructor(
    private val dataStore: DataStore<GameSaveFile>,
    private val recovery: AtomicReference<StorageError?>,
) : GameStore {

    override suspend fun load(): Result<GameSnapshot?, StorageError> = try {
        val file = dataStore.data.first()
        val recovered = recovery.getAndSet(null)
        if (recovered != null) Result.Error(recovered) else Result.Success(file.game?.toDomain())
    } catch (e: IOException) {
        Result.Error(StorageError.READ_FAILED)
    }

    override suspend fun save(snapshot: GameSnapshot): EmptyResult<StorageError> = try {
        dataStore.updateData { GameSaveFile(game = snapshot.toDto()) }
        // Сбой, замеченный при чтении перед этой записью, уже перезаписан: сообщать о нём поздно и не нужно
        recovery.set(null)
        EmptyDataSuccess
    } catch (e: IOException) {
        Result.Error(StorageError.WRITE_FAILED)
    }

    companion object {

        /**
         * @param scope область, в которой DataStore выполняет ввод-вывод; живёт столько же, сколько приложение
         * @param produceFile файл сохранения; в приложении это `context.dataStoreFile("game.json")`
         */
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
            return DataStoreGameStore(dataStore, recovery)
        }
    }
}
