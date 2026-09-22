package ru.larpinovplay.finniapp.data.game.store

import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.storage.StorageError
import ru.larpinovplay.finniapp.domain.util.result.EmptyResult
import ru.larpinovplay.finniapp.domain.util.result.Result

/**
 * Где лежит сохранённая игра. Знает только, как прочитать и записать снимок целиком; правил игры и порядка
 * команд не знает, это дело [ru.larpinovplay.finniapp.data.game.GameRepositoryImpl]. Отделено, чтобы репозиторий
 * проверялся без диска, а сбои записи подделывались в тесте. Исключения библиотеки хранения наружу не выходят:
 * все ожидаемые сбои приходят как [StorageError].
 */
interface GameStore {

    /**
     * Сохранённая игра. Success(null) — сохранения нет.
     * Повреждённое или чужой версии сохранение сбрасывается, а вместо данных приходит
     * [StorageError.CORRUPTED] или [StorageError.INCOMPATIBLE_VERSION]: следующая загрузка вернёт Success(null).
     */
    suspend fun load(): Result<GameSnapshot?, StorageError>

    /** Записывает [snapshot] вместо прежнего сохранения целиком. При ошибке прежнее сохранение остаётся. */
    suspend fun save(snapshot: GameSnapshot): EmptyResult<StorageError>

    /** Стирает сохранение: следующая [load] вернёт Success(null). При ошибке прежнее сохранение остаётся. */
    suspend fun clear(): EmptyResult<StorageError>
}
