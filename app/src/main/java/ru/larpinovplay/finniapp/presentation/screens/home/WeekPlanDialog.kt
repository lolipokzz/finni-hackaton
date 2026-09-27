package ru.larpinovplay.finniapp.presentation.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.content.FeedbackKey
import ru.larpinovplay.finniapp.domain.game.model.BudgetDirection
import ru.larpinovplay.finniapp.domain.game.model.Deed
import ru.larpinovplay.finniapp.presentation.components.CardDialog
import ru.larpinovplay.finniapp.presentation.components.CardSticker
import ru.larpinovplay.finniapp.presentation.components.CardTitle
import ru.larpinovplay.finniapp.presentation.components.DashedDivider
import ru.larpinovplay.finniapp.presentation.components.StepButton
import ru.larpinovplay.finniapp.presentation.components.TealButton
import ru.larpinovplay.finniapp.presentation.feedback.LocalFeedback
import ru.larpinovplay.finniapp.presentation.game.label
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * План недели (ТЗ 2.5.5): в начале недели ребёнок раскладывает все монеты — карманные и остаток —
 * по трём направлениям. Закрыть окно, не составив план, нельзя: без плана неделя не начинается.
 * Слишком мало на обязательное — только предупреждение, решает ребёнок.
 *
 * Та же карточка, что у дел недели: кремовая наклейка, те же наклейки-кружки и бирюзовая кнопка.
 * Направления носят наклейки дел, к которым ведут: обязательное — еда, необязательное — радость, копилка — копилка.
 */
@Composable
fun WeekPlanDialog(
    draft: HomeUiState.PlanDraft,
    onChange: (BudgetDirection, increase: Boolean) -> Unit,
    onConfirm: () -> Unit,
) {
    val feedback = LocalFeedback.current
    CardDialog {
        CardTitle("План недели", "Неделя ${draft.week} · у тебя ${draft.budget} монет")

        SplitBar(draft)

        BudgetDirection.entries.forEachIndexed { index, direction ->
            if (index > 0) DashedDivider()
            val hint = when {
                direction == BudgetDirection.MANDATORY && draft.need > 0 ->
                    // Предупреждение отличается словами и значком «!», а не только цветом
                    if (draft.mandatoryLow) "! " + feedback.text(FeedbackKey.PLAN_NEED_LOW, "n" to draft.need)
                    else feedback.text(FeedbackKey.PLAN_NEED, "n" to draft.need)
                direction == BudgetDirection.SAVINGS && draft.plan.savings > 0 -> feedback.text(FeedbackKey.PLAN_SAVINGS_NOW)
                else -> null
            }
            PlanRow(
                direction = direction,
                amount = draft.plan[direction],
                hint = hint,
                warning = direction == BudgetDirection.MANDATORY && draft.mandatoryLow,
                canIncrease = draft.unallocated > 0,
                onChange = { increase -> onChange(direction, increase) },
            )
        }

        TealButton("Начать неделю", R.drawable.ic_sun_small, onConfirm, enabled = draft.unallocated == 0)
    }
}

/** Наклейка дела, к которому ведёт направление, и её цвет на полоске. */
internal val BudgetDirection.deed: Deed
    get() = when (this) {
        BudgetDirection.MANDATORY -> Deed.FED
        BudgetDirection.OPTIONAL -> Deed.NOT_BORED
        BudgetDirection.SAVINGS -> Deed.SAVINGS_ON_PLAN
    }

internal val BudgetDirection.barColor: Color
    get() = when (this) {
        BudgetDirection.MANDATORY -> FinniColors.SatietyRing
        BudgetDirection.OPTIONAL -> Color(0xFFFFC83D)
        BudgetDirection.SAVINGS -> FinniColors.DreamRing
    }

/**
 * Полоска из цветов направлений и пустого остатка, справа — сколько ещё разложить.
 * Когда разложено всё, вместо числа — галочка.
 */
@Composable
private fun SplitBar(draft: HomeUiState.PlanDraft) {
    val budget = draft.budget.coerceAtLeast(1)
    val left = draft.unallocated
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = if (left == 0) "Все монеты разложены" else "Осталось разложить: $left"
        },
    ) {
        Row(Modifier.weight(1f).height(16.dp).clip(CircleShape).background(FinniColors.Dashed)) {
            BudgetDirection.entries.forEach { direction ->
                val share by animateFloatAsState(draft.plan[direction].toFloat() / budget, label = "share")
                if (share > 0f) Box(Modifier.weight(share).fillMaxHeight().background(direction.barColor))
            }
            if (left > 0) Spacer(Modifier.weight(left.toFloat() / budget))
        }
        if (left == 0) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(FinniColors.Teal).border(3.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Image(painterResource(R.drawable.ic_check), null, Modifier.size(14.dp)) }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Image(painterResource(R.drawable.ic_coin), null, Modifier.size(24.dp))
                Text("$left", fontSize = 20.sp, fontWeight = FontWeight.Black, color = FinniColors.CoinInk)
            }
        }
    }
}

/** Строка направления: наклейка, название и подсказка; под ними «−», сумма и «+». */
@Composable
private fun PlanRow(
    direction: BudgetDirection,
    amount: Int,
    hint: String?,
    warning: Boolean,
    canIncrease: Boolean,
    onChange: (increase: Boolean) -> Unit,
) {
    val (icon, tint) = direction.deed.sticker
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CardSticker(icon, tint, size = 48.dp)
            Column(Modifier.weight(1f)) {
                Text(direction.label, fontSize = 16.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink)
                hint?.let {
                    Text(
                        it, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold,
                        color = if (warning) Color(0xFFB4471B) else FinniColors.InkMuted,
                    )
                }
            }
        }
        Row(Modifier.padding(start = 60.dp), verticalAlignment = Alignment.CenterVertically) {
            StepButton("−", "Убавить: ${direction.label}", enabled = amount > 0) { onChange(false) }
            Text(
                "$amount",
                fontSize = 22.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(64.dp).semantics { contentDescription = "${direction.label}: $amount" },
            )
            StepButton("+", "Прибавить: ${direction.label}", enabled = canIncrease) { onChange(true) }
        }
    }
}
