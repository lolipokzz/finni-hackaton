package ru.larpinovplay.finniapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.larpinovplay.finniapp.data.content.defaultContent
import ru.larpinovplay.finniapp.data.game.GameRepositoryImpl
import ru.larpinovplay.finniapp.domain.game.engine.GameRules
import ru.larpinovplay.finniapp.domain.game.model.BudgetPlan
import ru.larpinovplay.finniapp.domain.game.model.LevelStatus
import ru.larpinovplay.finniapp.presentation.screens.tasks.MapNode
import ru.larpinovplay.finniapp.presentation.screens.tasks.TasksViewModel
import ru.larpinovplay.finniapp.presentation.screens.tasks.WeekLock

/** Карта заданий: какие недели открыты, где приключения и с какого узла начинать. */
@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {

    private val content = defaultContent()
    private val clock = TestClock()
    private val game = GameRepositoryImpl(FakeGameStore(), clock = clock, adventures = content.adventures, levels = content.levels)

    @Before
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun resetMain() = Dispatchers.resetMain()

    private fun viewModel(): TasksViewModel {
        runBlocking { game.createPet(SampleGames.newborn) }
        return TasksViewModel(game, content)
    }

    @Test
    fun firstWeekIsOpenLaterWeeksAreLocked() {
        val vm = viewModel()
        val weeks = vm.state.value.weeks

        assertNull(weeks[0].lock)
        assertEquals(WeekLock.NEXT_WEEK, weeks[1].lock)
        assertEquals(WeekLock.LATER, weeks[2].lock)
        assertTrue(weeks[1].nodes.all { it.status == LevelStatus.LOCKED })
        // Второй уровень первой недели ждёт первый — и карта говорит какой
        val second = weeks[0].nodes[1] as MapNode.LevelNode
        assertEquals(LevelStatus.LOCKED, second.status)
        assertEquals(content.levels.first().title, second.waitsFor)
        assertNull((weeks[1].nodes[0] as MapNode.LevelNode).waitsFor)   // закрыт неделей, а не очередью
        // Начать — с первого уровня; приключение недели — последний узел первой недели
        assertEquals(content.levels.first().id, vm.state.value.current)
        val adventure = weeks[0].nodes.last() as MapNode.AdventureNode
        assertEquals(content.adventures.first().id, adventure.id)
        // Приключение ждёт уровни недели — и карта так и говорит
        assertEquals(LevelStatus.LOCKED, adventure.status)
        assertTrue(adventure.waitsForLevels)
        assertEquals(0, weeks[0].stars)
        assertEquals(3 * GameRules.MAX_STARS, weeks[0].maxStars)
    }

    /** Уровни недели пройдены, приключение тоже, неделя прожита сегодня — следующая «откроется завтра». */
    @Test
    fun nextWeekOpensTomorrowWhenThisOneIsDone() {
        val vm = viewModel()
        runBlocking {
            game.confirmPlan(BudgetPlan(optional = game.snapshot.value!!.state.balance))
            content.levels.filter { it.week == 1 }.forEachIndexed { i, level -> game.completeLevel(level, mistakes = i) }
            game.completeAdventure(content.adventures.first(), mistakes = 0)
        }

        val week = vm.state.value.weeks[0]
        assertEquals(WeekLock.TOMORROW, vm.state.value.weeks[1].lock)
        assertEquals(LevelStatus.DONE, (week.nodes.last() as MapNode.AdventureNode).status)
        assertEquals(3 + 2 + 1, week.stars)   // без ошибок, с одной, с двумя
    }

    @Test
    fun demoOpensEveryLevelAndAdventure() {
        runBlocking { game.resetToDemo() }
        val vm = TasksViewModel(game, content)
        val nodes = vm.state.value.weeks.flatMap { it.nodes }

        assertTrue(vm.state.value.weeks.all { it.lock == null })
        assertTrue(nodes.all { it.status == LevelStatus.AVAILABLE })
        assertEquals(content.adventures.size, nodes.count { it is MapNode.AdventureNode })
    }
}
