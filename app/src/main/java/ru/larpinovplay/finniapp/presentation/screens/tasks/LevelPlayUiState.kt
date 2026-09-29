package ru.larpinovplay.finniapp.presentation.screens.tasks

import ru.larpinovplay.finniapp.domain.task.model.Level
import ru.larpinovplay.finniapp.domain.task.model.Task
import ru.larpinovplay.finniapp.domain.task.model.TaskOutcome

/**
 * Прохождение уровня: упражнения по одному, после ответа — объяснение, в конце — итог.
 * [tasks] — упражнения этого прохождения, выбранные случайно из всех упражнений уровня.
 * [challenge] — золотое испытание: без подсказок, на время, без награды.
 */
data class LevelPlayUiState(
    val level: Level,
    val tasks: List<Task>,
    val challenge: Boolean,
    val index: Int = 0,                   // текущее упражнение
    val mistakes: Int = 0,
    val feedback: TaskOutcome? = null,    // ответ на текущее упражнение проверен; ждём «Дальше»
    val secondsLeft: Int? = null,         // только в испытании
    val saving: Boolean = false,          // итог записывается: «Дальше» не нажать второй раз
    val finish: LevelFinish? = null,      // уровень закончен: показать итог
) {
    val task: Task get() = tasks[index]
    val total: Int get() = tasks.size

    /** Сколько упражнений уже отвечено — для полоски сверху. */
    val answered: Int get() = index + if (feedback != null) 1 else 0

    /** Последнее упражнение: после него «Дальше» ведёт к итогу. */
    val last: Boolean get() = index == total - 1

    /** Уровень начат и не закончен: выход сбросит ответы, поэтому экран сначала переспросит. */
    val inProgress: Boolean get() = answered > 0 && finish == null

    /** Таймер идёт, только пока ребёнок отвечает: объяснение читается без спешки. */
    val timerRunning: Boolean get() = secondsLeft != null && feedback == null && finish == null
}

/** Итог уровня. */
sealed interface LevelFinish {
    val correct: Int
    val total: Int

    /** Первое прохождение: звёзды и монеты. [reward] = 0 — уровень уже был пройден, награды нет. */
    data class Completed(val stars: Int, val reward: Int, override val correct: Int, override val total: Int) : LevelFinish

    /** Золотое испытание: [gold] — получено; [timeUp] — не успел за отведённое время. */
    data class Challenge(val gold: Boolean, val timeUp: Boolean, override val correct: Int, override val total: Int) : LevelFinish
}
