package ru.larpinovplay.finniapp.domain.task.model

import kotlinx.serialization.Serializable

/** Итог ответа: верно ли, сколько монет, что сказать ребёнку. Сериализуем: едет в ключе маршрута итога. */
@Serializable
data class TaskOutcome(val success: Boolean, val reward: Int, val consequence: String?, val explanation: String)
