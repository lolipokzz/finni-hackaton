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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import ru.larpinovplay.finniapp.presentation.components.CoachNote
import ru.larpinovplay.finniapp.presentation.components.DashedDivider
import ru.larpinovplay.finniapp.presentation.components.MeterBar
import ru.larpinovplay.finniapp.presentation.components.StepButton
import ru.larpinovplay.finniapp.presentation.components.TealButton
import ru.larpinovplay.finniapp.presentation.feedback.LocalFeedback
import ru.larpinovplay.finniapp.presentation.game.label
import ru.larpinovplay.finniapp.presentation.theme.FinniColors
import kotlin.math.roundToInt

/** План недели: в начале недели ребёнок раскладывает все монеты — карманные и остаток — по трём направлениям. */
@Composable
fun WeekPlanDialog(
    draft: HomeUiState.PlanDraft,
    onChange: (BudgetDirection, increase: Boolean) -> Unit,
    onSet: (BudgetDirection, amount: Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    coach: Boolean = false,
) {
    val feedback = LocalFeedback.current
    CardDialog(onDismiss = onDismiss) {
        CardTitle("План недели", "Неделя ${draft.week} · у тебя ${draft.budget} монет")
        if (coach) {
            // Обучение: зачем вообще план — коротко, до первого решения
            CoachNote(
                "Монет немного, а хочется всего. План — чтобы хватило и на еду, и на радости, и на цель. " +
                    "Начни с еды: положи на обязательное хотя бы ${draft.need}.",
            )
        }

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
                budget = draft.budget,
                canIncrease = draft.unallocated > 0,
                onChange = { increase -> onChange(direction, increase) },
                onSet = { amount -> onSet(direction, amount) },
            )
        }

        TealButton("Начать неделю", R.drawable.ic_sun_small, onConfirm, enabled = draft.unallocated == 0)
    }
}

/**
 * План идущей недели по кнопке «План»: что решили в начале недели и сколько уже ушло по каждому направлению. Только
 * смотреть — план меняется в начале следующей недели.
 */
@Composable
fun ActivePlanDialog(active: HomeUiState.ActivePlan, onDismiss: () -> Unit) {
    CardDialog(onDismiss = onDismiss) {
        CardTitle("План недели", "Неделя ${active.week} · решили в начале недели")
        BudgetDirection.entries.forEachIndexed { index, direction ->
            if (index > 0) DashedDivider()
            ActivePlanRow(direction, planned = active.plan[direction], used = active.used[direction])
        }
        Text(
            "Новый план составим в начале следующей недели",
            style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        TealButton("Понятно", R.drawable.ic_check, onDismiss)
    }
}

@Composable
private fun ActivePlanRow(direction: BudgetDirection, planned: Int, used: Int) {
    val (icon, tint) = direction.deed.sticker
    val saving = direction == BudgetDirection.SAVINGS
    // У трат плохо — выйти за план, у копилки — отложить меньше плана (например, забрать обратно)
    val warning = if (saving) used < planned else used > planned
    val text = when {
        saving -> "Отложено $used из $planned"
        used > planned -> "! Потрачено $used из $planned — сверх плана на ${used - planned}"
        else -> "Потрачено $used из $planned · осталось ${planned - used}"
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CardSticker(icon, tint, size = 48.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(direction.label, fontSize = 16.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink)
            MeterBar(
                fraction = if (planned > 0) used.toFloat() / planned else if (used > 0) 1f else 0f,
                color = if (warning && !saving) FinniColors.WarnInk else direction.barColor,
            )
            Text(
                (if (warning && saving) "! " else "") + text,
                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold,
                color = if (warning) Color(0xFFB4471B) else FinniColors.InkMuted,
            )
        }
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

/**
 * Строка направления: наклейка, название с подсказкой и сумма; под ними ползунок между «−» и «+». Ползунком — быстро и
 * крупно, кнопками — точно, по шагу.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanRow(
    direction: BudgetDirection,
    amount: Int,
    hint: String?,
    warning: Boolean,
    budget: Int,
    canIncrease: Boolean,
    onChange: (increase: Boolean) -> Unit,
    onSet: (amount: Int) -> Unit,
) {
    val (icon, tint) = direction.deed.sticker
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CardSticker(icon, tint, size = 48.dp)
            Column(Modifier.weight(1f)) {
                Text(direction.label, fontSize = 16.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink)
                hint?.let {
                    Text(
                        it, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold,
                        color = if (warning) Color(0xFFB4471B) else FinniColors.InkMuted,
                    )
                }
            }
            Text(
                "$amount",
                fontSize = 24.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(min = 48.dp).clearAndSetSemantics {},
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StepButton("−", "Убавить: ${direction.label}", enabled = amount > 0) { onChange(false) }
            Slider(
                value = amount.toFloat(),
                onValueChange = { onSet(it.roundToInt()) },
                valueRange = 0f..budget.coerceAtLeast(1).toFloat(),
                modifier = Modifier.weight(1f).semantics { contentDescription = "${direction.label}: $amount" },
                // Та же шкала, что в карточках: бежевая дорожка, цвет направления; ручка — белая наклейка
                thumb = {
                    Box(
                        Modifier
                            .size(30.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(6.dp, direction.barColor, CircleShape),
                    )
                },
                track = { MeterBar(amount.toFloat() / budget.coerceAtLeast(1), direction.barColor, height = 12.dp) },
            )
            StepButton("+", "Прибавить: ${direction.label}", enabled = canIncrease) { onChange(true) }
        }
    }
}
