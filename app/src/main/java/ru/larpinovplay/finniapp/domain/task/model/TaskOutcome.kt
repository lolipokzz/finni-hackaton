package ru.larpinovplay.finniapp.domain.task.model

/** Итог ответа: верно ли, сколько монет, что сказать ребёнку. */
data class TaskOutcome(val success: Boolean, val reward: Int, val consequence: String?, val explanation: String)
