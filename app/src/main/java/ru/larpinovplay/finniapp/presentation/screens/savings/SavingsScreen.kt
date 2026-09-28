package ru.larpinovplay.finniapp.presentation.screens.savings

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import ru.larpinovplay.finniapp.presentation.components.spotlightTarget
import ru.larpinovplay.finniapp.presentation.components.TutorialSpotlight
import ru.larpinovplay.finniapp.presentation.components.SpotlightTargets
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.game.engine.GameRules
import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal
import ru.larpinovplay.finniapp.presentation.components.ScreenHeader
import ru.larpinovplay.finniapp.presentation.components.CardDialog
import ru.larpinovplay.finniapp.presentation.components.CardSticker
import ru.larpinovplay.finniapp.presentation.components.CoinPill
import ru.larpinovplay.finniapp.presentation.components.DashedDivider
import ru.larpinovplay.finniapp.presentation.components.EffectChip
import ru.larpinovplay.finniapp.presentation.components.OnRoomLabel
import ru.larpinovplay.finniapp.presentation.components.PillButton
import ru.larpinovplay.finniapp.presentation.components.RoomBackground
import ru.larpinovplay.finniapp.presentation.components.SoftButton
import ru.larpinovplay.finniapp.presentation.components.StatRow
import ru.larpinovplay.finniapp.presentation.components.StepButton
import ru.larpinovplay.finniapp.presentation.components.TealButton
import ru.larpinovplay.finniapp.presentation.components.changesRoom
import ru.larpinovplay.finniapp.presentation.components.creamCard
import ru.larpinovplay.finniapp.presentation.theme.FinniColors
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/**
 * Копилка, ТЗ 2.5.7: цели с понятной стоимостью, выбранная цель выделена; видны накоплено,
 * осталось и срок по среднему пополнению; можно регулярно переводить монеты в копилку и забирать их.
 *
 * Сверху — мечта в большом розовом кольце: то же кольцо, что вокруг «Копилки» в меню главного экрана,
 * только крупно. Под ним — все мечты плитками, как товары в магазине.
 */
@Composable
fun SavingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SavingsScreenContent(state = state, onAction = viewModel::onAction, onBack = onBack, modifier = modifier)
}

@Composable
fun SavingsScreenContent(
    state: SavingsUiState,
    onAction: (SavingsAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Обучение: мечты — единственное, что можно нажать; какую выбрать — решает ребёнок
    val dreams = remember { SpotlightTargets() }
    val list = rememberLazyGridState()
    Box(modifier = modifier.fillMaxSize()) {
        RoomBackground()
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
            Spacer(Modifier.height(10.dp))
            ScreenHeader("Копилка", onBack) { CoinPill(state.balance) }
            Spacer(Modifier.height(14.dp))
            LazyVerticalGrid(
                state = list,
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
                modifier = Modifier.weight(1f),
            ) {
                item(key = "dream", span = { GridItemSpan(maxLineSpan) }) {
                    val goal = state.goal
                    if (goal == null) NoDreamCard(state.savings, onWithdraw = { onAction(SavingsAction.WithdrawClicked) })
                    else DreamCard(
                        goal = goal,
                        state = state,
                        onChangeDeposit = { onAction(SavingsAction.ChangeDeposit(it)) },
                        onDeposit = { onAction(SavingsAction.Deposit) },
                        onReach = { onAction(SavingsAction.ReachGoalClicked) },
                        onWithdraw = { onAction(SavingsAction.WithdrawClicked) },
                    )
                }
                item(key = "title", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        if (state.goal == null) "Выбери цель" else "Все цели",
                        style = OnRoomLabel.copy(fontSize = 20.sp),
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp).semantics { heading() },
                    )
                }
                items(state.goals, key = { it.id }) { goal ->
                    GoalTile(
                        goal = goal,
                        selected = goal.id == state.goal?.id,
                        completed = goal.id in state.completedGoalIds,
                        onClick = { onAction(SavingsAction.GoalClicked(goal)) },
                        modifier = if (state.coach) Modifier.spotlightTarget(dreams, goal.id) else Modifier,
                    )
                }
            }
        }
        if (state.coach) {
            TutorialSpotlight(
                "Выбери цель — любую, какую хочешь! Монеты из копилки будут копиться на неё.",
                dreams.all,
                scroll = list,
                onSkip = { onAction(SavingsAction.SkipTutorialStep) },
            )
        }
    }

    state.switchTo?.let { goal ->
        SwitchGoalDialog(
            goal = goal,
            savings = state.savings,
            onConfirm = { onAction(SavingsAction.ConfirmSwitch) },
            onDismiss = { onAction(SavingsAction.DismissSwitch) },
        )
    }
    state.withdraw?.let { draft ->
        WithdrawDialog(
            draft = draft,
            goal = state.goal,
            onChange = { onAction(SavingsAction.ChangeWithdraw(it)) },
            onConfirm = { onAction(SavingsAction.ConfirmWithdraw) },
            onDismiss = { onAction(SavingsAction.DismissWithdraw) },
        )
    }
    state.reached?.let { goal -> DreamCameTrueDialog(goal, onDismiss = { onAction(SavingsAction.DismissReached) }) }
}

// ---------- Мечта ----------

/**
 * Большое кольцо мечты: розовая дуга растёт по мере накопления, в середине — сама мечта.
 * Заполнение плавно догоняет новое значение после «Отложить».
 */
@Composable
private fun DreamRing(progress: Float, icon: Int, modifier: Modifier = Modifier, size: Dp = 176.dp) {
    val shown by animateFloatAsState(
        progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "dream",
    )
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = 16.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(FinniColors.DreamTrack, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
            if (shown > 0f) {
                drawArc(
                    FinniColors.DreamRing, -90f, 360f * shown, useCenter = false, topLeft = topLeft, size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        CardSticker(icon, FinniColors.DreamTint, size = size - 44.dp, iconScale = 0.62f)
    }
}

@Composable
private fun DreamCard(
    goal: SavingsGoal,
    state: SavingsUiState,
    onChangeDeposit: (increase: Boolean) -> Unit,
    onDeposit: () -> Unit,
    onReach: () -> Unit,
    onWithdraw: () -> Unit,
) {
    val remaining = state.goalRemaining ?: 0
    val reached = remaining == 0
    val amount = state.depositAmount

    Column(
        Modifier
            .fillMaxWidth()
            .creamCard(RoundedCornerShape(34.dp), elevation = 10.dp)
            .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = "Цель: ${goal.name}. Накоплено ${state.savings} из ${goal.cost}"
            },
        ) {
            DreamRing(state.savings.toFloat() / goal.cost, goal.icon, Modifier.padding(bottom = 18.dp))
            // Накоплено — наклейка на нижнем краю кольца
            Row(
                Modifier
                    .creamCard(CircleShape, elevation = 6.dp, border = 3.dp)
                    .background(FinniColors.CoinPill)
                    .padding(start = 6.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Image(painterResource(R.drawable.ic_coin), null, Modifier.size(26.dp))
                Text("${state.savings}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = FinniColors.CoinInk)
                Text("из ${goal.cost}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(goal.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = FinniColors.Ink, textAlign = TextAlign.Center)
            Text(
                when {
                    reached -> "Хватает на цель!"
                    state.weeksToGoal != null -> "Осталось $remaining · примерно ${state.weeksToGoal} нед."
                    else -> "Осталось $remaining. Отложи — и я посчитаю срок"
                },
                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.ExtraBold, color = FinniColors.Teal, textAlign = TextAlign.Center,
            )
        }

        DashedDivider()
        if (reached) {
            TealButton("Достичь цели!", R.drawable.ic_sun_small, onReach)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepButton("−", "Отложить меньше", enabled = state.canDepositLess) { onChangeDeposit(false) }
                Text(
                    "$amount",
                    fontSize = 22.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink, textAlign = TextAlign.Center,
                    modifier = Modifier.width(52.dp).semantics { contentDescription = "Отложить: $amount" },
                )
                StepButton("+", "Отложить больше", enabled = state.canDepositMore) { onChangeDeposit(true) }
                Spacer(Modifier.width(10.dp))
                TealButton("Отложить", R.drawable.ic_coin, onDeposit, modifier = Modifier.weight(1f))
            }
            state.depositError?.let {
                Text(it.text(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.WarnInk, textAlign = TextAlign.Center)
            }
        }
        if (state.savings > 0) {
            PillButton("Забрать из копилки", onWithdraw, color = FinniColors.Pebble, ink = FinniColors.InkMuted)
        }
    }
}

/** Мечта ещё не выбрана, но монеты в копилке уже могут быть: например, отложенные по плану недели. */
@Composable
private fun NoDreamCard(savings: Int, onWithdraw: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .creamCard(RoundedCornerShape(34.dp), elevation = 10.dp)
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CardSticker(R.drawable.ic_deed_pig, FinniColors.DreamTint, size = 112.dp, iconScale = 0.6f)
        Text("На что будем копить?", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = FinniColors.Ink, textAlign = TextAlign.Center)
        Text(
            if (savings > 0) "В копилке уже $savings. Выбери цель ниже — и они пойдут на неё"
            else "Выбери цель ниже. Её кольцо появится и в меню",
            style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted, textAlign = TextAlign.Center,
        )
        if (savings > 0) PillButton("Забрать из копилки", onWithdraw, color = FinniColors.Pebble, ink = FinniColors.InkMuted)
    }
}

// ---------- Все мечты ----------

/** Плитка мечты: картинка, название и цена. Выбранная — в бирюзовой рамке и со словом «копим», не только цветом. */
@Composable
private fun GoalTile(goal: SavingsGoal, selected: Boolean, completed: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(26.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .creamCard(shape, elevation = 8.dp)
            // Белая обводка наклейки рисуется поверх, поэтому бирюзовая рамка шире: видны её внутренние 3 dp
            .then(if (selected) Modifier.border(7.dp, FinniColors.Teal, shape) else Modifier)
            .clearAndSetSemantics {
                role = Role.Button
                contentDescription = "${goal.name}, ${goal.cost} монет" + when {
                    selected -> ". Копим на неё"
                    completed -> ". Уже получено. Выбрать снова"
                    else -> ". Выбрать"
                }
            },
    ) {
        Column(
            Modifier.padding(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box {
                CardSticker(goal.icon, FinniColors.DreamTint, size = 88.dp, iconScale = 0.64f)
                if (completed) {
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 2.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(FinniColors.Teal)
                            .border(3.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Image(painterResource(R.drawable.ic_check), null, Modifier.size(13.dp)) }
                }
            }
            Text(
                goal.name, fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink,
                textAlign = TextAlign.Center, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(if (selected) FinniColors.Teal else FinniColors.DreamTint),
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selected) {
                    Text("копим", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text("· ${goal.cost}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.85f))
                } else {
                    Image(painterResource(R.drawable.ic_coin), null, Modifier.size(22.dp))
                    Text("${goal.cost}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = FinniColors.CoinInk)
                }
            }
        }
    }
}

// ---------- Окна ----------

@Composable
private fun GoalHeader(goal: SavingsGoal, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        CardSticker(goal.icon, FinniColors.DreamTint, size = 72.dp, iconScale = 0.62f)
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, color = FinniColors.Ink, modifier = Modifier.semantics { heading() })
            Text(subtitle, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = FinniColors.Teal)
        }
    }
}

/** Смена мечты: монеты в копилке никуда не деваются, меняется только то, на что они копятся. */
@Composable
private fun SwitchGoalDialog(goal: SavingsGoal, savings: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    CardDialog(onDismiss = onDismiss) {
        // Название мечты не склоняется в «Копим на …», поэтому оно в подстроке, а не в вопросе
        GoalHeader(goal, "Новая цель?", "${goal.name} · ${goal.cost} монет")
        Text(goal.hint, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
        DashedDivider()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatRow("В копилке останется", "$savings", coin = true)
            StatRow("Осталось накопить", "${(goal.cost - savings).coerceAtLeast(0)}", coin = true)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TealButton("Да, копим", R.drawable.ic_check, onConfirm)
            SoftButton("Оставить прежнюю", onDismiss)
        }
    }
}

/** Мечта сбылась: праздник вместе с Финни и что это ему дало. */
@Composable
private fun DreamCameTrueDialog(goal: SavingsGoal, onDismiss: () -> Unit) {
    CardDialog(onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box {
                DreamRing(1f, goal.icon, size = 148.dp)
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-6).dp, y = (-6).dp)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(FinniColors.Teal)
                        .border(3.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Image(painterResource(R.drawable.ic_check), null, Modifier.size(18.dp)) }
            }
            Text(
                "Цель достигнута!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = FinniColors.Ink,
                textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() },
            )
            val done = when {
                goal.id == "room" -> "Ремонт готов: загляни на главный экран, какая теперь комната у Финни!"
                goal.trip -> "Финни уехал на целую неделю — загляни на главный экран! Когда неделя закончится, он вернётся с сувенирами."
                changesRoom(goal.id) -> "«${goal.name}» теперь в комнате Финни — загляни на главный экран."
                else -> "«${goal.name}» теперь у Финни."
            }
            Text(
                "$done Ты откладывал каждую неделю — и получилось!",
                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted, textAlign = TextAlign.Center,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Финни получил", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
                EffectChip(R.drawable.ic_meter_smile, GameRules.GOAL_MOOD_BONUS, Color(0xFFFFF5C9))
            }
        }
        TealButton("Выбрать новую цель", R.drawable.ic_sun_small, onDismiss)
    }
}
