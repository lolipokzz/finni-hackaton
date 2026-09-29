package ru.larpinovplay.finniapp.presentation.screens.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.game.model.Deed
import ru.larpinovplay.finniapp.domain.game.model.LedgerEntry
import ru.larpinovplay.finniapp.domain.game.model.TopicProgress
import ru.larpinovplay.finniapp.domain.game.model.WeekDeeds
import ru.larpinovplay.finniapp.domain.game.model.WeekSummary
import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal
import ru.larpinovplay.finniapp.domain.pet.model.PetGrowthStage
import ru.larpinovplay.finniapp.presentation.components.CardSticker
import ru.larpinovplay.finniapp.presentation.components.DashedDivider
import ru.larpinovplay.finniapp.presentation.components.DoneBadge
import ru.larpinovplay.finniapp.presentation.components.MeterBar
import ru.larpinovplay.finniapp.presentation.components.Paws
import ru.larpinovplay.finniapp.presentation.components.RoomBackground
import ru.larpinovplay.finniapp.presentation.components.ScreenHeader
import ru.larpinovplay.finniapp.presentation.components.StatRow
import ru.larpinovplay.finniapp.presentation.components.creamCard
import ru.larpinovplay.finniapp.presentation.components.growth
import ru.larpinovplay.finniapp.presentation.game.deedText
import ru.larpinovplay.finniapp.presentation.game.grewText
import ru.larpinovplay.finniapp.presentation.game.text
import ru.larpinovplay.finniapp.presentation.pet.nextStageTitle
import ru.larpinovplay.finniapp.presentation.pet.title
import ru.larpinovplay.finniapp.presentation.screens.home.short
import ru.larpinovplay.finniapp.presentation.screens.home.sticker
import ru.larpinovplay.finniapp.presentation.screens.savings.icon
import ru.larpinovplay.finniapp.presentation.screens.tasks.sticker
import ru.larpinovplay.finniapp.presentation.task.title
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * Прогресс (ТЗ 2.5.10, 2.5.11): как растёт Финни, цель, итоги прошлой недели, задания по темам и журнал монет.
 * Тот же язык наклеек, что у главного экрана: те же наклейки дел, лапки роста и кольцевые шкалы-полоски.
 */
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
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
            Spacer(Modifier.height(10.dp))
            ScreenHeader("Прогресс", onBack)
            Spacer(Modifier.height(14.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
                modifier = Modifier.weight(1f),
            ) {
                item { GrowthCard(state) }
                item { DreamCard(state) }
                item { LastWeekCard(state.lastWeek, state.weeksCompleted) }
                item { TasksCard(state.taskTopics) }
                if (state.completedGoals.isNotEmpty()) item { CompletedDreamsCard(state.completedGoals) }
                item { LedgerCard(state.week, state.ledgerThisWeek) }
            }
        }
    }
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .creamCard(RoundedCornerShape(30.dp), elevation = 8.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun CardHeading(title: String, subtitle: String? = null) {
    Column {
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = FinniColors.Ink, modifier = Modifier.semantics { heading() })
        subtitle?.let { Text(it, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = FinniColors.Teal) }
    }
}

// ---------- Рост ----------

/**
 * Как растёт Финни: лесенка из трёх стадий (лапка растёт вместе с ним), лапки шагов до следующей
 * и напоминание, откуда берутся шаги.
 */
@Composable
private fun GrowthCard(state: ProgressUiState) {
    val pet = state.pet
    Card {
        StageLadder(pet.growthStage)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(pet.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = FinniColors.Ink, modifier = Modifier.semantics { heading() })
            Text(
                "${pet.growthStage.title()} · неделя ${state.week}",
                fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = FinniColors.Teal,
            )
        }
        val growth = pet.growth(0)
        if (growth == null) {
            Text(
                "Финни вырос до последней стадии. Так держать!",
                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(FinniColors.CardMint).padding(12.dp)
                    .clearAndSetSemantics {
                        contentDescription = "Шагов роста ${growth.earned} из ${growth.total}. Ещё ${growth.left} — и Финни станет ${nextStageTitle(pet.growthStage)}"
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Paws(growth, size = 24.dp)
                Text(
                    "Ещё ${growth.left} — и Финни станет ${nextStageTitle(pet.growthStage)}",
                    fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF0B5E4F), textAlign = TextAlign.Center,
                )
            }
        }
        Text(
            "Каждое дело недели — шаг: Финни сыт, не скучает, копилка и траты по плану. Шаги не пропадают, а новая стадия даёт больше карманных.",
            style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted,
        )
    }
}

/** Три стадии на пунктирной тропинке: пройденные — с галочкой, текущая — крупнее, впереди — бледнее. */
@Composable
private fun StageLadder(current: PetGrowthStage) {
    Box(
        Modifier.fillMaxWidth().height(92.dp).clearAndSetSemantics { contentDescription = "Стадия: ${current.title()}" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxWidth().padding(horizontal = 48.dp).height(4.dp).offset(y = (-10).dp)) {
            drawLine(
                FinniColors.Dashed, Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
                strokeWidth = size.height, pathEffect = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 6.dp.toPx())),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            PetGrowthStage.entries.forEach { stage ->
                val passed = stage.order < current.order
                val now = stage == current
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.size(width = 96.dp, height = 92.dp)) {
                    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                        val size: Dp = if (now) 64.dp else 50.dp
                        Box(
                            Modifier.size(size)
                                .then(if (now) Modifier.creamCard(CircleShape, elevation = 6.dp, border = 3.dp) else Modifier.clip(CircleShape))
                                .background(if (now || passed) FinniColors.CardMint else FinniColors.Pebble),
                            contentAlignment = Alignment.Center,
                        ) {
                            // Лапка растёт вместе с Финни: у малыша маленькая, у взрослого большая
                            Image(
                                painterResource(R.drawable.ic_paw), null, Modifier.size(size * (0.36f + 0.1f * stage.order)),
                                colorFilter = ColorFilter.tint(if (now || passed) FinniColors.TealBright else FinniColors.PawEmpty),
                            )
                        }
                        if (passed) DoneBadge(Modifier.align(Alignment.BottomEnd).offset(x = (-4).dp, y = (-4).dp), size = 22.dp)
                    }
                    Text(
                        stage.title(), fontSize = 13.sp, fontWeight = FontWeight.Black,
                        color = if (now) FinniColors.Teal else FinniColors.InkMuted,
                    )
                }
            }
        }
    }
}

// ---------- Цель ----------

@Composable
private fun DreamCard(state: ProgressUiState) {
    val goal = state.goal
    Card {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CardSticker(goal?.icon ?: R.drawable.ic_deed_pig, FinniColors.DreamTint, size = 64.dp, iconScale = 0.62f)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (goal == null) {
                    Text("Цель не выбрана", fontSize = 17.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink)
                    Text("В копилке ${state.savings}. Выбери цель в «Копилке»", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(goal.name, fontSize = 17.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink, modifier = Modifier.weight(1f))
                        Image(painterResource(R.drawable.ic_coin), null, Modifier.size(20.dp))
                        Text(" ${state.savings}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = FinniColors.CoinInk)
                        Text(" из ${goal.cost}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
                    }
                    MeterBar(state.savings.toFloat() / goal.cost, FinniColors.DreamRing)
                    Text(
                        "Осталось ${state.goalRemaining ?: 0}",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted,
                    )
                }
            }
        }
    }
}

// ---------- Итоги недели ----------

/** Итоги последней недели: четыре дела наклейками, что получилось словами, траты и настроение. */
@Composable
private fun LastWeekCard(summary: WeekSummary?, weeksCompleted: Int) {
    Card {
        CardHeading(
            if (summary == null) "Итоги недели" else "Итоги недели ${summary.week}",
            summary?.let { "Сделано ${it.steps} из ${WeekDeeds.MAX_STEPS} · всего недель: $weeksCompleted" },
        )
        if (summary == null) {
            Text(
                "Пока нет завершённых недель. Когда сделаешь дела, нажми на солнышко на главном экране",
                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted,
            )
            return@Card
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Deed.entries.forEach { deed -> DeedMark(deed, summary.deeds[deed]) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Deed.entries.forEach { deed ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.padding(top = 6.dp).size(8.dp).clip(CircleShape)
                            .background(if (summary.deeds[deed]) FinniColors.TealBright else FinniColors.DeedPending),
                    )
                    Text(summary.deedText(deed), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
                }
            }
        }
        DashedDivider()
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            StatRow("На обязательное", "${summary.spentMandatory}", coin = true)
            StatRow("На необязательное", "${summary.spentOptional}", coin = true)
            StatRow("Отложено", "${summary.saved}", coin = true)
            StatRow("Настроение", if (summary.moodDelta >= 0) "+${summary.moodDelta}" else "${summary.moodDelta}")
        }
        if (summary.grew) {
            Text(
                summary.grewText(), fontSize = 15.sp, fontWeight = FontWeight.Black, color = FinniColors.Teal,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(FinniColors.CardMint).padding(12.dp),
            )
        }
    }
}

/** Дело на итогах: наклейка с галочкой или пунктирное место, подпись — одно слово. */
@Composable
private fun DeedMark(deed: Deed, done: Boolean) {
    val (icon, tint) = deed.sticker
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clearAndSetSemantics { contentDescription = "${deed.short}: " + if (done) "получилось" else "не вышло" },
    ) {
        Box {
            if (done) {
                CardSticker(icon, tint, size = 52.dp, iconScale = 0.56f)
                DoneBadge(Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 3.dp), size = 22.dp)
            } else {
                Box(Modifier.size(52.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(52.dp)) {
                        val w = 2.5.dp.toPx()
                        drawCircle(
                            FinniColors.DeedPending, radius = size.minDimension / 2 - w / 2,
                            style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                        )
                    }
                    Image(painterResource(icon), null, Modifier.size(26.dp))
                }
            }
        }
        Text(deed.short, fontSize = 13.sp, fontWeight = FontWeight.Black, color = if (done) FinniColors.Ink else FinniColors.WarnInk)
    }
}

// ---------- Задания ----------

/** Выполненные задания по темам (ТЗ 2.5.8, 2.5.11): наклейка темы, шкала и число. */
@Composable
private fun TasksCard(topics: List<TopicProgress>) {
    Card {
        CardHeading("Задания", "Пройдено уровней: ${topics.sumOf { it.done }} из ${topics.sumOf { it.total }}")
        topics.forEach { topic ->
            val (icon, tint) = topic.topic.sticker
            val name = topic.topic.title()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.clearAndSetSemantics { contentDescription = "$name: выполнено ${topic.done} из ${topic.total}" },
            ) {
                CardSticker(icon, tint, size = 44.dp, iconScale = 0.58f)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row {
                        Text(name, fontSize = 15.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink, modifier = Modifier.weight(1f))
                        Text("${topic.done} из ${topic.total}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = FinniColors.InkMuted)
                    }
                    MeterBar(if (topic.total == 0) 0f else topic.done.toFloat() / topic.total, FinniColors.TealBright)
                }
            }
        }
    }
}

// ---------- Достигнутые цели ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompletedDreamsCard(goals: List<SavingsGoal>) {
    Card {
        CardHeading("Достигнутые цели", "Уже у Финни: ${goals.size}")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            goals.forEach { goal ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box {
                        CardSticker(goal.icon, FinniColors.DreamTint, size = 64.dp, iconScale = 0.62f)
                        DoneBadge(Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 3.dp), size = 24.dp)
                    }
                    Text(
                        goal.name, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink,
                        textAlign = TextAlign.Center, minLines = 2, maxLines = 2, modifier = Modifier.width(92.dp),
                    )
                }
            }
        }
    }
}

// ---------- Журнал монет ----------

/** Журнал монет: у каждого движения есть название и сумма (ТЗ 2.5.4). Пришло, ушло и в копилку — разными пилюлями. */
@Composable
private fun LedgerCard(week: Int, entries: List<LedgerEntry>) {
    Card {
        CardHeading("Монеты этой недели", "Неделя $week")
        if (entries.isEmpty()) {
            Text("Пока ничего не происходило", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
        }
        entries.asReversed().forEach { e ->
            val (icon, tint) = ledgerSticker(e)
            val (text, bg, ink) = when {
                e.balanceDelta > 0 -> Triple("+${e.balanceDelta}", FinniColors.CardMint, FinniColors.Teal)
                e.balanceDelta < 0 && e.savingsDelta > 0 -> Triple("${e.balanceDelta} в копилку", FinniColors.DreamTint, Color(0xFFA23566))
                e.balanceDelta < 0 -> Triple("${e.balanceDelta}", FinniColors.Pebble, FinniColors.Ink)
                else -> Triple("из копилки ${e.savingsDelta}", FinniColors.DreamTint, Color(0xFFA23566))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CardSticker(icon, tint, size = 36.dp, iconScale = 0.6f)
                Text(e.reason.text(), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FinniColors.Ink, modifier = Modifier.weight(1f))
                Text(
                    text, fontSize = 14.sp, fontWeight = FontWeight.Black, color = ink,
                    modifier = Modifier.clip(CircleShape).background(bg).padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

private fun ledgerSticker(e: LedgerEntry): Pair<Int, Color> = when {
    e.savingsDelta != 0 -> R.drawable.ic_deed_pig to FinniColors.DreamTint
    e.balanceDelta > 0 -> R.drawable.ic_coin to FinniColors.CoinPill
    else -> R.drawable.ic_nav_shop to Color(0xFFFFF0E6)
}
