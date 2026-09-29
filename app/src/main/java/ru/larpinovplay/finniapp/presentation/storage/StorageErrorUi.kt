package ru.larpinovplay.finniapp.presentation.storage

import androidx.compose.runtime.Composable
import ru.larpinovplay.finniapp.domain.content.FeedbackKey
import ru.larpinovplay.finniapp.domain.storage.StorageError
import ru.larpinovplay.finniapp.domain.util.result.Result
import ru.larpinovplay.finniapp.presentation.events.SnackbarAction
import ru.larpinovplay.finniapp.presentation.events.SnackbarController
import ru.larpinovplay.finniapp.presentation.events.SnackbarEvent
import ru.larpinovplay.finniapp.presentation.feedback.LocalFeedback

/** Что сказать о сбое сохранения. Домен отдаёт только причину, слова берутся из контента. */
private val StorageError.feedbackKey: FeedbackKey
    get() = when (this) {
        StorageError.READ_FAILED -> FeedbackKey.STORAGE_READ_FAILED
        StorageError.WRITE_FAILED -> FeedbackKey.STORAGE_WRITE_FAILED
        StorageError.CORRUPTED -> FeedbackKey.STORAGE_CORRUPTED
        StorageError.INCOMPATIBLE_VERSION -> FeedbackKey.STORAGE_INCOMPATIBLE
        StorageError.NO_SPACE -> FeedbackKey.STORAGE_NO_SPACE
        StorageError.NO_ACCESS -> FeedbackKey.STORAGE_NO_ACCESS
    }

@Composable
fun StorageError.text(): String = LocalFeedback.current.text(feedbackKey)

/** Сбой, который пользователь может исправить сам: освободить память, вернуть доступ к ней. */
val StorageError.userCanFix: Boolean
    get() = this == StorageError.NO_SPACE || this == StorageError.NO_ACCESS

/**
 * Сообщение об ошибке — только если пользователь может её исправить. [retry] — что сделать по «Повторить»
 * (например, после того как освободили память); null — кнопки нет.
 */
suspend fun StorageError.snackbar(retry: (suspend () -> Unit)?) {
    if (!userCanFix) return
    SnackbarController.sendEvent(SnackbarEvent(feedbackKey, retry?.let { SnackbarAction("Повторить", it) }))
}

/** Данные команды; при ошибке хранения — сообщение с «Повторить» ([retry]), если её можно исправить, и null. */
suspend fun <D> Result<D, StorageError>.orSnackbar(retry: suspend () -> Unit): D? = when (this) {
    is Result.Success -> data
    is Result.Error -> {
        error.snackbar(retry)
        null
    }
}
