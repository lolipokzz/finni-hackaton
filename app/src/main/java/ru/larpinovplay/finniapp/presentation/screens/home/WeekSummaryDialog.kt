package ru.larpinovplay.finniapp.presentation.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.game.model.BudgetDirection
import ru.larpinovplay.finniapp.domain.game.model.Deed
import ru.larpinovplay.finniapp.domain.game.model.WeekDeeds
import ru.larpinovplay.finniapp.domain.game.model.WeekSummary
import ru.larpinovplay.finniapp.presentation.game.deedText
import ru.larpinovplay.finniapp.presentation.game.grewText
import ru.larpinovplay.finniapp.presentation.game.label
import ru.larpinovplay.finniapp.presentation.game.title
import ru.larpinovplay.finniapp.presentation.pet.nextStageTitle
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * Итоги недели (ТЗ 2.5.9, 2.5.10): четыре дела недели с объяснением и сколько шагов роста они дали,
 * план против факта по каждому направлению, что стало с питомцем и сколько пришло на новую неделю.
 * Отметки — значок и слово, не только цвет (ТЗ 3.6). Закрывается только кнопкой: за итогами сразу идёт план.
 *
 * Та же карточка, что у дел недели и плана: наклейки дел, лапки роста, бирюзовая кнопка.
 */
@Composable
fun WeekSummaryDialog(summary: WeekSummary, onDismiss: () -> Unit) {
    CardDialog {
        CardTitle(
            if (summary.steps == WeekDeeds.MAX_STEPS) "Отличная неделя!" else "Итоги недели",
            "Неделя ${summary.week} · сделано ${summary.steps} из ${WeekDeeds.MAX_STEPS}",
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Deed.entries.forEach { DeedLine(it, summary.deeds[it], summary.deedText(it)) }
        }

        GrowthBlock(summary)

        DashedDivider()
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("План и факт", fontSize = 15.sp, fontWeight = FontWeight.Black, color = FinniColors.InkMuted)
            BudgetDirection.entries.forEach { PlanFactRow(it, plan = summary.plan[it], fact = summary.fact(it)) }
        }

        DashedDivider()
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (summary.savingsBonus > 0) StatRow("Бонус копилки", "+${summary.savingsBonus} в копилку")
            StatRow("Настроение", if (summary.moodDelta >= 0) "+${summary.moodDelta}" else "${summary.moodDelta}")
            StatRow("На новую неделю", "+${summary.nextIncome}", coin = true)
        }

        TealButton("Составить план", R.drawable.ic_sun_small, onDismiss)
    }
}

/** Дело недели: наклейка с галочкой или пунктирное место под неё, название и объяснение словами. */
@Composable
private fun DeedLine(deed: Deed, done: Boolean, explanation: String) {
    val (icon, tint) = deed.sticker
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = "${deed.title}: " + (if (done) "сделано. " else "не вышло. ") + explanation
        },
    ) {
        Box {
            if (done) {
                CardSticker(icon, tint, size = 44.dp)
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 3.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(FinniColors.Teal)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Image(painterResource(R.drawable.ic_check), null, Modifier.size(10.dp)) }
            } else {
                Box(Modifier.size(44.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(44.dp)) {
                        val w = 2.5.dp.toPx()
                        drawCircle(
                            FinniColors.DeedPending,
                            radius = size.minDimension / 2 - w / 2,
                            style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                        )
                    }
                    Image(painterResource(icon), null, Modifier.size(24.dp))
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(deed.title, fontSize = 15.sp, fontWeight = FontWeight.Black, color = if (done) FinniColors.Ink else Color(0xFFB4471B))
            Text(explanation, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
        }
    }
}

/** Лапки роста: сколько шагов дала неделя и сколько осталось до следующей стадии. Вырос — праздник. */
@Composable
private fun GrowthBlock(summary: WeekSummary) {
    val left = summary.stepsToNextStage
    val next = summary.stageAfter.next
    val text = when {
        summary.grew -> summary.grewText()
        left != null -> "+${summary.steps} ${stepsWord(summary.steps)} роста. До того как Финни станет " +
            "${nextStageTitle(summary.stageAfter)}, ещё $left"
        else -> "+${summary.steps} ${stepsWord(summary.steps)} роста"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(FinniColors.CardMint)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // После недели шаги этой недели уже засчитаны: жёлтые — только что заработанные
        if (!summary.grew && left != null && next != null) {
            val total = next.minGrowthPoints - summary.stageAfter.minGrowthPoints
            val fresh = summary.steps.coerceAtMost(total - left)
            Paws(Growth(total, earned = total - left - fresh, fresh = fresh), size = 16.dp)
        }
        Text(text, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold, color = FinniColors.Teal, modifier = Modifier.weight(1f))
    }
}

/** «Необязательное · план 20 → факт 25» и отметка словами. */
@Composable
private fun PlanFactRow(direction: BudgetDirection, plan: Int, fact: Int) {
    val warn = (direction == BudgetDirection.OPTIONAL && fact > plan) || (direction == BudgetDirection.SAVINGS && fact < plan)
    val (word, background, ink) = when {
        fact == plan -> Triple("✓ по плану", FinniColors.CardMint, FinniColors.Teal)
        warn -> Triple(if (fact > plan) "↑ больше плана" else "↓ меньше плана", Color(0xFFFFF0E6), Color(0xFFB4471B))
        else -> Triple(if (fact > plan) "↑ больше плана" else "↓ меньше плана", FinniColors.Pebble, FinniColors.InkMuted)
    }
    val (icon, tint) = direction.deed.sticker
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CardSticker(icon, tint, size = 40.dp)
        Column(Modifier.weight(1f)) {
            Text(direction.label, fontSize = 15.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink)
            Text("план $plan → факт $fact", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
        }
        Text(
            word, fontSize = 13.sp, fontWeight = FontWeight.Black, color = ink,
            modifier = Modifier.clip(CircleShape).background(background).padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** «1 шаг», «2 шага», «0 шагов». */
private fun stepsWord(n: Int): String = when {
    n % 10 == 1 && n % 100 != 11 -> "шаг"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "шага"
    else -> "шагов"
}

@Composable
private fun StatRow(label: String, value: String, coin: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted, modifier = Modifier.weight(1f))
        if (coin) Image(painterResource(R.drawable.ic_coin), null, Modifier.padding(end = 4.dp).size(20.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = if (coin) FinniColors.CoinInk else FinniColors.Ink)
    }
}
