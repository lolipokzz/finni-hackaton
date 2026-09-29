package ru.larpinovplay.finniapp.presentation.game

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.content.FeedbackKey
import ru.larpinovplay.finniapp.domain.game.engine.GameRules
import ru.larpinovplay.finniapp.domain.game.model.Deed
import ru.larpinovplay.finniapp.domain.game.model.WeekSummary
import ru.larpinovplay.finniapp.presentation.feedback.LocalFeedback

/** Название дела недели — одинаковое на главном экране, в итогах и в прогрессе. */
val Deed.title: String
    get() = when (this) {
        Deed.FED -> "Финни сыт"
        Deed.NOT_BORED -> "Финни не скучает"
        Deed.SAVINGS_ON_PLAN -> "Копилка по плану"
        Deed.SPENDING_ON_PLAN -> "Траты по плану"
    }

@get:DrawableRes
val Deed.icon: Int
    get() = when (this) {
        Deed.FED -> R.drawable.ic_apple
        Deed.NOT_BORED -> R.drawable.ic_smile
        Deed.SAVINGS_ON_PLAN -> R.drawable.ic_pig
        Deed.SPENDING_ON_PLAN -> R.drawable.ic_clipboard
    }

/** Что сделать для дела на этой неделе; [weekSatiety] — сколько сытости уже куплено. */
@Composable
fun Deed.todoText(weekSatiety: Int): String {
    val feedback = LocalFeedback.current
    return when (this) {
        Deed.FED -> feedback.text(FeedbackKey.DEED_FED_TODO, "need" to GameRules.WEEKLY_HUNGER, "n" to weekSatiety)
        Deed.NOT_BORED -> feedback.text(FeedbackKey.DEED_NOT_BORED_TODO)
        Deed.SAVINGS_ON_PLAN -> feedback.text(FeedbackKey.DEED_SAVINGS_TODO)
        Deed.SPENDING_ON_PLAN -> feedback.text(FeedbackKey.DEED_SPENDING_TODO)
    }
}

/**
 * Почему за неделю изменилось настроение (ТЗ 2.5.10): неделя его снижает, а одежда и достигнутые цели немного
 * поддерживают (docs/04-rules-and-formulas.md, «Состояние питомца»).
 */
@Composable
fun WeekSummary.moodText(): String {
    val feedback = LocalFeedback.current
    return if (lastingMood > 0) {
        feedback.text(FeedbackKey.PERIOD_MOOD_LASTING, "decay" to GameRules.WEEKLY_MOOD_DECAY, "lasting" to lastingMood)
    } else {
        feedback.text(FeedbackKey.PERIOD_MOOD, "decay" to GameRules.WEEKLY_MOOD_DECAY)
    }
}

/** Итог дела за неделю: получилось — что именно, нет — почему и что попробовать. */
@Composable
fun WeekSummary.deedText(deed: Deed): String {
    val feedback = LocalFeedback.current
    val done = deeds[deed]
    return when (deed) {
        Deed.FED -> feedback.text(if (done) FeedbackKey.PERIOD_FOOD_OK else FeedbackKey.PERIOD_FOOD_FAIL, "need" to GameRules.WEEKLY_HUNGER)
        Deed.NOT_BORED -> feedback.text(if (done) FeedbackKey.PERIOD_BORED_OK else FeedbackKey.PERIOD_BORED_FAIL)
        Deed.SAVINGS_ON_PLAN -> when {
            done -> feedback.text(FeedbackKey.PERIOD_SAVED_OK, "n" to saved)
            withdrawn > 0 && savingsUnderPlan -> feedback.text(FeedbackKey.PERIOD_PLAN_SAVINGS_UNDER, "withdrawn" to withdrawn, "plan" to plan.savings)
            else -> feedback.text(FeedbackKey.PERIOD_SAVED_FAIL)
        }
        Deed.SPENDING_ON_PLAN ->
            if (done) {
                feedback.text(FeedbackKey.PERIOD_PLAN_OK)
            } else {
                feedback.text(FeedbackKey.PERIOD_PLAN_OPTIONAL_OVER, "spent" to spentOptional, "plan" to plan.optional)
            }
    }
}

/** Поздравление, если питомец перешёл на следующую стадию (см. [WeekSummary.grew]). */
@Composable
fun WeekSummary.grewText(): String = LocalFeedback.current.text(FeedbackKey.STAGE_UP)
