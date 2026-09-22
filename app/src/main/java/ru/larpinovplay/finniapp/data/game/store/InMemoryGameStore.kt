package ru.larpinovplay.finniapp.data.game.store

import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.storage.StorageError
import ru.larpinovplay.finniapp.domain.util.result.EmptyDataSuccess
import ru.larpinovplay.finniapp.domain.util.result.EmptyResult
import ru.larpinovplay.finniapp.domain.util.result.Result

/** Хранит игру в памяти процесса: не переживает перезапуск, зато никогда не даёт сбоев. */
class InMemoryGameStore(@Volatile private var saved: GameSnapshot? = null) : GameStore {

    override suspend fun load(): Result<GameSnapshot?, StorageError> = Result.Success(saved)

    override suspend fun save(snapshot: GameSnapshot): EmptyResult<StorageError> {
        saved = snapshot
        return EmptyDataSuccess
    }

    override suspend fun clear(): EmptyResult<StorageError> {
        saved = null
        return EmptyDataSuccess
    }
}
