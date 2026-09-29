package ru.larpinovplay.finniapp.data.game.store

import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.storage.StorageError
import ru.larpinovplay.finniapp.domain.util.result.EmptyResult
import ru.larpinovplay.finniapp.domain.util.result.Result
import ru.larpinovplay.finniapp.data.game.GameRepositoryImpl

/**
 * Где лежит сохранённая игра. Знает только, как прочитать и записать снимок целиком; правил игры и порядка команд не
 * знает, это дело [GameRepositoryImpl].
 */
interface GameStore {

    /**
     * Сохранённая игра. Success(null) — сохранения нет.
     * Повреждённое или чужой версии сохранение сбрасывается, а вместо данных приходит
     * [StorageError.CORRUPTED] или [StorageError.INCOMPATIBLE_VERSION]: следующая загрузка вернёт Success(null).
     */
    suspend fun load(): Result<GameSnapshot?, StorageError>

    /** Игра, отложенная на время демо-режима; Success(null) — её нет. Сбои сохранения сообщает [load], а не она. */
    suspend fun loadBeforeDemo(): Result<GameSnapshot?, StorageError>

    /**
     * Записывает [snapshot] вместо прежней текущей игры. Отложенная на время демо игра остаётся как была.
     * При ошибке прежнее сохранение остаётся.
     */
    suspend fun save(snapshot: GameSnapshot): EmptyResult<StorageError>

    /**
     * Записывает одной записью текущую игру [snapshot] и отложенную на время демо [beforeDemo] (null — её больше нет).
     * При ошибке прежнее сохранение остаётся целиком.
     */
    suspend fun save(snapshot: GameSnapshot, beforeDemo: GameSnapshot?): EmptyResult<StorageError>

    /** Стирает сохранение вместе с отложенной игрой: следующая [load] вернёт Success(null). При ошибке всё остаётся. */
    suspend fun clear(): EmptyResult<StorageError>
}
