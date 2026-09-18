package ru.larpinovplay.finniapp.presentation.feedback

import androidx.compose.runtime.staticCompositionLocalOf
import ru.larpinovplay.finniapp.domain.content.Feedback

/**
 * Фразы обратной связи из контента. Задаётся один раз над всем приложением (MainActivity),
 * а функции вроде `LedgerReason.text()` берут отсюда шаблоны.
 */
val LocalFeedback = staticCompositionLocalOf<Feedback> {
    error("Feedback не предоставлен: оберни экран в CompositionLocalProvider(LocalFeedback provides ...)")
}
