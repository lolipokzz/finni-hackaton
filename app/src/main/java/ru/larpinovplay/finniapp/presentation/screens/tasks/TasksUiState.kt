package ru.larpinovplay.finniapp.presentation.screens.tasks

import ru.larpinovplay.finniapp.domain.adventure.model.Adventure
import ru.larpinovplay.finniapp.domain.game.model.LevelStatus
import ru.larpinovplay.finniapp.domain.task.model.Level

/** Карта заданий: недели сверху вниз, в каждой — уровни и приключение недели. */
data class TasksUiState(
    val weeks: List<MapWeek>,
    val week: Int,                  // текущая игровая неделя
    val current: String?,           // id узла «Начни отсюда»: первый открытый уровень или приключение
    val coach: Boolean = false,     // обучение: Финни просит пройти первый уровень
)

/**
 * Неделя на карте. [lock] — почему её уровни пока закрыты; null — открыта.
 * [stars] из [maxStars] — звёзды за уровни недели: счётчик в шапке показывает неделю, которая сейчас на экране.
 */
data class MapWeek(
    val number: Int,
    val lock: WeekLock?,
    val nodes: List<MapNode>,
    val stars: Int = 0,
    val maxStars: Int = 0,
) {
    val done: Int get() = nodes.count { it.status == LevelStatus.DONE }
}

/** Когда откроется неделя: завтра (эта уже прожита сегодня), после закрытия этой недели, или позже. */
enum class WeekLock { TOMORROW, NEXT_WEEK, LATER }

/** Узел тропинки: уровень или приключение недели. Статус приключения — те же «закрыто / можно / пройдено». */
sealed interface MapNode {
    val id: String
    val title: String
    val status: LevelStatus

    /**
     * [waitsFor] — название предыдущего уровня, если этот закрыт только потому, что тот ещё не пройден;
     * [challengeMinutes] — сколько минут даётся на золотое испытание уровня.
     */
    data class LevelNode(
        val level: Level,
        override val status: LevelStatus,
        val waitsFor: String? = null,
        val stars: Int,
        val gold: Boolean,
        val taskCount: Int,   // сколько упражнений в одном прохождении
        val challengeMinutes: Int,
    ) : MapNode {
        override val id: String get() = level.id
        override val title: String get() = level.title
    }

    /** [waitsForLevels] — приключение этой недели закрыто, пока не пройдены её уровни. */
    data class AdventureNode(
        val adventure: Adventure,
        override val status: LevelStatus,
        val waitsForLevels: Boolean = false,
    ) : MapNode {
        override val id: String get() = adventure.id
        override val title: String get() = adventure.title
    }
}
