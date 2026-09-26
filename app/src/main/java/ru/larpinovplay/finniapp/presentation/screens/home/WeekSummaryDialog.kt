package ru.larpinovplay.finniapp.presentation.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import ru.larpinovplay.finniapp.domain.game.model.BudgetDirection
import ru.larpinovplay.finniapp.domain.game.model.WeekSummary
import ru.larpinovplay.finniapp.domain.game.model.Deed
import ru.larpinovplay.finniapp.domain.game.model.WeekDeeds
import ru.larpinovplay.finniapp.presentation.game.deedText
import ru.larpinovplay.finniapp.presentation.game.grewText
import ru.larpinovplay.finniapp.presentation.game.icon
import ru.larpinovplay.finniapp.presentation.game.label
import ru.larpinovplay.finniapp.presentation.game.title
import ru.larpinovplay.finniapp.presentation.pet.nextStageTitle
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * Итоги недели (ТЗ 2.5.9, 2.5.10): четыре дела недели с объяснением и сколько шагов роста они дали,
 * план против факта по каждому направлению, что стало с питомцем и сколько пришло на новую неделю. Отметки — значок и слово, не только цвет (ТЗ 3.6).
 * Закрывается только кнопкой: за итогами сразу идёт план новой недели.
 */
@Composable
fun WeekSummaryDialog(summary: WeekSummary, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        // Шире стандартного окна: строкам плана нужны подпись и две кнопки по 48 dp
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false),
        modifier = Modifier.padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White,
        title = { Text("Неделя ${summary.week}: итоги", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                SectionTitle("Дела недели: ${summary.steps} из ${WeekDeeds.MAX_STEPS}")
                Deed.entries.forEach { DeedLine(summary.deeds[it], it.title, summary.deedText(it)) }
                Text(
                    when {
                        summary.grew -> summary.grewText()
                        summary.stepsToNextStage != null ->
                            "+${summary.steps} ${stepsWord(summary.steps)} роста. До того как Финни станет " +
                                "${nextStageTitle(summary.stageAfter)}, ещё ${summary.stepsToNextStage}"
                        else -> "+${summary.steps} ${stepsWord(summary.steps)} роста"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = FinniColors.Blue,
                    modifier = Modifier.padding(top = 6.dp),
                )

                Spacer(Modifier.height(12.dp))
                SectionTitle("План и факт")
                BudgetDirection.entries.forEach { PlanFactRow(it, plan = summary.plan[it], fact = summary.fact(it)) }

                Spacer(Modifier.height(12.dp))
                if (summary.savingsBonus > 0) StatRow("Бонус копилки", "+${summary.savingsBonus} в копилку")
                StatRow("Настроение", if (summary.moodDelta >= 0) "+${summary.moodDelta}" else "${summary.moodDelta}")
                StatRow("На новую неделю", "+${summary.nextIncome} карманных")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(16.dp), modifier = Modifier.height(48.dp)) {
                Text("Составить план", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
}

/** «Необязательное: 20 → 25 ↑ больше плана». */
@Composable
private fun PlanFactRow(direction: BudgetDirection, plan: Int, fact: Int) {
    val (mark, word, color) = when {
        fact == plan -> Triple("✓", "по плану", FinniColors.Mood)
        fact > plan -> Triple("↑", "больше плана", if (direction == BudgetDirection.OPTIONAL) FinniColors.Warning else FinniColors.Blue)
        else -> Triple("↓", "меньше плана", if (direction == BudgetDirection.SAVINGS) FinniColors.Warning else FinniColors.Blue)
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(direction.icon), contentDescription = null, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(direction.label, style = MaterialTheme.typography.bodyLarge)
            Text("план $plan → факт $fact", style = MaterialTheme.typography.bodyMedium, color = FinniColors.NavyMuted)
        }
        Text("$mark $word", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

/** Дело недели: ✓ или ○ и слово, а не только цвет (ТЗ 3.6). */
@Composable
private fun DeedLine(done: Boolean, name: String, explanation: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text(
            if (done) "✓" else "○",
            style = MaterialTheme.typography.titleLarge,
            color = if (done) FinniColors.Mood else FinniColors.NavyMuted,
        )
        Column(Modifier.padding(start = 8.dp)) {
            Text(if (done) "$name — сделано" else "$name — не вышло", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(explanation, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** «1 шаг», «2 шага», «0 шагов». */
private fun stepsWord(n: Int): String = when {
    n % 10 == 1 && n % 100 != 11 -> "шаг"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "шага"
    else -> "шагов"
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = FinniColors.NavyMuted, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Bold)
    }
}
