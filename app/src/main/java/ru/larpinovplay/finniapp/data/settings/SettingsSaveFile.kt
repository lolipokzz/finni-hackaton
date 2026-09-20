package ru.larpinovplay.finniapp.data.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import ru.larpinovplay.finniapp.data.storage.StorageJson
import ru.larpinovplay.finniapp.domain.settings.model.AppSettings
import java.io.InputStream
import java.io.OutputStream

/**
 * Формат файла настроек. Версии нет: настройки простые, новое поле получает значение по умолчанию, лишние поля
 * пропускаются. Поэтому чужая версия для них не проблема, а повреждённый файл сбрасывается в значения по умолчанию.
 */
@Serializable
internal data class SettingsSaveFile(
    val soundEnabled: Boolean = true,
    val animationsEnabled: Boolean = true,
    val tipsEnabled: Boolean = true,
)

internal fun SettingsSaveFile.toDomain() = AppSettings(soundEnabled, animationsEnabled, tipsEnabled)

internal fun AppSettings.toDto() = SettingsSaveFile(soundEnabled, animationsEnabled, tipsEnabled)

internal object SettingsSaveSerializer : Serializer<SettingsSaveFile> {

    override val defaultValue: SettingsSaveFile = SettingsSaveFile()

    override suspend fun readFrom(input: InputStream): SettingsSaveFile {
        val text = withContext(Dispatchers.IO) { input.readBytes().decodeToString() }
        return try {
            StorageJson.decodeFromString(SettingsSaveFile.serializer(), text)
        } catch (e: IllegalArgumentException) {   // SerializationException — его подкласс
            throw CorruptionException("Настройки не читаются", e)
        }
    }

    override suspend fun writeTo(t: SettingsSaveFile, output: OutputStream) {
        withContext(Dispatchers.IO) {
            output.write(
                StorageJson.encodeToString(SettingsSaveFile.serializer(), t).encodeToByteArray()
            )
        }
    }
}
