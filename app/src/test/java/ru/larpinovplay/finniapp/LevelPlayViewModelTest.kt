package ru.larpinovplay.finniapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.larpinovplay.finniapp.data.content.defaultContent
import ru.larpinovplay.finniapp.data.game.GameRepositoryImpl
import ru.larpinovplay.finniapp.domain.game.engine.GameRules
import ru.larpinovplay.finniapp.domain.game.model.LevelStatus
import ru.larpinovplay.finniapp.domain.game.repository.requireSnapshot
import ru.larpinovplay.finniapp.domain.storage.StorageError
import ru.larpinovplay.finniapp.domain.task.model.Task
import ru.larpinovplay.finniapp.domain.task.model.TaskAnswer
import ru.larpinovplay.finniapp.domain.task.model.TaskPayload
import ru.larpinovplay.finniapp.presentation.screens.tasks.LevelFinish
import ru.larpinovplay.finniapp.presentation.screens.tasks.LevelPlayAction
import ru.larpinovplay.finniapp.presentation.screens.tasks.LevelPlayViewModel
import kotlin.random.Random

/** Уровень от первого упражнения до итога: разбор после каждого ответа, звёзды, награда и золотое испытание. */
@OptIn(ExperimentalCoroutinesApi::class)
class LevelPlayViewModelTest {

    private val content = defaultContent()
    private val level = content.levels.first()
    private val store = FakeGameStore()
    private val game = GameRepositoryImpl(store, clock = TestClock(), levels = content.levels)

    @Before
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun resetMain() = Dispatchers.resetMain()

    private fun viewModel(challenge: Boolean = false): LevelPlayViewModel {
        if (game.snapshot.value == null) runBlocking { game.createPet(SampleGames.newborn) }
        return LevelPlayViewModel(level.id, challenge, game, content)
    }

    private val LevelPlayViewModel.now get() = checkNotNull(state.value)

    /** Отвечает на текущее упражнение верно или с ошибкой и жмёт «Дальше». */
    private fun LevelPlayViewModel.answer(right: Boolean) {
        onAction(LevelPlayAction.Submit(if (right) rightAnswer(now.task) else wrongAnswer(now.task)))
        onAction(LevelPlayAction.Next)
    }

    @Test
    fun answerShowsExplanationAndWaitsForNext() {
        val vm = viewModel()

        vm.onAction(LevelPlayAction.Submit(wrongAnswer(vm.now.task)))
        vm.onAction(LevelPlayAction.Submit(rightAnswer(vm.now.task)))   // второй ответ на то же упражнение не считается

        assertFalse(checkNotNull(vm.now.feedback).success)
        assertEquals(1, vm.now.mistakes)
        assertEquals(0, vm.now.index)
        vm.onAction(LevelPlayAction.Next)
        assertEquals(1, vm.now.index)
        assertNull(vm.now.feedback)
    }

    @Test
    fun finishingLevelGivesStarsAndRewardOnce() {
        val vm = viewModel()
        val balance = game.requireSnapshot().state.balance

        vm.answer(right = false)
        repeat(vm.now.total - 1) { vm.answer(right = true) }

        val finish = vm.now.finish as LevelFinish.Completed
        assertEquals(GameRules.MAX_STARS - 1, finish.stars)
        assertEquals(level.rewardOnMistake, finish.reward)
        assertEquals(vm.now.total - 1, finish.correct)
        assertEquals(balance + level.rewardOnMistake, game.requireSnapshot().state.balance)
        assertEquals(LevelStatus.DONE, game.requireSnapshot().state.levelStatus(level, content.levels))
    }

    @Test
    fun saveErrorKeepsLastTaskToRetry() {
        val vm = viewModel()
        store.saveFailure = StorageError.WRITE_FAILED
        repeat(vm.now.total) { vm.answer(right = true) }

        assertNull(vm.now.finish)
        assertFalse(vm.now.saving)
        store.saveFailure = null
        vm.onAction(LevelPlayAction.Next)
        assertTrue(vm.now.finish is LevelFinish.Completed)
    }

    @Test
    fun challengeWithoutMistakesInTimeMakesLevelGold() {
        runBlocking {
            game.createPet(SampleGames.newborn)
            game.completeLevel(level, mistakes = 2)
        }
        val balance = game.requireSnapshot().state.balance
        val vm = viewModel(challenge = true)
        assertEquals(GameRules.challengeSeconds(level), vm.now.secondsLeft)

        vm.onAction(LevelPlayAction.Tick)
        repeat(vm.now.total) { vm.answer(right = true) }

        assertTrue((vm.now.finish as LevelFinish.Challenge).gold)
        assertTrue(level.id in game.requireSnapshot().state.goldLevels)
        assertEquals(balance, game.requireSnapshot().state.balance)   // без монет
    }

    @Test
    fun challengeEndsWhenTimeIsUpAndPausesDuringExplanation() {
        runBlocking {
            game.createPet(SampleGames.newborn)
            game.completeLevel(level, mistakes = 0)
        }
        val vm = viewModel(challenge = true)
        val limit = checkNotNull(vm.now.secondsLeft)

        vm.onAction(LevelPlayAction.Submit(rightAnswer(vm.now.task)))
        vm.onAction(LevelPlayAction.Tick)   // разбор читается без спешки
        assertEquals(limit, vm.now.secondsLeft)

        vm.onAction(LevelPlayAction.Next)
        repeat(limit) { vm.onAction(LevelPlayAction.Tick) }

        val finish = vm.now.finish as LevelFinish.Challenge
        assertTrue(finish.timeUp)
        assertFalse(finish.gold)
        assertTrue(game.requireSnapshot().state.goldLevels.isEmpty())
    }

    /** Из заданий уровня берутся несколько случайных — разных, в порядке уровня; разные прохождения — разные наборы. */
    @Test
    fun levelPlaysRandomTasksInLevelOrder() {
        runBlocking { game.createPet(SampleGames.newborn) }
        val runs = (1..20).map { seed -> checkNotNull(LevelPlayViewModel(level.id, false, game, content, Random(seed)).state.value).tasks }

        runs.forEach { tasks ->
            assertEquals(GameRules.TASKS_PER_LEVEL, tasks.toSet().size)
            assertEquals(tasks.sortedBy { level.tasks.indexOf(it) }, tasks)
        }
        assertTrue(runs.toSet().size > 1)
    }

    @Test
    fun exitIsConfirmedOnlyAfterFirstAnswer() {
        val vm = viewModel()
        assertFalse(vm.now.inProgress)

        vm.onAction(LevelPlayAction.Submit(rightAnswer(vm.now.task)))
        assertTrue(vm.now.inProgress)
    }

    @Test
    fun unknownLevelClosesScreen() {
        runBlocking { game.createPet(SampleGames.newborn) }
        assertNull(LevelPlayViewModel("нет-такого", challenge = false, game, content).state.value)
    }

    companion object {
        fun rightAnswer(task: Task): TaskAnswer = when (val p = task.payload) {
            is TaskPayload.Choice -> TaskAnswer.Choice(p.options.first { it.correct }.id)
            is TaskPayload.Allocate -> TaskAnswer.Allocation(
                mapOf("mandatory" to p.mandatoryMin, "optional" to p.total - p.mandatoryMin - p.savingsMin, "savings" to p.savingsMin),
            )
            is TaskPayload.ShopList -> TaskAnswer.Selection(p.items.filter { it.mandatory }.map { it.id }.toSet())
        }

        fun wrongAnswer(task: Task): TaskAnswer = when (val p = task.payload) {
            is TaskPayload.Choice -> TaskAnswer.Choice(p.options.first { !it.correct }.id)
            is TaskPayload.Allocate -> TaskAnswer.Allocation(mapOf("optional" to p.total))
            is TaskPayload.ShopList -> TaskAnswer.Selection(p.items.map { it.id }.toSet())
        }
    }
}
