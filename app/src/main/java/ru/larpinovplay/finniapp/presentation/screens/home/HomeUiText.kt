package ru.larpinovplay.finniapp.presentation.screens.home

import androidx.compose.runtime.Composable
import ru.larpinovplay.finniapp.domain.content.FeedbackKey
import ru.larpinovplay.finniapp.presentation.feedback.LocalFeedback

/** Фраза под питомцем. */
@Composable
fun HomeUiState.MoodExplanation.text(): String {
    val feedback = LocalFeedback.current
    return when (this) {
        HomeUiState.MoodExplanation.Hungry -> feedback.text(FeedbackKey.MOOD_HUNGRY)
        HomeUiState.MoodExplanation.Grew -> feedback.text(FeedbackKey.MOOD_GREW)
        is HomeUiState.MoodExplanation.Purchased -> feedback.text(FeedbackKey.MOOD_PURCHASE, "item" to itemName.lowercase())
        HomeUiState.MoodExplanation.Waiting -> feedback.text(FeedbackKey.MOOD_DEFAULT)
    }
}

/** Текст облачка с подсказкой. */
@Composable
fun HomeUiState.Tip.text(): String {
    val feedback = LocalFeedback.current
    return when (this) {
        HomeUiState.Tip.ChooseGoal -> feedback.text(FeedbackKey.TIP_CHOOSE_GOAL)
        is HomeUiState.Tip.SaveFor -> feedback.text(FeedbackKey.TIP_SAVE_FOR, "goal" to goalName)
    }
}
