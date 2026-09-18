package ru.larpinovplay.finniapp.presentation.game

import androidx.compose.runtime.Composable
import ru.larpinovplay.finniapp.domain.content.FeedbackKey
import ru.larpinovplay.finniapp.domain.game.model.LedgerReason
import ru.larpinovplay.finniapp.presentation.feedback.LocalFeedback

/** Подпись причины в журнале монет. Domain текста не знает: шаблон берётся из контента. */
@Composable
fun LedgerReason.text(): String {
    val feedback = LocalFeedback.current
    return when (this) {
        LedgerReason.StartCoins -> feedback.text(FeedbackKey.LEDGER_START)
        is LedgerReason.Purchase -> itemName
        LedgerReason.Deposit -> feedback.text(FeedbackKey.LEDGER_DEPOSIT)
        is LedgerReason.GoalReached -> feedback.text(FeedbackKey.LEDGER_GOAL, "goal" to goalName)
        is LedgerReason.TaskReward -> feedback.text(FeedbackKey.LEDGER_TASK, "task" to taskTitle)
        LedgerReason.WeekIncome -> feedback.text(FeedbackKey.LEDGER_WEEK_INCOME)
    }
}
