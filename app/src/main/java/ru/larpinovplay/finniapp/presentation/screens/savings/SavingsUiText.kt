package ru.larpinovplay.finniapp.presentation.screens.savings

import androidx.compose.runtime.Composable
import ru.larpinovplay.finniapp.domain.content.FeedbackKey
import ru.larpinovplay.finniapp.domain.game.model.DepositResult
import ru.larpinovplay.finniapp.presentation.feedback.LocalFeedback

/** Почему отложить не удалось. */
@Composable
fun DepositResult.Rejected.text(): String =
    LocalFeedback.current.text(FeedbackKey.DEPOSIT_REJECTED, "n" to balance)
