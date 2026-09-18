package ru.larpinovplay.finniapp.presentation.task

import androidx.compose.runtime.Composable
import ru.larpinovplay.finniapp.domain.content.FeedbackKey
import ru.larpinovplay.finniapp.domain.task.model.TaskTopic
import ru.larpinovplay.finniapp.presentation.feedback.LocalFeedback

/** Название темы для ребёнка. */
@Composable
fun TaskTopic.title(): String = LocalFeedback.current.text(
    when (this) {
        TaskTopic.BUDGET -> FeedbackKey.TOPIC_BUDGET
        TaskTopic.SAVINGS -> FeedbackKey.TOPIC_SAVINGS
        TaskTopic.PAYMENTS -> FeedbackKey.TOPIC_PAYMENTS
    }
)
