package ru.larpinovplay.finniapp.presentation.screens.tasks

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.data.content.defaultContent
import ru.larpinovplay.finniapp.domain.content.FeedbackKey
import ru.larpinovplay.finniapp.domain.game.engine.GameRules
import ru.larpinovplay.finniapp.domain.task.model.TaskOutcome
import ru.larpinovplay.finniapp.presentation.components.CardDialog
import ru.larpinovplay.finniapp.presentation.components.CardTitle
import ru.larpinovplay.finniapp.presentation.components.MeterBar
import ru.larpinovplay.finniapp.presentation.components.SoftButton
import ru.larpinovplay.finniapp.presentation.components.RoomBackground
import ru.larpinovplay.finniapp.presentation.components.ScreenHeader
import ru.larpinovplay.finniapp.presentation.components.StatRow
import ru.larpinovplay.finniapp.presentation.components.TealButton
import ru.larpinovplay.finniapp.presentation.components.creamCard
import ru.larpinovplay.finniapp.presentation.feedback.LocalFeedback
import ru.larpinovplay.finniapp.presentation.task.title
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * Прохождение уровня: полоска упражнений сверху, упражнение, после ответа — разбор и «Дальше»,
 * в конце — итог со звёздами. Куда идти потом, решает навигация ([onBack]).
 */
@Composable
fun LevelPlayScreen(
    viewModel: LevelPlayViewModel,   // без значения по умолчанию: ему нужны id уровня и режим из ключа маршрута
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state
    if (current == null) {
        LaunchedEffect(Unit) { onBack() }
    } else {
        LevelPlayScreenContent(state = current, onAction = viewModel::onAction, onBack = onBack, modifier = modifier)
    }
}

@Composable
fun LevelPlayScreenContent(
    state: LevelPlayUiState,
    onAction: (LevelPlayAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Выход из начатого уровня сбрасывает ответы: сначала переспрашиваем (и кнопкой, и системным «назад»)
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    val exit = { if (state.inProgress) confirmExit = true else onBack() }
    BackHandler(enabled = state.inProgress) { confirmExit = true }
    // Секунды испытания идут, только пока экран виден и не открыт вопрос о выходе: свернул приложение — таймер ждёт
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    if (state.timerRunning && !confirmExit) {
        LaunchedEffect(lifecycle) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(1_000)
                    onAction(LevelPlayAction.Tick)
                }
            }
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        RoomBackground()
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            ScreenHeader(state.level.title, exit, Modifier.padding(top = 8.dp)) {
                state.secondsLeft?.let { TimerChip(it) }
            }
            Spacer(Modifier.height(10.dp))
            Progress(state)
            Spacer(Modifier.height(10.dp))
            val scroll = rememberScrollState()
            // Новое упражнение открывается сверху; после ответа экран доезжает до разбора и «Дальше»
            LaunchedEffect(state.index, state.finish != null) { scroll.scrollTo(0) }
            LaunchedEffect(state.feedback) { if (state.feedback != null) scroll.animateScrollTo(scroll.maxValue) }
            Column(
                Modifier.weight(1f).verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val finish = state.finish
                if (finish != null) {
                    FinishCard(finish, onDone = onBack)
                } else {
                    TaskCard {
                        Column(Modifier.padding(16.dp)) {
                            Text(state.level.topic.title(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = FinniColors.Teal)
                            Text(state.task.intro, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.Ink)
                            // В испытании подсказок нет: в этом и испытание
                            if (!state.challenge) key(state.task.id) { HintButton(state.task.hint) }
                        }
                    }
                    key(state.task.id) {
                        TaskWidget(state.task.payload, enabled = state.feedback == null) { onAction(LevelPlayAction.Submit(it)) }
                    }
                    state.feedback?.let { FeedbackCard(it) }
                    if (state.feedback != null) {
                        TealButton(
                            if (state.last) "Завершить уровень" else "Дальше",
                            if (state.last) R.drawable.ic_sun_small else null,
                            { onAction(LevelPlayAction.Next) },
                            enabled = !state.saving,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    if (confirmExit) {
        ExitDialog(
            onStay = { confirmExit = false },
            onLeave = {
                confirmExit = false
                onBack()
            },
        )
    }
}

/** «Выйти из уровня?»: ответы этого прохождения не сохранятся. Остаться — главная кнопка. */
@Composable
private fun ExitDialog(onStay: () -> Unit, onLeave: () -> Unit) {
    CardDialog(onDismiss = onStay) {
        CardTitle("Выйти из уровня?", "Прогресс уровня сбросится")
        Text(
            "Ответы этого прохождения не сохранятся — уровень придётся начать сначала.",
            style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted,
        )
        TealButton("Остаться", null, onStay)
        SoftButton("Выйти", onLeave)
    }
}

/** Полоска упражнений: сколько уже отвечено из скольких, числом и цветом. */
@Composable
private fun Progress(state: LevelPlayUiState) {
    Row(
        Modifier
            .fillMaxWidth()
            .creamCard(CircleShape, elevation = 6.dp, border = 3.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clearAndSetSemantics { contentDescription = "Задание ${minOf(state.index + 1, state.total)} из ${state.total}" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MeterBar(state.answered.toFloat() / state.total, FinniColors.TealBright, Modifier.weight(1f), height = 12.dp)
        Text("${state.answered}/${state.total}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = FinniColors.Ink)
    }
}

/** Секунды испытания; последние десять — на тёплой плашке. */
@Composable
private fun TimerChip(seconds: Int) {
    val low = seconds <= 10
    Text(
        "%d:%02d".format(seconds / 60, seconds % 60),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Black,
        color = if (low) FinniColors.WarnInk else FinniColors.Ink,
        modifier = Modifier
            .creamCard(CircleShape, elevation = 8.dp, border = 3.dp)
            .background(if (low) FinniColors.WarnTint else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clearAndSetSemantics { contentDescription = "Осталось секунд: $seconds" },
    )
}

/** Разбор ответа: верно или «почти», что из-за выбора случилось и почему. Объявляется TalkBack сразу. */
@Composable
private fun FeedbackCard(outcome: TaskOutcome) {
    Column(
        Modifier
            .fillMaxWidth()
            .creamCard(RoundedCornerShape(26.dp), elevation = 10.dp)
            .background(if (outcome.success) FinniColors.CardMint else FinniColors.WarnTint)
            .padding(16.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(if (outcome.success) FinniColors.Teal else FinniColors.WarnInk)
                    .border(3.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (outcome.success) Image(painterResource(R.drawable.ic_check), null, Modifier.size(16.dp))
                else Text("!", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
            Text(
                if (outcome.success) "Верно!" else "Почти получилось",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black,
                color = if (outcome.success) FinniColors.Teal else FinniColors.WarnInk,
            )
        }
        outcome.consequence?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.ExtraBold, color = FinniColors.Ink)
        }
        Text(outcome.explanation, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
    }
}

/** Итог уровня: звёзды или золото, сколько верно, монеты — и «Готово» обратно на карту. */
@Composable
private fun FinishCard(finish: LevelFinish, onDone: () -> Unit) {
    val feedback = LocalFeedback.current
    Column(
        Modifier
            .fillMaxWidth()
            .creamCard(RoundedCornerShape(34.dp), elevation = 12.dp)
            .padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (finish) {
            is LevelFinish.Completed -> {
                BigStars(finish.stars)
                Title("Уровень пройден!")
                Body(feedback.text(if (finish.reward == 0) FeedbackKey.LEVEL_REPLAY else if (finish.correct == finish.total) FeedbackKey.LEVEL_DONE_PERFECT else FeedbackKey.LEVEL_DONE_MISTAKES))
                StatRow("Верных ответов", "${finish.correct} из ${finish.total}")
                if (finish.reward > 0) StatRow("Монеты", "+${finish.reward}", coin = true)
            }
            is LevelFinish.Challenge -> {
                Box(
                    Modifier.size(96.dp).creamCard(CircleShape, elevation = 8.dp).background(if (finish.gold) FinniColors.Gold else FinniColors.Pebble),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painterResource(R.drawable.ic_star_small), null, Modifier.size(48.dp),
                        colorFilter = ColorFilter.tint(if (finish.gold) Color.White else FinniColors.Dashed),
                    )
                }
                Title(if (finish.gold) "Золотой уровень!" else "Золото пока не получилось")
                Body(
                    feedback.text(
                        when {
                            finish.gold -> FeedbackKey.CHALLENGE_GOLD
                            finish.timeUp -> FeedbackKey.CHALLENGE_TIME_UP
                            else -> FeedbackKey.CHALLENGE_MISTAKES
                        }
                    )
                )
                StatRow("Верных ответов", "${finish.correct} из ${finish.total}")
            }
        }
        TealButton("Готово", R.drawable.ic_check, onDone)
    }
}

/** Три крупные звезды: заработанные — жёлтые. Число — в описании для TalkBack. */
@Composable
private fun BigStars(stars: Int) {
    Row(
        Modifier.clearAndSetSemantics { contentDescription = "Звёзд: $stars из ${GameRules.MAX_STARS}" },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(GameRules.MAX_STARS) { i ->
            Image(
                painterResource(R.drawable.ic_star_small), null,
                // Средняя звезда крупнее — как пьедестал
                Modifier.size(if (i == 1) 64.dp else 50.dp),
                colorFilter = ColorFilter.tint(if (i < stars) FinniColors.PawNew else FinniColors.Dashed),
            )
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(
        text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = FinniColors.Ink,
        textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted, textAlign = TextAlign.Center)
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun LevelPlayFeedbackPreview() {
    val level = defaultContent().levels.first()
    LevelPlayScreenContent(
        state = LevelPlayUiState(
            level = level,
            tasks = level.tasks.take(4),
            challenge = false,
            feedback = TaskOutcome(success = false, consequence = "Шляпа красивая, но без неё Финни хорошо.", explanation = level.tasks.first().explanationMistake),
            mistakes = 1,
        ),
        onAction = {},
        onBack = {},
    )
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun LevelPlayFinishPreview() {
    LevelPlayScreenContent(
        state = LevelPlayUiState(
            level = defaultContent().levels.first(),
            tasks = defaultContent().levels.first().tasks.take(4),
            challenge = false,
            index = 3,
            finish = LevelFinish.Completed(stars = 2, reward = 2, correct = 3, total = 4),
        ),
        onAction = {},
        onBack = {},
    )
}
