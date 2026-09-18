package ru.larpinovplay.finniapp.domain.task.model

/** Ответ ребёнка на задание; вид соответствует [TaskPayload]. */
sealed interface TaskAnswer {
    data class Choice(val optionId: String) : TaskAnswer
    data class Allocation(val amounts: Map<String, Int>) : TaskAnswer
    data class Selection(val itemIds: Set<String>) : TaskAnswer
}
