package ru.larpinovplay.finniapp.domain.task.model

/** Итог ответа на упражнение: верно ли, что из-за этого случилось, как это объяснить ребёнку. */
data class TaskOutcome(val success: Boolean, val consequence: String?, val explanation: String)
