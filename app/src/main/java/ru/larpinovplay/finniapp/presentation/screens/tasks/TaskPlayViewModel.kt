package ru.larpinovplay.finniapp.presentation.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.larpinovplay.finniapp.domain.content.Content
import ru.larpinovplay.finniapp.domain.game.repository.GameRepository
import ru.larpinovplay.finniapp.domain.task.model.Task
import ru.larpinovplay.finniapp.domain.task.model.TaskAnswer

/** Прохождение одного задания. Само задание не меняется, поэтому состояния нет, есть только разовое событие. */
class TaskPlayViewModel(
    taskId: String,
    private val game: GameRepository,
    content: Content,
) : ViewModel() {

    /** null — ключ из сохранённого стека указывает на задание, которого уже нет. */
    val task: Task? = content.tasks.firstOrNull { it.id == taskId }

    private val _effects = Channel<TaskPlayEffect>(Channel.BUFFERED)
    val effects: Flow<TaskPlayEffect> = _effects.receiveAsFlow()

    private var submitted = false

    fun onAction(action: TaskPlayAction) {
        when (action) {
            is TaskPlayAction.Submit -> submit(action.answer)
        }
    }

    private fun submit(answer: TaskAnswer) {
        val task = task ?: return
        if (submitted) return   // двойной тап по «Готово» не должен засчитать ответ дважды
        submitted = true
        viewModelScope.launch {
            _effects.send(TaskPlayEffect.Completed(task.id, game.answerTask(task, answer)))
        }
    }
}
