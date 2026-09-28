package ru.larpinovplay.finniapp.presentation.events

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import ru.larpinovplay.finniapp.domain.content.FeedbackKey

/** Сообщение внизу экрана. Текст — ключ из контента (feedback.json): слова подставит тот, кто показывает. */
data class SnackbarEvent(
    val message: FeedbackKey,
    val action: SnackbarAction? = null,
)

/** Кнопка в сообщении, например «Повторить»: [action] выполняется, если её нажали. */
data class SnackbarAction(
    val name: String,
    val action: suspend () -> Unit,
)

/**
 * Сообщения со всего приложения: ViewModel отправляет, показывает одно место — MainActivity через [ObserveAsEvents].
 * Канал без буфера: отправка ждёт, пока приложение на экране, и сообщение не теряется.
 */
object SnackbarController {

    private val _events = Channel<SnackbarEvent>()
    val events = _events.receiveAsFlow()

    suspend fun sendEvent(event: SnackbarEvent) {
        _events.send(event)
    }
}
