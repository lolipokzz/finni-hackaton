package ru.larpinovplay.finniapp.presentation.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.larpinovplay.finniapp.domain.pet.model.PetGrowthStage
import ru.larpinovplay.finniapp.presentation.screens.savings.SavingsGoal
import ru.larpinovplay.finniapp.presentation.screens.shop.ShopCategory
import ru.larpinovplay.finniapp.presentation.screens.shop.ShopItem
import ru.larpinovplay.finniapp.presentation.screens.tasks.Task
import ru.larpinovplay.finniapp.presentation.screens.tasks.TaskAnswer
import ru.larpinovplay.finniapp.presentation.screens.tasks.TaskOutcome
import ru.larpinovplay.finniapp.presentation.screens.tasks.TaskTopic as ContentTopic
import ru.larpinovplay.finniapp.presentation.screens.tasks.evaluate
import ru.larpinovplay.finniapp.presentation.screens.tasks.tasks

/**
 * Временная замена игрового движка для вёрстки и демонстрации экранов.
 * Держит монеты, показатели питомца, покупки, копилку, журнал и историю недель в памяти.
 * Правила — из docs/04-rules-and-formulas.md, с поправкой на то, что план бюджета убран:
 * неделя оценивается по двум критериям (еда куплена, что-то отложено), максимум 2 очка.
 * Заменяется на GameRepository + GameEngine без изменения экранов.
 */
class SampleGame(startBalance: Int = 100, satiety: Int = 70, mood: Int = 50) {

    // ---------- Кошелёк и питомец ----------

    var balance by mutableIntStateOf(0)
        private set
    var savings by mutableIntStateOf(0)
        private set
    var satiety by mutableIntStateOf(satiety)
        private set
    var mood by mutableIntStateOf(mood)
        private set

    var week by mutableIntStateOf(1)
        private set
    var growthPoints by mutableIntStateOf(0)
        private set
    val stage: PetGrowthStage
        get() = when {
            growthPoints >= STAGE_ADULT -> PetGrowthStage.ADULT
            growthPoints >= STAGE_TEEN -> PetGrowthStage.TEEN
            else -> PetGrowthStage.BABY
        }
    /** Сколько очков до следующей стадии; null — стадия последняя. */
    val pointsToNextStage: Int?
        get() = when (stage) {
            PetGrowthStage.BABY -> STAGE_TEEN - growthPoints
            PetGrowthStage.TEEN -> STAGE_ADULT - growthPoints
            PetGrowthStage.ADULT -> null
        }

    // ---------- Журнал: баланс не меняется без записи (ТЗ 2.5.4) ----------

    data class LedgerEntry(val week: Int, val title: String, val balanceDelta: Int, val savingsDelta: Int = 0)

    val ledger = mutableStateListOf<LedgerEntry>()

    private fun post(title: String, balanceDelta: Int, savingsDelta: Int = 0) {
        balance += balanceDelta
        savings += savingsDelta
        ledger += LedgerEntry(week, title, balanceDelta, savingsDelta)
    }

    init {
        post("Стартовые монеты", +startBalance)
    }

    // ---------- Покупки ----------

    val purchases = mutableStateListOf<ShopItem>()

    val foodCovered: Boolean get() = purchases.any { it.category == ShopCategory.MANDATORY }

    sealed interface PurchaseResult {
        data class Success(val item: ShopItem, val balanceAfter: Int) : PurchaseResult
        data class NotEnough(val missing: Int) : PurchaseResult
    }

    fun buy(item: ShopItem): PurchaseResult {
        if (item.price > balance) return PurchaseResult.NotEnough(item.price - balance)
        post(item.name, -item.price)
        satiety = (satiety + item.satiety).coerceIn(0, 100)
        mood = (mood + item.mood).coerceIn(0, 100)
        purchases += item
        return PurchaseResult.Success(item, balance)
    }

    // ---------- Копилка и цель ----------

    var goal by mutableStateOf<SavingsGoal?>(null)
        private set
    val completedGoals = mutableStateListOf<SavingsGoal>()
    val depositsThisWeek = mutableStateListOf<Int>()
    private val depositsByWeek = mutableListOf<Int>()   // сумма пополнений по закрытым неделям

    fun chooseGoal(goal: SavingsGoal) {
        this.goal = goal
    }

    fun deposit(amount: Int): Boolean {
        if (amount <= 0 || amount > balance) return false
        post("В копилку", -amount, +amount)
        depositsThisWeek += amount
        return true
    }

    fun reachGoal(): SavingsGoal? {
        val g = goal ?: return null
        if (savings < g.cost) return null
        post("Цель: ${g.name}", 0, -g.cost)
        mood = (mood + 30).coerceIn(0, 100)
        completedGoals += g
        goal = null
        return g
    }

    /** Срок в неделях по среднему пополнению за последние 3 закрытые недели, иначе по текущей. */
    fun weeksToGoal(): Int? {
        val g = goal ?: return null
        val remaining = g.cost - savings
        if (remaining <= 0) return 0
        val history = depositsByWeek.takeLast(3).filter { it > 0 }
        val avg = if (history.isNotEmpty()) history.average().toInt() else depositsThisWeek.sum()
        if (avg <= 0) return null
        return (remaining + avg - 1) / avg
    }

    // ---------- Задания (ТЗ 2.5.8) ----------

    data class TaskResult(val taskId: String, val week: Int, val success: Boolean, val reward: Int)

    enum class TaskStatus { AVAILABLE, DONE, RETRY_NEXT_WEEK, LIMIT_REACHED }

    val taskResults = mutableStateListOf<TaskResult>()
    var tasksDoneThisWeek by mutableIntStateOf(0)
        private set

    fun taskStatus(task: Task): TaskStatus {
        val last = taskResults.lastOrNull { it.taskId == task.id }
        return when {
            last?.success == true -> TaskStatus.DONE
            last != null && last.week == week -> TaskStatus.RETRY_NEXT_WEEK
            tasksDoneThisWeek >= TASKS_PER_WEEK -> TaskStatus.LIMIT_REACHED
            else -> TaskStatus.AVAILABLE
        }
    }

    val availableTasks: List<Task> get() = tasks.filter { taskStatus(it) == TaskStatus.AVAILABLE }

    /** Ответ на задание: оценка, награда в журнал, объяснение. null — задание сейчас недоступно. */
    fun answerTask(task: Task, answer: TaskAnswer): TaskOutcome? {
        if (taskStatus(task) != TaskStatus.AVAILABLE) return null
        val outcome = task.evaluate(answer)
        post("Задание: ${task.title}", +outcome.reward)
        taskResults += TaskResult(task.id, week, outcome.success, outcome.reward)
        tasksDoneThisWeek += 1
        return outcome
    }

    data class TaskTopic(val title: String, val total: Int, val done: Int)

    val taskTopics: List<TaskTopic>
        get() = ContentTopic.entries.map { topic ->
            val ofTopic = tasks.filter { it.topic == topic }
            TaskTopic(topic.title, ofTopic.size, ofTopic.count { taskStatus(it) == TaskStatus.DONE })
        }

    // ---------- Неделя ----------

    data class WeekSummary(
        val week: Int,
        val foodCovered: Boolean,          // критерий A
        val savedSomething: Boolean,       // критерий C
        val spentMandatory: Int,
        val spentOptional: Int,
        val saved: Int,
        val score: Int,                    // 0..2
        val moodDelta: Int,
        val stageBefore: PetGrowthStage,
        val stageAfter: PetGrowthStage,
        val explanation: List<String>,
    )

    val history = mutableStateListOf<WeekSummary>()

    fun finishWeek(): WeekSummary {
        val saved = depositsThisWeek.sum()
        val a = foodCovered
        val c = saved > 0
        val score = (if (a) 1 else 0) + (if (c) 1 else 0)
        val moodDelta = -10 + when (score) { 2 -> 20; 1 -> 0; else -> -15 }
        val stageBefore = stage

        satiety = (satiety - 35).coerceIn(0, 100)
        mood = (mood + moodDelta).coerceIn(0, 100)
        growthPoints += score

        val summary = WeekSummary(
            week = week,
            foodCovered = a,
            savedSomething = c,
            spentMandatory = purchases.filter { it.category == ShopCategory.MANDATORY }.sumOf { it.price },
            spentOptional = purchases.filter { it.category == ShopCategory.OPTIONAL }.sumOf { it.price },
            saved = saved,
            score = score,
            moodDelta = moodDelta,
            stageBefore = stageBefore,
            stageAfter = stage,
            explanation = buildList {
                add(if (a) "Ты купил еду — Финни сыт" else "Еды не было. На новой неделе купи её первой")
                add(if (c) "Ты отложил $saved — до цели ближе" else "Копилка не пополнилась. Попробуй отложить хотя бы 5")
                if (stage != stageBefore) add("Финни подрос! Это потому, что ты хорошо решаешь")
            }
        )
        history += summary

        depositsByWeek += saved
        depositsThisWeek.clear()
        purchases.clear()
        tasksDoneThisWeek = 0
        week += 1
        post("Карманные деньги", +WEEK_INCOME)
        return summary
    }

    private companion object {
        const val WEEK_INCOME = 60
        const val TASKS_PER_WEEK = 2
        const val STAGE_TEEN = 5
        const val STAGE_ADULT = 10
    }
}

fun PetGrowthStage.title(): String = when (this) {
    PetGrowthStage.BABY -> "Малыш"
    PetGrowthStage.TEEN -> "Подросток"
    PetGrowthStage.ADULT -> "Взрослый"
}
