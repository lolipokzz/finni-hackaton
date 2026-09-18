package ru.larpinovplay.finniapp.presentation.screens.progress

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.game.model.LedgerEntry
import ru.larpinovplay.finniapp.domain.game.model.TopicProgress
import ru.larpinovplay.finniapp.domain.game.model.WeekSummary
import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal
import ru.larpinovplay.finniapp.domain.pet.model.PetGrowthStage
import ru.larpinovplay.finniapp.presentation.components.RoomBackground
import ru.larpinovplay.finniapp.presentation.game.foodText
import ru.larpinovplay.finniapp.presentation.game.grewText
import ru.larpinovplay.finniapp.presentation.game.savedText
import ru.larpinovplay.finniapp.presentation.game.text
import ru.larpinovplay.finniapp.presentation.pet.title
import ru.larpinovplay.finniapp.presentation.screens.savings.icon
import ru.larpinovplay.finniapp.presentation.task.title
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

@Composable
fun ProgressScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    state?.let { ProgressScreenContent(state = it, onBack = onBack, modifier = modifier) }
}

@Composable
fun ProgressScreenContent(state: ProgressUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        RoomBackground()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Header(onBack) }
            item { PetStageCard(state) }
            item { GoalCard(state) }
            item { LastWeekCard(state.lastWeek, state.weeksCompleted) }
            item { TasksCard(state.taskTopics) }
            if (state.completedGoals.isNotEmpty()) item { CompletedGoalsCard(state.completedGoals) }
            item { LedgerCard(state.week, state.ledgerThisWeek) }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

// ---------- Карточки ----------

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(onClick = onBack, shape = CircleShape, color = FinniColors.Lavender, modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text("‹", style = MaterialTheme.typography.headlineMedium, color = FinniColors.Navy)
            }
        }
        Text("Прогресс", style = MaterialTheme.typography.headlineSmall)
    }
}

/** Стадия развития и очки роста до следующей (ТЗ 2.5.10). */
@Composable
private fun PetStageCard(state: ProgressUiState) {
    WhiteCard(color = FinniColors.CardMint) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBox(R.drawable.ic_star)
                Column(Modifier.padding(start = 12.dp)) {
                    Text("${state.pet.name}: ${state.pet.growthStage.title()}", style = MaterialTheme.typography.titleLarge)
                    Text("Неделя ${state.week} · пройдено недель: ${state.weeksCompleted}", style = MaterialTheme.typography.bodyMedium, color = FinniColors.NavyMuted)
                }
            }
            Spacer(Modifier.height(12.dp))
            val pet = state.pet
            val next = pet.pointsToNextStage
            if (next == null) {
                Text("Финни вырос до последней стадии. Так держать!", style = MaterialTheme.typography.bodyLarge)
            } else {
                val target = pet.growthPoints + next
                Bar(pet.growthPoints, target, FinniColors.Mood)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Очки роста: ${pet.growthPoints} из $target. Ещё $next — и Финни станет ${nextStageTitle(pet.growthStage)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Очки даются за неделю: купил еду +1, отложил в копилку +1. Стадия никогда не падает",
                style = MaterialTheme.typography.labelSmall,
                color = FinniColors.NavyMuted
            )
        }
    }
}

private fun nextStageTitle(stage: PetGrowthStage) = when (stage) {
    PetGrowthStage.BABY -> "Подростком"
    PetGrowthStage.TEEN -> "Взрослым"
    PetGrowthStage.ADULT -> ""
}

@Composable
private fun GoalCard(state: ProgressUiState) {
    WhiteCard(color = FinniColors.CardPink) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBox(state.goal?.icon ?: R.drawable.ic_pig)
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                val goal = state.goal
                if (goal == null) {
                    Text("Цель не выбрана", style = MaterialTheme.typography.titleMedium)
                    Text("В копилке ${state.savings}. Выбери цель в разделе «Копилка»", style = MaterialTheme.typography.bodyMedium, color = FinniColors.NavyMuted)
                } else {
                    Text(goal.name, style = MaterialTheme.typography.titleMedium)
                    Text("Накоплено ${state.savings} из ${goal.cost}, осталось ${(goal.cost - state.savings).coerceAtLeast(0)}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Bar(state.savings, goal.cost, FinniColors.Care)
                }
            }
        }
    }
}

/** Итоги последней недели: два критерия словами и значками, траты, настроение. */
@Composable
private fun LastWeekCard(summary: WeekSummary?, weeksCompleted: Int) {
    WhiteCard {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (summary == null) "Итоги недели" else "Итоги недели ${summary.week}",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))
            if (summary == null) {
                Text(
                    "Пока нет завершённых недель. Нажми «Завершить неделю» на главном экране, когда сделаешь все дела",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FinniColors.NavyMuted
                )
                return@Column
            }
            CriterionLine(summary.foodCovered, summary.foodText())
            CriterionLine(summary.savedSomething, summary.savedText())
            Spacer(Modifier.height(8.dp))
            StatLine("Потрачено на нужное", "${summary.spentMandatory}")
            StatLine("Потрачено на желаемое", "${summary.spentOptional}")
            StatLine("Отложено", "${summary.saved}")
            StatLine("Настроение", if (summary.moodDelta >= 0) "+${summary.moodDelta}" else "${summary.moodDelta}")
            StatLine("Оценка недели", "${summary.score} из 2")
            if (summary.stageAfter != summary.stageBefore) {
                Spacer(Modifier.height(6.dp))
                Text("★ ${summary.grewText()}", style = MaterialTheme.typography.bodyLarge, color = FinniColors.Blue)
            }
            if (weeksCompleted > 1) {
                Spacer(Modifier.height(6.dp))
                Text("Всего недель пройдено: $weeksCompleted", style = MaterialTheme.typography.labelSmall, color = FinniColors.NavyMuted)
            }
        }
    }
}

/** Выполненные задания по темам (ТЗ 2.5.8, 2.5.11). */
@Composable
private fun TasksCard(topics: List<TopicProgress>) {
    WhiteCard {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBox(R.drawable.ic_target)
                Column(Modifier.padding(start = 12.dp)) {
                    Text("Задания", style = MaterialTheme.typography.titleLarge)
                    Text("Выполнено ${topics.sumOf { it.done }} из ${topics.sumOf { it.total }}", style = MaterialTheme.typography.bodyMedium, color = FinniColors.NavyMuted)
                }
            }
            Spacer(Modifier.height(10.dp))
            topics.forEach { topic ->
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(topic.topic.title(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text("${topic.done} из ${topic.total}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Bar(topic.done, topic.total, FinniColors.Blue)
            }
        }
    }
}

@Composable
private fun CompletedGoalsCard(goals: List<SavingsGoal>) {
    WhiteCard(color = FinniColors.Sunny) {
        Column(Modifier.padding(16.dp)) {
            Text("Достигнутые цели", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            goals.forEach { goal ->
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(goal.icon), null, Modifier.size(28.dp))
                    Text(goal.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 10.dp).weight(1f))
                    Text("${goal.cost}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Image(painterResource(R.drawable.ic_coin), null, Modifier.padding(start = 4.dp).size(18.dp))
                }
            }
        }
    }
}

/** Журнал монет: у каждого движения есть название и сумма (ТЗ 2.5.4). */
@Composable
private fun LedgerCard(week: Int, entries: List<LedgerEntry>) {
    WhiteCard {
        Column(Modifier.padding(16.dp)) {
            Text("Откуда и куда монеты · неделя $week", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            if (entries.isEmpty()) {
                Text("Пока ничего не происходило", style = MaterialTheme.typography.bodyMedium, color = FinniColors.NavyMuted)
            }
            entries.asReversed().forEach { e ->
                Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(ledgerIcon(e)), null, Modifier.size(26.dp))
                    Text(e.reason.text(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 10.dp).weight(1f))
                    val (text, color) = when {
                        e.balanceDelta > 0 -> "+${e.balanceDelta}" to FinniColors.Mood
                        e.balanceDelta < 0 && e.savingsDelta > 0 -> "${e.balanceDelta} → копилка" to FinniColors.Blue
                        e.balanceDelta < 0 -> "${e.balanceDelta}" to FinniColors.Coral
                        else -> "из копилки ${e.savingsDelta}" to FinniColors.Blue
                    }
                    Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = color)
                }
            }
        }
    }
}

@DrawableRes
private fun ledgerIcon(e: LedgerEntry): Int = when {
    e.savingsDelta != 0 -> R.drawable.ic_pig
    e.balanceDelta > 0 -> R.drawable.ic_coin
    else -> R.drawable.ic_cart
}

// ---------- Мелкие элементы ----------

@Composable
private fun CriterionLine(ok: Boolean, text: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Text(if (ok) "✓" else "○", style = MaterialTheme.typography.titleMedium, color = if (ok) FinniColors.Mood else FinniColors.NavyMuted)
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = FinniColors.NavyMuted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Bar(value: Int, max: Int, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(FinniColors.Track)
    ) {
        Box(
            Modifier
                .fillMaxWidth(if (max <= 0) 0f else (value.toFloat() / max).coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(color, RoundedCornerShape(5.dp))
        )
    }
}

@Composable
private fun IconBox(@DrawableRes icon: Int) {
    Box(
        Modifier
            .size(52.dp)
            .background(Color.White, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) { Image(painterResource(icon), null, Modifier.size(34.dp)) }
}

@Composable
private fun WhiteCard(color: Color = FinniColors.Card, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, shape, ambientColor = FinniColors.Navy.copy(alpha = 0.15f), spotColor = FinniColors.Navy.copy(alpha = 0.15f)),
        shape = shape,
        color = color,
        content = content
    )
}
