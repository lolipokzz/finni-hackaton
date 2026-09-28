package ru.larpinovplay.finniapp.data.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import ru.larpinovplay.finniapp.domain.storage.StorageError
import ru.larpinovplay.finniapp.domain.util.result.Result
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/** Сколько раз повторить непонятный сбой ввода-вывода и сколько ждать между попытками. */
private const val RETRIES = 2
private const val RETRY_PAUSE_MS = 100L

/**
 * Чтение или запись файла хранения на Dispatchers.IO. Со сбоем сначала пытаемся справиться сами:
 * - пропала папка файла — создаём её и пробуем снова (сам файл DataStore создаёт при записи);
 * - непонятный сбой ввода-вывода — ещё [RETRIES] попытки с короткой паузой: такие бывают разовыми.
 *
 * Не повторяем то, что само не пройдёт, и сразу отдаём причину, которую может исправить пользователь:
 * нет места — [StorageError.NO_SPACE], нет доступа к памяти (права, память только для чтения) —
 * [StorageError.NO_ACCESS]. Не помогли и повторы — [failure] (READ_FAILED или WRITE_FAILED).
 */
internal suspend fun <T> onDisk(file: () -> File, failure: StorageError, block: suspend () -> T): Result<T, StorageError> =
    withContext(Dispatchers.IO) {
        repeat(RETRIES + 1) { attempt ->
            try {
                return@withContext Result.Success(block())
            } catch (e: IOException) {
                e.userFixable()?.let { return@withContext Result.Error(it) }
                if (e.isMissingPath()) file().parentFile?.mkdirs()
            } catch (e: SecurityException) {
                return@withContext Result.Error(StorageError.NO_ACCESS)
            }
            if (attempt < RETRIES) delay(RETRY_PAUSE_MS)
        }
        Result.Error(failure)
    }

/**
 * Причина, которую может исправить пользователь, по тексту ошибки. Android пишет в него код errno
 * (например, «ENOSPC (No space left on device)»), в том числе у вложенных причин.
 */
internal fun IOException.userFixable(): StorageError? {
    val text = allMessages()
    return when {
        NO_SPACE.any { it in text } -> StorageError.NO_SPACE
        NO_ACCESS.any { it in text } -> StorageError.NO_ACCESS
        else -> null
    }
}

private val NO_SPACE = listOf("ENOSPC", "EDQUOT", "No space left")
private val NO_ACCESS = listOf("EACCES", "EPERM", "EROFS", "Permission denied", "Read-only file system")

private fun IOException.isMissingPath() = this is FileNotFoundException || "ENOENT" in allMessages()

private fun Throwable.allMessages(): String = generateSequence(this) { it.cause }.joinToString(" ") { it.message.orEmpty() }
