package ru.larpinovplay.finniapp.domain.game.model

import ru.larpinovplay.finniapp.domain.game.engine.GameRules
import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem
import ru.larpinovplay.finniapp.domain.task.model.Task
import ru.larpinovplay.finniapp.domain.task.model.TaskTopic

/**
 * Игровое состояние: кошелёк, журнал, покупки, копилка, задания, история недель.
 * Питомец сюда не входит: он лежит рядом, в [GameSnapshot]. Неизменяемо; менять его может только [GameEngine][ru.larpinovplay.finniapp.domain.game.engine.GameEngine].
 */
data class GameState(
    val balance: Int = 0,
    val savings: Int = 0,
    val week: Int = 1,
    val ledger: List<LedgerEntry> = emptyList(),
    val purchases: List<ShopItem> = emptyList(),
    val goal: SavingsGoal? = null,
    val completedGoals: List<SavingsGoal> = emptyList(),
    val depositsThisWeek: List<Int> = emptyList(),
    val depositsByWeek: List<Int> = emptyList(),    // сумма пополнений по закрытым неделям
    val taskResults: List<TaskResult> = emptyList(),
    val tasksDoneThisWeek: Int = 0,
    val history: List<WeekSummary> = emptyList(),
) {
    val foodCovered: Boolean get() = purchases.any { it.category == ShopCategory.MANDATORY }

    val tasksPerWeek: Int get() = GameRules.TASKS_PER_WEEK

    fun taskStatus(task: Task): TaskStatus {
        val last = taskResults.lastOrNull { it.taskId == task.id }
        return when {
            last?.success == true -> TaskStatus.DONE
            last != null && last.week == week -> TaskStatus.RETRY_NEXT_WEEK
            tasksDoneThisWeek >= GameRules.TASKS_PER_WEEK -> TaskStatus.LIMIT_REACHED
            else -> TaskStatus.AVAILABLE
        }
    }

    fun availableTasks(tasks: List<Task>): List<Task> = tasks.filter { taskStatus(it) == TaskStatus.AVAILABLE }

    fun topicProgress(tasks: List<Task>): List<TopicProgress> =
        TaskTopic.entries.map { topic ->
            val ofTopic = tasks.filter { it.topic == topic }
            TopicProgress(topic, total = ofTopic.size, done = ofTopic.count { taskStatus(it) == TaskStatus.DONE })
        }

    /** Срок в неделях по среднему пополнению за последние 3 закрытые недели, иначе по текущей. */
    fun weeksToGoal(): Int? {
        val g = goal ?: return null
        val remaining = g.cost - savings
        if (remaining <= 0) return 0
        val recent = depositsByWeek.takeLast(3).filter { it > 0 }
        val avg = if (recent.isNotEmpty()) recent.average().toInt() else depositsThisWeek.sum()
        if (avg <= 0) return null
        return (remaining + avg - 1) / avg
    }
}
