package ru.larpinovplay.finniapp.presentation.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random
import ru.larpinovplay.finniapp.domain.content.Content
import ru.larpinovplay.finniapp.domain.game.engine.GameRules
import ru.larpinovplay.finniapp.domain.game.repository.GameRepository
import ru.larpinovplay.finniapp.domain.task.evaluate
import ru.larpinovplay.finniapp.domain.task.model.TaskAnswer
import ru.larpinovplay.finniapp.domain.util.result.Result
import ru.larpinovplay.finniapp.domain.util.result.map
import ru.larpinovplay.finniapp.presentation.storage.snackbar

/**
 * Проходит уровень [levelId] упражнение за упражнением. Ошибка не останавливает уровень: после каждого ответа —
 * объяснение, и идём дальше; ошибки меняют только звёзды и награду.
 */
class LevelPlayViewModel(
    levelId: String,
    challenge: Boolean,
    private val game: GameRepository,
    content: Content,
    random: Random = Random.Default,   // в тестах подменяется, чтобы набор упражнений был известен заранее
) : ViewModel() {

    /** null — такого уровня нет (устаревший маршрут): экран сразу закрывается. */
    private val _state = MutableStateFlow(
        content.levels.firstOrNull { it.id == levelId }?.let { level ->
            LevelPlayUiState(
                level = level,
                // Случайные упражнения, но в порядке уровня: от простых к сложным
                tasks = level.tasks.indices.shuffled(random).take(GameRules.levelTasks(level)).sorted().map { level.tasks[it] },
                challenge = challenge,
                secondsLeft = GameRules.challengeSeconds(level).takeIf { challenge },
            )
        }
    )
    val state: StateFlow<LevelPlayUiState?> = _state.asStateFlow()

    fun onAction(action: LevelPlayAction) {
        when (action) {
            is LevelPlayAction.Submit -> submit(action.answer)
            LevelPlayAction.Next -> next()
            LevelPlayAction.Tick -> tick()
        }
    }

    private fun submit(answer: TaskAnswer) = _state.update { current ->
        if (current == null || current.feedback != null || current.finish != null) return@update current
        val outcome = current.task.evaluate(answer)
        current.copy(feedback = outcome, mistakes = if (outcome.success) current.mistakes else current.mistakes + 1)
    }

    private fun next() {
        val current = _state.value ?: return
        if (current.feedback == null || current.finish != null || current.saving) return
        if (current.last) finish(current) else _state.value = current.copy(index = current.index + 1, feedback = null)
    }

    private fun tick() = _state.update { current ->
        val left = current?.secondsLeft ?: return@update current
        if (!current.timerRunning) return@update current
        if (left > 1) {
            current.copy(secondsLeft = left - 1)
        } else {
            // Время вышло: испытание не засчитано, в игре ничего не меняется
            current.copy(secondsLeft = 0, finish = LevelFinish.Challenge(gold = false, timeUp = true, correct(current), current.total))
        }
    }

    private fun finish(done: LevelPlayUiState) {
        _state.value = done.copy(saving = true)
        viewModelScope.launch {
            val finish = if (done.challenge) {
                val seconds = GameRules.challengeSeconds(done.level) - (done.secondsLeft ?: 0)
                game.completeChallenge(done.level, done.mistakes, seconds).map { gold ->
                    LevelFinish.Challenge(gold, timeUp = false, correct(done), done.total)
                }
            } else {
                game.completeLevel(done.level, done.mistakes).map { result ->
                    // null — уровень уже был пройден (второе окно, устаревший маршрут): звёзды те же, награды нет
                    LevelFinish.Completed(result?.stars ?: GameRules.levelStars(done.mistakes), result?.reward ?: 0, correct(done), done.total)
                }
            }
            when (finish) {
                is Result.Success -> _state.value = done.copy(finish = finish.data)
                // Итог не сохранился: остаёмся на последнем упражнении, «Дальше» можно нажать ещё раз
                is Result.Error -> {
                    _state.value = done
                    finish.error.snackbar { finish(done) }
                }
            }
        }
    }

    private fun correct(state: LevelPlayUiState): Int = state.answered - state.mistakes
}
