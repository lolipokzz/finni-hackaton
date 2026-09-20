package ru.larpinovplay.finniapp.data.game.store

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.larpinovplay.finniapp.data.storage.StorageJson
import java.io.InputStream
import java.io.OutputStream

/** Сохранение записано другой версией формата; лежит в `cause` у [CorruptionException]. */
internal class IncompatibleSaveVersion(val found: Int) : Exception("Версия сохранения $found, ожидается $GAME_SAVE_VERSION")

/**
 * Читает и пишет [GameSaveFile] как JSON. Любой файл, который не удаётся разобрать, это
 * [CorruptionException]: DataStore передаёт её обработчику повреждений, а не роняет приложение.
 * Ошибки ввода-вывода (`IOException`) сюда не оборачиваются, они означают сбой диска, а не плохие данные.
 */
internal object GameSaveSerializer : Serializer<GameSaveFile> {

    override val defaultValue: GameSaveFile = GameSaveFile()

    override suspend fun readFrom(input: InputStream): GameSaveFile {
        val text = input.readBytes().decodeToString()
        // Версия читается отдельно и первой: иначе смена формата выглядела бы как повреждение файла
        val version = decode<GameSaveVersion>(text).version
        if (version != GAME_SAVE_VERSION) {
            throw CorruptionException("Несовместимая версия сохранения: $version", IncompatibleSaveVersion(version))
        }
        return decode(text)
    }

    override suspend fun writeTo(t: GameSaveFile, output: OutputStream) {
        withContext(Dispatchers.IO) {
            output.write(
                StorageJson.encodeToString(GameSaveFile.serializer(), t).encodeToByteArray()
            )
        }
    }

    private inline fun <reified T> decode(text: String): T = try {
        StorageJson.decodeFromString<T>(text)
    } catch (e: IllegalArgumentException) {   // SerializationException — его подкласс
        throw CorruptionException("Сохранение не читается", e)
    }
}
