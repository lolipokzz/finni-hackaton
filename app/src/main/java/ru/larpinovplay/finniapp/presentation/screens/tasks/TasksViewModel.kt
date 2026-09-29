package ru.larpinovplay.finniapp.presentation.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import ru.larpinovplay.finniapp.domain.content.Content
import ru.larpinovplay.finniapp.domain.game.engine.GameRules
import ru.larpinovplay.finniapp.domain.game.model.FinishBlock
import ru.larpinovplay.finniapp.domain.game.model.GameState
import ru.larpinovplay.finniapp.domain.game.model.LevelStatus
import ru.larpinovplay.finniapp.domain.game.model.TutorialStep
import ru.larpinovplay.finniapp.domain.game.model.tutorialStep
import ru.larpinovplay.finniapp.domain.game.repository.GameRepository
import ru.larpinovplay.finniapp.domain.game.repository.requireSnapshot

/**
 * Карта заданий: неделя за неделей — уровни по трём темам и приключение недели. Уровни недели открываются вместе с ней;
 * какой узел выбран на карте — вид, он живёт в экране.
 */
class TasksViewModel(
    private val game: GameRepository,
    private val content: Content,
) : ViewModel() {

    private val _state = MutableStateFlow(toUiState(game.requireSnapshot().state))
    val state: StateFlow<TasksUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            game.snapshot.filterNotNull().collect { _state.value = toUiState(it.state) }
        }
    }

    private fun toUiState(s: GameState): TasksUiState {
        val adventures = adventuresByWeek(s)
        val lastWeek = maxOf(content.levels.maxOfOrNull { it.week } ?: 0, adventures.keys.maxOrNull() ?: 0, s.week)
        val weeks = (1..lastWeek).map { number ->
            val levels = content.levels.filter { it.week == number }.map { level ->
                val status = s.levelStatus(level, content.levels)
                MapNode.LevelNode(
                    level = level,
                    status = status,
                    // Неделя уже идёт, а уровень закрыт — значит, ждёт предыдущий
                    waitsFor = s.previousLevel(level, content.levels)?.title
                        ?.takeIf { status == LevelStatus.LOCKED && level.week <= s.week },
                    stars = s.levelResult(level)?.stars ?: 0,
                    gold = level.id in s.goldLevels,
                    taskCount = GameRules.levelTasks(level),
                    challengeMinutes = (GameRules.challengeSeconds(level) + 59) / 60,
                )
            }
            MapWeek(
                number = number,
                lock = weekLock(s, number),
                nodes = levels + adventures[number].orEmpty(),
                stars = levels.sumOf { it.stars },
                maxStars = levels.size * GameRules.MAX_STARS,
            )
        }.filter { it.nodes.isNotEmpty() }
        return TasksUiState(
            weeks = weeks,
            week = s.week,
            current = weeks.flatMap { it.nodes }.firstOrNull { it.status == LevelStatus.AVAILABLE }?.id,
            coach = s.tutorialStep == TutorialStep.TASKS,
        )
    }

    /** Почему уровни недели [number] закрыты. В демо уровни открыты все. */
    private fun weekLock(s: GameState, number: Int): WeekLock? = when {
        number <= s.week || s.demoMode -> null
        number > s.week + 1 -> WeekLock.LATER
        // Неделю прожили сегодня: следующая начнётся не раньше завтра
        game.finishBlock() == FinishBlock.SAME_DAY -> WeekLock.TOMORROW
        else -> WeekLock.NEXT_WEEK
    }

    /**
     * Приключения по неделям, в конце тропинки недели: пройденные — на неделе прохождения, ждущее — на этой, остальные
     * — по одному на следующие недели, по порядку.
     */
    private fun adventuresByWeek(s: GameState): Map<Int, List<MapNode.AdventureNode>> {
        val nodes = mutableListOf<Pair<Int, MapNode.AdventureNode>>()
        s.adventureResults.forEach { result ->
            content.adventures.firstOrNull { it.id == result.adventureId }?.let { nodes += result.week to MapNode.AdventureNode(it, LevelStatus.DONE) }
        }
        val pending = content.adventures.filter { a -> s.adventureResults.none { it.adventureId == a.id } }
        val firstWeek = if (s.adventureDoneThisWeek) s.week + 1 else s.week
        pending.forEachIndexed { i, adventure ->
            val status = s.adventureStatus(adventure, content.adventures, content.levels)
            // Приключение этой недели ждёт только её уровни; остальные — ещё и свою неделю
            val waitsForLevels = status == LevelStatus.LOCKED && firstWeek + i == s.week
            nodes += firstWeek + i to MapNode.AdventureNode(adventure, status, waitsForLevels)
        }
        return nodes.groupBy({ it.first }, { it.second })
    }
}
