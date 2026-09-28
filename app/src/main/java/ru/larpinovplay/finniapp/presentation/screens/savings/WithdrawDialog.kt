package ru.larpinovplay.finniapp.presentation.screens.savings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.content.FeedbackKey
import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal
import ru.larpinovplay.finniapp.presentation.components.CardDialog
import ru.larpinovplay.finniapp.presentation.components.CardSticker
import ru.larpinovplay.finniapp.presentation.components.CardTitle
import ru.larpinovplay.finniapp.presentation.components.DashedDivider
import ru.larpinovplay.finniapp.presentation.components.SoftButton
import ru.larpinovplay.finniapp.presentation.components.StatRow
import ru.larpinovplay.finniapp.presentation.components.StepButton
import ru.larpinovplay.finniapp.presentation.components.TealButton
import ru.larpinovplay.finniapp.presentation.feedback.LocalFeedback
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * Забрать из копилки (ТЗ 2.5.7): можно всё, это деньги ребёнка. Окно не отговаривает, а показывает,
 * что изменится: сколько останется в копилке и насколько отодвинется мечта.
 */
@Composable
fun WithdrawDialog(
    draft: SavingsUiState.Withdraw,
    goal: SavingsGoal?,
    onChange: (increase: Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val feedback = LocalFeedback.current
    CardDialog(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CardSticker(R.drawable.ic_deed_pig, FinniColors.DreamTint, size = 64.dp, iconScale = 0.6f)
            Column(Modifier.weight(1f)) { CardTitle("Забрать из копилки?", "Там сейчас ${draft.savingsBefore}") }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            StepButton("−", "Забрать меньше", enabled = draft.amount > 1) { onChange(false) }
            Row(
                Modifier.width(112.dp).semantics(mergeDescendants = true) { contentDescription = "Забрать: ${draft.amount}" },
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(painterResource(R.drawable.ic_coin), null, Modifier.size(30.dp))
                Text("${draft.amount}", fontSize = 30.sp, fontWeight = FontWeight.Black, color = FinniColors.CoinInk, textAlign = TextAlign.Center)
            }
            StepButton("+", "Забрать больше", enabled = draft.amount < draft.savingsBefore) { onChange(true) }
        }

        DashedDivider()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatRow("В копилке станет", "${draft.savingsAfter}", coin = true)
            val before = draft.remainingBefore
            val after = draft.remainingAfter
            if (goal != null && before != null && after != null) {
                Text(
                    feedback.text(FeedbackKey.WITHDRAW_GOAL_FURTHER, "goal" to goal.name, "before" to before, "after" to after),
                    style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted,
                )
                val weeksBefore = draft.weeksBefore
                val weeksAfter = draft.weeksAfter
                if (weeksBefore != null && weeksAfter != null && weeksAfter > weeksBefore) {
                    Text(
                        feedback.text(FeedbackKey.WITHDRAW_WEEKS, "before" to weeksBefore, "after" to weeksAfter),
                        style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.WarnInk,
                    )
                }
            } else {
                Text(feedback.text(FeedbackKey.WITHDRAW_NO_GOAL), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TealButton("Забрать ${draft.amount}", R.drawable.ic_coin, onConfirm)
            SoftButton("Оставить в копилке", onDismiss)
        }
    }
}
