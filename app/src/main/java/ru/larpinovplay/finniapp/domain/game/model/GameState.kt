package ru.larpinovplay.finniapp.domain.game.model

import ru.larpinovplay.finniapp.domain.adventure.model.Adventure
import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem
import ru.larpinovplay.finniapp.domain.task.model.Level
import ru.larpinovplay.finniapp.domain.task.model.TaskTopic
import java.time.LocalDate
import ru.larpinovplay.finniapp.domain.game.engine.GameEngine

/**
 * Игровое состояние: кошелёк, журнал, покупки, копилка, задания, история недель, гардероб.
 * [purchases] — покупки текущей недели (обнуляются в конце недели), [wardrobe] — купленная одежда, она остаётся.
 * Питомец сюда не входит: он лежит рядом, в [GameSnapshot]. Неизменяемо; менять его может только [GameEngine].
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
    val wardrobe: List<ShopItem> = emptyList(),
    val goal: SavingsGoal? = null,
    val completedGoals: List<SavingsGoal> = emptyList(),
    val trip: Trip? = null,                         // последняя поездка; питомец в ней, только пока идёт её неделя
    val depositsThisWeek: List<Int> = emptyList(),
    val depositsByWeek: List<Int> = emptyList(),    // сколько за закрытую неделю отложено за вычетом снятого
    val withdrawalsThisWeek: List<Int> = emptyList(),
    val levelResults: List<LevelResult> = emptyList(),   // первые прохождения уровней карты заданий
    val goldLevels: Set<String> = emptySet(),             // уровни с пройденным золотым испытанием
    val adventureResults: List<AdventureResult> = emptyList(),
    val history: List<WeekSummary> = emptyList(),
    val tutorial: Boolean = false,                  // идёт обучение на главном экране (см. tutorialStep)
    val tutorialSkipped: Set<TutorialStep> = emptySet(),   // шаги обучения, которые ребёнок пропустил
) {
    fun owns(item: ShopItem): Boolean = wardrobe.any { it.id == item.id }

    /** Сколько за эту неделю потрачено на товары [category]. */
    fun spentThisWeek(category: ShopCategory): Int = purchases.filter { it.category == category }.sumOf { it.price }

    /** Сколько за эту неделю отложено за вычетом снятого; может быть меньше нуля. Покупка цели сюда не входит. */
    val savedThisWeek: Int get() = depositsThisWeek.sum() - withdrawalsThisWeek.sum()

    /** Карманные (или стартовые) монеты, пришедшие в начале этой недели. */
    val weekIncome: Int
        get() = ledger.filter { it.week == week && (it.reason == LedgerReason.WeekIncome || it.reason == LedgerReason.StartCoins) }
            .sumOf { it.balanceDelta }

    /** Сколько вещей и целей радуют Финни каждую неделю: одежда из гардероба и достигнутые цели. */
    val lastingJoys: Int get() = wardrobe.size + completedGoals.size

    /** Поездка, в которой питомец сейчас; null — он дома. */
    val currentTrip: Trip? get() = trip?.takeIf { it.week == week }

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

    /** Пройдены все уровни [levels] до этой недели включительно: после них открывается приключение недели. */
    fun weekLevelsDone(levels: List<Level>): Boolean = levels.filter { it.week <= week }.all { levelResult(it) != null }

    /**
     * Приключение на карте: пройдено; можно — это приключение недели и уровни недели пройдены (оно в конце
     * тропинки недели); иначе закрыто. В демо (ТЗ 2.5.8) все непройденные приключения открыты сразу.
     */
    fun adventureStatus(adventure: Adventure, adventures: List<Adventure>, levels: List<Level>): LevelStatus = when {
        adventureResults.any { it.adventureId == adventure.id } -> LevelStatus.DONE
        demoMode -> LevelStatus.AVAILABLE
        adventureOfWeek(adventures)?.id == adventure.id && weekLevelsDone(levels) -> LevelStatus.AVAILABLE
        else -> LevelStatus.LOCKED
    }

    /**
     * Почему неделю сейчас нельзя закончить, или null, если можно. [today] — сегодняшняя дата устройства,
     * [adventures] и [levels] — приключения и уровни из контента: приключение недели открывается после её уровней.
     */
    fun finishBlock(today: LocalDate, adventures: List<Adventure> = emptyList(), levels: List<Level> = emptyList()): FinishBlock? = when {
        phase != PeriodPhase.ACTIVE -> FinishBlock.PLAN_NOT_CONFIRMED
        adventureOfWeek(adventures) != null && !demoMode && !weekLevelsDone(levels) -> FinishBlock.LEVELS_NOT_PLAYED
        adventureOfWeek(adventures) != null -> FinishBlock.ADVENTURE_NOT_PLAYED
        // Не больше недели в день. Если часы перевели назад, не запираем игру: блокирует только тот же день
        !demoMode && today == periodStartedOn -> FinishBlock.SAME_DAY
        else -> null
    }

    /**
     * Уровень на карте [levels] (по порядку тропинки). Уровни идут друг за другом: следующий открывается, когда
     * пройден предыдущий, и не раньше своей недели. Непройденный уровень ждёт и на следующих неделях.
     * В демо (ТЗ 2.5.8) все уровни открыты сразу — без недель и без очереди.
     */
    fun levelStatus(level: Level, levels: List<Level>): LevelStatus = when {
        levelResult(level) != null -> LevelStatus.DONE
        demoMode -> LevelStatus.AVAILABLE
        level.week > week -> LevelStatus.LOCKED
        previousLevel(level, levels)?.let { levelResult(it) == null } == true -> LevelStatus.LOCKED
        else -> LevelStatus.AVAILABLE
    }

    /** Уровень перед [level] на тропинке [levels]; null — [level] первый. */
    fun previousLevel(level: Level, levels: List<Level>): Level? = levels.getOrNull(levels.indexOfFirst { it.id == level.id } - 1)

    fun levelResult(level: Level): LevelResult? = levelResults.firstOrNull { it.levelId == level.id }

    fun availableLevels(levels: List<Level>): List<Level> = levels.filter { levelStatus(it, levels) == LevelStatus.AVAILABLE }

    fun topicProgress(levels: List<Level>): List<TopicProgress> =
        TaskTopic.entries.map { topic ->
            val ofTopic = levels.filter { it.topic == topic }
            TopicProgress(topic, total = ofTopic.size, done = ofTopic.count { levelResult(it) != null })
        }

    /** Сколько не хватает до цели, если бы в копилке было [savingsIfAny]; 0 — хватает, null — цели нет. */
    fun goalRemaining(savingsIfAny: Int = savings): Int? = goal?.let { (it.cost - savingsIfAny).coerceAtLeast(0) }

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
