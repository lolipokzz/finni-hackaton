package ru.larpinovplay.finniapp.presentation.screens.tasks

import kotlinx.serialization.Serializable
import ru.larpinovplay.finniapp.domain.task.model.TaskOutcome

/**
 * Итог задания для окна результата. Едет в ключе маршрута TaskResult, поэтому сериализуемый:
 * доменная [TaskOutcome] о формате навигации не знает.
 */
@Serializable
data class TaskOutcomeUi(val success: Boolean, val reward: Int, val consequence: String?, val explanation: String)

fun TaskOutcome.toUi() = TaskOutcomeUi(success, reward, consequence, explanation)
