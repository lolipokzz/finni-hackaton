package ru.larpinovplay.finniapp.presentation.screens.tasks

import androidx.compose.foundation.lazy.rememberLazyListState
import ru.larpinovplay.finniapp.presentation.components.spotlightTarget
import ru.larpinovplay.finniapp.presentation.components.TutorialSpotlight
import ru.larpinovplay.finniapp.presentation.components.SpotlightTargets
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.adventure.model.Adventure
import ru.larpinovplay.finniapp.domain.game.model.TaskStatus
import ru.larpinovplay.finniapp.domain.task.model.Task
import ru.larpinovplay.finniapp.domain.task.model.TaskPayload
import ru.larpinovplay.finniapp.domain.task.model.TaskTopic
import ru.larpinovplay.finniapp.presentation.adventure.AdventureSky
import ru.larpinovplay.finniapp.presentation.adventure.look
import ru.larpinovplay.finniapp.presentation.components.ScreenHeader
import ru.larpinovplay.finniapp.presentation.components.CardSticker
import ru.larpinovplay.finniapp.presentation.components.OnRoomLabel
import ru.larpinovplay.finniapp.presentation.components.RoomBackground
import ru.larpinovplay.finniapp.presentation.components.TealButton
import ru.larpinovplay.finniapp.presentation.components.creamCard
import ru.larpinovplay.finniapp.presentation.task.title
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * Задания (ТЗ 2.5.8): сверху — приключение недели, большое и яркое, со своим небом и эмблемой:
 * без него неделю не закончить. Ниже — задания по темам. Статус задания — словом, не только цветом.
 * Само прохождение и итог — отдельные маршруты TaskPlay и TaskResult.
 */
@Composable
fun TasksScreen(
    onOpenTask: (Task) -> Unit,
    onOpenAdventure: (Adventure) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TasksViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TasksScreenContent(
        state = state,
        onOpenTask = onOpenTask,
        onOpenAdventure = onOpenAdventure,
        onBack = onBack,
        onSkipTutorialStep = { viewModel.onAction(TasksAction.SkipTutorialStep) },
        modifier = modifier,
    )
}

@Composable
fun TasksScreenContent(
    state: TasksUiState,
    onOpenTask: (Task) -> Unit,
    onOpenAdventure: (Adventure) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSkipTutorialStep: () -> Unit = {},
) {
    // Обучение: нажать можно только доступные задания, а какое решать — выбирает ребёнок
    val open = remember { SpotlightTargets() }
    val list = rememberLazyListState()
    Box(modifier = modifier.fillMaxSize()) {
        RoomBackground()
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
            Spacer(Modifier.height(10.dp))
            ScreenHeader("Задания", onBack) { state.perWeek?.let { WeekCounter(state.doneThisWeek, it) } }
            Spacer(Modifier.height(14.dp))
            LazyColumn(
                state = list,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
                modifier = Modifier.weight(1f),
            ) {
                val adventure = state.adventure
                when {
                    adventure != null -> item(key = "adventure") { AdventureHero(adventure) { onOpenAdventure(adventure) } }
                    state.adventureDone -> item(key = "adventure-done") { AdventureDone() }
                }
                TaskTopic.entries.forEach { topic ->
                    val tasks = state.items.filter { it.task.topic == topic }
                    if (tasks.isEmpty()) return@forEach
                    item(key = "topic-$topic") {
                        Text(
                            topic.title(),
                            style = OnRoomLabel.copy(fontSize = 20.sp),
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp).semantics { heading() },
                        )
                    }
                    items(tasks, key = { it.task.id }) { item ->
                        TaskRow(
                            item.task, item.status,
                            modifier = if (state.coach && item.status == TaskStatus.AVAILABLE) Modifier.spotlightTarget(open, item.task.id) else Modifier,
                        ) { onOpenTask(item.task) }
                    }
                }
            }
        }
        if (state.coach) {
            TutorialSpotlight(
                "Задания — это задачки про деньги. Решишь — получишь монеты! Выбери любое. А раз в неделю тут ждёт приключение.",
                open.all,
                scroll = list,
                onSkip = onSkipTutorialStep,
            )
        }
    }
}

/** Сколько заданий сделано за неделю: звёздочки-наклейки в плашке, как монеты на главном экране. */
@Composable
private fun WeekCounter(done: Int, perWeek: Int) {
    Row(
        Modifier
            .height(52.dp)
            .creamCard(CircleShape, elevation = 8.dp, border = 3.dp)
            .padding(horizontal = 14.dp)
            .clearAndSetSemantics { contentDescription = "Заданий на этой неделе: $done из $perWeek" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(perWeek) { i ->
            Image(
                painterResource(R.drawable.ic_star_small), null, Modifier.size(22.dp),
                colorFilter = ColorFilter.tint(if (i < done) Color(0xFFFFB020) else FinniColors.Dashed),
            )
        }
        Text("$done/$perWeek", fontSize = 17.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink, modifier = Modifier.padding(start = 4.dp))
    }
}

// ---------- Приключение недели ----------

/**
 * Приключение недели — главное на экране: своё небо с лучами, крупная эмблема, награда и число шагов,
 * короткое вступление и большая кнопка. Нажать можно на всю карточку.
 */
@Composable
private fun AdventureHero(adventure: Adventure, onOpen: () -> Unit) {
    val look = adventure.look
    val shape = RoundedCornerShape(34.dp)
    Surface(
        onClick = onOpen,
        shape = shape,
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .creamCard(shape, elevation = 12.dp)
            .semantics(mergeDescendants = true) { role = Role.Button },
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
                AdventureSky(look, Modifier.matchParentSize(), sunY = 0.6f)
                // Эмблема ниже середины: над ней остаётся место для наклеек «Приключение недели» и награды
                CardSticker(look.emblem, look.tint, size = 112.dp, iconScale = 0.66f, modifier = Modifier.padding(top = 40.dp))
                SkyChip("Приключение недели", Modifier.align(Alignment.TopStart).padding(14.dp))
                RewardChip(adventure.reward, Modifier.align(Alignment.TopEnd).padding(14.dp))
            }
            Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(adventure.title, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, color = FinniColors.Ink)
                Text(adventure.intro, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Тропинка из шагов: сколько будет сцен
                    repeat(adventure.scenes.size) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(look.skyBottom))
                    }
                    Text(stepsText(adventure.scenes.size), fontSize = 13.sp, fontWeight = FontWeight.Black, color = FinniColors.InkMuted, modifier = Modifier.padding(start = 4.dp))
                }
                TealButton("Вперёд!", R.drawable.ic_adventure, onOpen)
            }
        }
    }
}

/** Наклейка на небе: белая пилюля с тёмным текстом — читается на любом цвете неба. */
@Composable
private fun SkyChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text, fontSize = 13.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink,
        modifier = modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.9f)).padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun RewardChip(reward: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(CircleShape).background(FinniColors.CoinPill).border(2.dp, Color.White, CircleShape).padding(start = 4.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(painterResource(R.drawable.ic_coin), null, Modifier.size(22.dp))
        Text("+$reward", fontSize = 15.sp, fontWeight = FontWeight.Black, color = FinniColors.CoinInk)
    }
}

/** Приключение этой недели пройдено: спокойная карточка с галочкой, следующее — на новой неделе. */
@Composable
private fun AdventureDone() {
    Row(
        Modifier
            .fillMaxWidth()
            .creamCard(RoundedCornerShape(26.dp), elevation = 8.dp)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box {
            CardSticker(R.drawable.ic_adventure, FinniColors.CardMint, size = 56.dp, iconScale = 0.6f)
            Box(
                Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 3.dp).size(22.dp).clip(CircleShape)
                    .background(FinniColors.Teal).border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Image(painterResource(R.drawable.ic_check), null, Modifier.size(11.dp)) }
        }
        Column(Modifier.weight(1f)) {
            Text("Приключение пройдено!", fontSize = 16.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink)
            Text("Следующее — на новой неделе", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
        }
    }
}

// ---------- Задания ----------

internal val TaskTopic.sticker: Pair<Int, Color>
    get() = when (this) {
        TaskTopic.BUDGET -> R.drawable.ic_deed_plan to Color(0xFFE6EEFF)
        TaskTopic.SAVINGS -> R.drawable.ic_deed_pig to FinniColors.DreamTint
        TaskTopic.PAYMENTS -> R.drawable.ic_coin to FinniColors.CoinPill
    }

/** Задание: наклейка темы, название, справа — награда (или «сделано»). Недоступное — спокойнее и со словами почему. */
@Composable
private fun TaskRow(task: Task, status: TaskStatus, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    val available = status == TaskStatus.AVAILABLE
    val (icon, tint) = task.topic.sticker
    val note = when (status) {
        TaskStatus.AVAILABLE -> null
        TaskStatus.DONE -> "Сделано"
        TaskStatus.RETRY_NEXT_WEEK -> "Попробуй на следующей неделе"
        TaskStatus.LIMIT_REACHED -> "На этой неделе хватит"
    }
    val shape = RoundedCornerShape(24.dp)
    Surface(
        onClick = onOpen,
        enabled = available,
        shape = shape,
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .creamCard(shape, elevation = if (available) 8.dp else 3.dp)
            .clearAndSetSemantics {
                role = Role.Button
                contentDescription = "${task.title}. Награда ${task.reward}" + (note?.let { ". $it" } ?: "")
            },
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box {
                CardSticker(icon, tint, size = 52.dp, iconScale = 0.58f)
                if (status == TaskStatus.DONE) {
                    Box(
                        Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 3.dp).size(22.dp).clip(CircleShape)
                            .background(FinniColors.Teal).border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Image(painterResource(R.drawable.ic_check), null, Modifier.size(11.dp)) }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    task.title, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Black,
                    color = if (available) FinniColors.Ink else FinniColors.InkMuted,
                )
                note?.let {
                    Text(it, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (status == TaskStatus.DONE) FinniColors.Teal else FinniColors.InkMuted)
                }
            }
            Row(
                Modifier
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(if (available) FinniColors.ActionPeach else FinniColors.Pebble)
                    .padding(start = 6.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Image(painterResource(R.drawable.ic_coin), null, Modifier.size(24.dp))
                Text(
                    "+${task.reward}", fontSize = 16.sp, fontWeight = FontWeight.Black,
                    color = if (available) FinniColors.ActionPeachInk else FinniColors.InkMuted,
                )
            }
        }
    }
}

/** «4 шага», «5 шагов», «1 шаг». */
private fun stepsText(n: Int): String = "$n " + when {
    n % 10 == 1 && n % 100 != 11 -> "шаг"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "шага"
    else -> "шагов"
}
