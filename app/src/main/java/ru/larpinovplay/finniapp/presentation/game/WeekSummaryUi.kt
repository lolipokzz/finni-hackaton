package ru.larpinovplay.finniapp.presentation.game

import androidx.compose.runtime.Composable
import ru.larpinovplay.finniapp.domain.content.FeedbackKey
import ru.larpinovplay.finniapp.domain.game.model.WeekSummary
import ru.larpinovplay.finniapp.presentation.feedback.LocalFeedback

/** Что случилось с едой: критерий A итога недели. */
@Composable
fun WeekSummary.foodText(): String =
    LocalFeedback.current.text(if (foodCovered) FeedbackKey.PERIOD_FOOD_OK else FeedbackKey.PERIOD_FOOD_FAIL)

/** Что случилось с копилкой: критерий C итога недели. */
@Composable
fun WeekSummary.savedText(): String =
    if (savedSomething) {
        LocalFeedback.current.text(FeedbackKey.PERIOD_SAVED_OK, "n" to saved)
    } else {
        LocalFeedback.current.text(FeedbackKey.PERIOD_SAVED_FAIL)
    }

/** Поздравление, если питомец перешёл на следующую стадию (см. [WeekSummary.grew]). */
@Composable
fun WeekSummary.grewText(): String = LocalFeedback.current.text(FeedbackKey.STAGE_UP)

/** Что с планом: критерий B итога недели. Если план нарушен, объясняем, в чём и что попробовать. */
@Composable
fun WeekSummary.planText(): String {
    val feedback = LocalFeedback.current
    return when {
        planKept -> feedback.text(FeedbackKey.PERIOD_PLAN_OK)
        optionalOverPlan -> feedback.text(FeedbackKey.PERIOD_PLAN_OPTIONAL_OVER, "spent" to spentOptional, "plan" to plan.optional)
        else -> feedback.text(FeedbackKey.PERIOD_PLAN_SAVINGS_UNDER, "withdrawn" to withdrawn, "plan" to plan.savings)
    }
}
