package ru.larpinovplay.finniapp.domain.game.model

import ru.larpinovplay.finniapp.domain.adventure.model.Adventure
import ru.larpinovplay.finniapp.domain.game.engine.GameRules
import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem
import ru.larpinovplay.finniapp.domain.task.model.Task
import ru.larpinovplay.finniapp.domain.task.model.TaskTopic
import java.time.LocalDate

/**
 * Игровое состояние: кошелёк, журнал, покупки, копилка, задания, история недель.
 * Питомец сюда не входит: он лежит рядом, в [GameSnapshot]. Неизменяемо; менять его может только [GameEngine][ru.larpinovplay.finniapp.domain.game.engine.GameEngine].
 */
data class GameState(
    val demoMode: Boolean = false,
    val balance: Int = 0,
    val savings: Int = 0,
    val week: Int = 1,
    val phase: PeriodPhase = PeriodPhase.PLANNING,
    val plan: BudgetPlan? = null,                   // null, пока план недели не подтверждён
    val periodStartedOn: LocalDate? = null,         // день начала недели; null — старое сохранение, дня не знаем
    val ledger: List<LedgerEntry> = emptyList(),
    val purchases: List<ShopItem> = emptyList(),
    val goal: SavingsGoal? = null,
    val completedGoals: List<SavingsGoal> = emptyList(),
    val depositsThisWeek: List<Int> = emptyList(),
    val depositsByWeek: List<Int> = emptyList(),    // сколько за закрытую неделю отложено за вычетом снятого
    val withdrawalsThisWeek: List<Int> = emptyList(),
    val taskResults: List<TaskResult> = emptyList(),
    val tasksDoneThisWeek: Int = 0,
    val adventureResults: List<AdventureResult> = emptyList(),
    val history: List<WeekSummary> = emptyList(),
) {
    val foodCovered: Boolean get() = purchases.any { it.category == ShopCategory.MANDATORY }

    /** Сколько за эту неделю потрачено на товары [category]. */
    fun spentThisWeek(category: ShopCategory): Int = purchases.filter { it.category == category }.sumOf { it.price }

    /** Сколько по плану недели ещё осталось на [category]; меньше нуля — потрачено сверх плана, null — плана нет. */
    fun planLeft(category: ShopCategory): Int? = plan?.let { it[category.budgetDirection] - spentThisWeek(category) }

    /** Сколько за эту неделю отложено за вычетом снятого; может быть меньше нуля. Покупка цели сюда не входит. */
    val savedThisWeek: Int get() = depositsThisWeek.sum() - withdrawalsThisWeek.sum()

    val tasksPerWeek: Int get() = GameRules.TASKS_PER_WEEK

    /** Карманные (или стартовые) монеты, пришедшие в начале этой недели. */
    val weekIncome: Int
        get() = ledger.filter { it.week == week && (it.reason == LedgerReason.WeekIncome || it.reason == LedgerReason.StartCoins) }
            .sumOf { it.balanceDelta }

    /** Приключение этой недели уже пройдено. */
    val adventureDoneThisWeek: Boolean get() = adventureResults.any { it.week == week }

    /**
     * Приключение, которое ждёт на этой неделе: первое непройденное из [adventures] по порядку.
     * null — на этой неделе приключение уже пройдено или все пройдены. Непройденное не сгорает и ждёт дальше.
     */
    fun adventureOfWeek(adventures: List<Adventure>): Adventure? {
        if (adventureDoneThisWeek) return null
        val done = adventureResults.map { it.adventureId }.toSet()
        return adventures.firstOrNull { it.id !in done }
    }

    /**
     * Почему неделю сейчас нельзя закончить, или null, если можно. [today] — сегодняшняя дата устройства,
     * [adventures] — приключения из контента.
     */
    fun finishBlock(today: LocalDate, adventures: List<Adventure> = emptyList()): FinishBlock? = when {
        phase != PeriodPhase.ACTIVE -> FinishBlock.PLAN_NOT_CONFIRMED
        adventureOfWeek(adventures) != null -> FinishBlock.ADVENTURE_NOT_PLAYED
        // Не больше недели в день. Если часы перевели назад, не запираем игру: блокирует только тот же день
        !demoMode && today == periodStartedOn -> FinishBlock.SAME_DAY
        else -> null
    }

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
    fun weeksToGoal(): Int? = weeksToGoal(savings)

    /** Срок, если бы в копилке было [savingsIfAny]: так окно снятия показывает, насколько отодвинется цель. */
    fun weeksToGoal(savingsIfAny: Int): Int? {
        val g = goal ?: return null
        val remaining = g.cost - savingsIfAny
        if (remaining <= 0) return 0
        val recent = depositsByWeek.takeLast(3).filter { it > 0 }
        val avg = if (recent.isNotEmpty()) recent.average().toInt() else depositsThisWeek.sum()
        if (avg <= 0) return null
        return (remaining + avg - 1) / avg
    }
}
