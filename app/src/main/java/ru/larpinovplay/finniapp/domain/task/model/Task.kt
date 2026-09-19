package ru.larpinovplay.finniapp.domain.task.model

/** Финансовое задание (справочник контента, только чтение), ТЗ 2.5.8. */
data class Task(
    val id: String,
    val topic: TaskTopic,
    val title: String,
    val intro: String,
    val reward: Int,
    val rewardOnMistake: Int,
    val explanationSuccess: String,
    val explanationMistake: String,
    val hint: String,
    val payload: TaskPayload,
)
