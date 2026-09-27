package ru.larpinovplay.finniapp.presentation.screens.savings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.larpinovplay.finniapp.domain.content.FeedbackKey
import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal
import ru.larpinovplay.finniapp.presentation.feedback.LocalFeedback
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * Забрать из копилки (ТЗ 2.5.7): можно всё, это деньги ребёнка. Окно не отговаривает, а показывает,
 * что изменится: сколько останется в копилке и насколько отодвинется цель.
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
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White,
        title = { Text("Забрать из копилки?", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AmountButton("−", "Забрать меньше", enabled = draft.amount > 1) { onChange(false) }
                    Text(
                        "${draft.amount}",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(72.dp),
                    )
                    AmountButton("+", "Забрать больше", enabled = draft.amount < draft.savingsBefore) { onChange(true) }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "В копилке ${draft.savingsBefore} → станет ${draft.savingsAfter}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (goal != null) {
                    val before = (goal.cost - draft.savingsBefore).coerceAtLeast(0)
                    val after = (goal.cost - draft.savingsAfter).coerceAtLeast(0)
                    Text(
                        feedback.text(FeedbackKey.WITHDRAW_GOAL_FURTHER, "goal" to goal.name, "before" to before, "after" to after),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    val weeksBefore = draft.weeksBefore
                    val weeksAfter = draft.weeksAfter
                    if (weeksBefore != null && weeksAfter != null && weeksAfter > weeksBefore) {
                        Text(
                            feedback.text(FeedbackKey.WITHDRAW_WEEKS, "before" to weeksBefore, "after" to weeksAfter),
                            style = MaterialTheme.typography.bodyMedium,
                            color = FinniColors.NavyMuted,
                        )
                    }
                } else {
                    Text(feedback.text(FeedbackKey.WITHDRAW_NO_GOAL), style = MaterialTheme.typography.bodyMedium, color = FinniColors.NavyMuted)
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, shape = RoundedCornerShape(16.dp), modifier = Modifier.height(48.dp)) {
                Text("Забрать ${draft.amount}", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
                Text("Оставить", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

@Composable
private fun AmountButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (enabled) FinniColors.BlueLight else FinniColors.Track,
        modifier = Modifier.size(48.dp).semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(symbol, style = MaterialTheme.typography.headlineSmall, color = if (enabled) FinniColors.Blue else FinniColors.NavyMuted)
        }
    }
}
