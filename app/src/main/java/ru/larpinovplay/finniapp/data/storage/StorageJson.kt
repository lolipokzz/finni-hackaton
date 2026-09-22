package ru.larpinovplay.finniapp.data.storage

import kotlinx.serialization.json.Json

/**
 * JSON для файлов сохранений. Неизвестные поля пропускаются (файл, записанный чуть более новой сборкой того же
 * формата, всё ещё читается), значения по умолчанию пишутся явно, чтобы файл не менялся вслед за кодом.
 */
internal val StorageJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
