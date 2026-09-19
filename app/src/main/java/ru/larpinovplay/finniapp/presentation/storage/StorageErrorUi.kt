package ru.larpinovplay.finniapp.presentation.storage

import androidx.compose.runtime.Composable
import ru.larpinovplay.finniapp.domain.content.FeedbackKey
import ru.larpinovplay.finniapp.domain.storage.StorageError
import ru.larpinovplay.finniapp.presentation.feedback.LocalFeedback

/** Что сказать ребёнку о сбое сохранения. Домен отдаёт только причину, слова берутся из контента. */
@Composable
fun StorageError.text(): String = LocalFeedback.current.text(
    when (this) {
        StorageError.READ_FAILED -> FeedbackKey.STORAGE_READ_FAILED
        StorageError.WRITE_FAILED -> FeedbackKey.STORAGE_WRITE_FAILED
        StorageError.CORRUPTED -> FeedbackKey.STORAGE_CORRUPTED
        StorageError.INCOMPATIBLE_VERSION -> FeedbackKey.STORAGE_INCOMPATIBLE
    }
)
