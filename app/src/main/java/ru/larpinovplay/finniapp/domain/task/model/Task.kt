package ru.larpinovplay.finniapp.domain.task.model

/**
 * Одно упражнение уровня (справочник контента, только чтение), ТЗ 2.5.8: вопрос или игровая ситуация,
 * виджет по [payload] и объяснение после ответа — и верного, и ошибочного.
 */
data class Task(
    val id: String,
    val intro: String,
    val explanationSuccess: String,
    val explanationMistake: String,
    val hint: String,
    val payload: TaskPayload,
)
