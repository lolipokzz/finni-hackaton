package ru.larpinovplay.finniapp.presentation.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import ru.larpinovplay.finniapp.domain.content.FeedbackKey
import ru.larpinovplay.finniapp.domain.game.model.BudgetDirection
import ru.larpinovplay.finniapp.presentation.feedback.LocalFeedback
import ru.larpinovplay.finniapp.presentation.game.icon
import ru.larpinovplay.finniapp.presentation.game.label
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * План недели (ТЗ 2.5.5): в начале недели ребёнок раскладывает все монеты — карманные и остаток —
 * по трём направлениям. Закрыть окно, не составив план, нельзя: без плана неделя не начинается.
 * Слишком мало на обязательное — только предупреждение, решает ребёнок.
 */
@Composable
fun WeekPlanDialog(
    draft: HomeUiState.PlanDraft,
    onChange: (BudgetDirection, increase: Boolean) -> Unit,
    onConfirm: () -> Unit,
) {
    val feedback = LocalFeedback.current
    AlertDialog(
        onDismissRequest = {},
        // Шире стандартного окна: строкам плана нужны подпись и две кнопки по 48 dp
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false),
        modifier = Modifier.padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White,
        title = { Text("Неделя ${draft.week}: план", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text("У тебя ${draft.budget} монет", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (draft.carried > 0) "${draft.income} карманных + ${draft.carried} с прошлой недели" else "${draft.income} карманных",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FinniColors.NavyMuted,
                )
                Spacer(Modifier.height(12.dp))
                BudgetDirection.entries.forEach { direction ->
                    PlanRow(
                        direction = direction,
                        amount = draft.plan[direction],
                        canIncrease = draft.unallocated > 0,
                        onChange = { increase -> onChange(direction, increase) },
                    )
                    if (direction == BudgetDirection.MANDATORY && draft.need > 0) {
                        // Предупреждение отличается словами и значком «!», а не только цветом
                        Text(
                            if (draft.mandatoryLow) "! " + feedback.text(FeedbackKey.PLAN_NEED_LOW, "n" to draft.need)
                            else feedback.text(FeedbackKey.PLAN_NEED, "n" to draft.need),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (draft.mandatoryLow) FinniColors.Warning else FinniColors.NavyMuted,
                            modifier = Modifier.padding(start = 44.dp, bottom = 4.dp),
                        )
                    }
                    if (direction == BudgetDirection.SAVINGS && draft.plan.savings > 0) {
                        Text(
                            feedback.text(FeedbackKey.PLAN_SAVINGS_NOW),
                            style = MaterialTheme.typography.bodyMedium,
                            color = FinniColors.NavyMuted,
                            modifier = Modifier.padding(start = 44.dp, bottom = 4.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (draft.unallocated == 0) FinniColors.CardMint else FinniColors.Sunny,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (draft.unallocated == 0) feedback.text(FeedbackKey.PLAN_DONE)
                        else feedback.text(FeedbackKey.PLAN_UNALLOCATED, "n" to draft.unallocated),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = draft.unallocated == 0,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(48.dp),
            ) {
                Text("Начать неделю", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

@Composable
private fun PlanRow(
    direction: BudgetDirection,
    amount: Int,
    canIncrease: Boolean,
    onChange: (increase: Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(direction.icon), contentDescription = null, modifier = Modifier.size(36.dp))
        Spacer(Modifier.width(8.dp))
        Text(direction.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StepButton("−", "Убавить: ${direction.label}", enabled = amount > 0) { onChange(false) }
            Text(
                "$amount",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(40.dp),
            )
            StepButton("+", "Прибавить: ${direction.label}", enabled = canIncrease) { onChange(true) }
        }
    }
}

@Composable
private fun StepButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (enabled) FinniColors.Lavender else FinniColors.Track,
        modifier = Modifier.size(48.dp).semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                symbol,
                style = MaterialTheme.typography.titleLarge,
                color = if (enabled) FinniColors.Navy else FinniColors.NavyMuted,
            )
        }
    }
}
