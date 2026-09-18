package ru.larpinovplay.finniapp.presentation.screens.savings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal
import ru.larpinovplay.finniapp.presentation.components.RoomBackground
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * Копилка, ТЗ 2.5.7: цели с понятной стоимостью, выбранная цель выделена; видны накоплено,
 * осталось и срок по среднему пополнению; можно регулярно переводить монеты в копилку.
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
    Box(modifier = modifier.fillMaxSize()) {
        RoomBackground()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(Modifier.height(0.dp)); Header(state.balance, onBack) }
            item {
                val goal = state.goal
                if (goal == null) NoGoalCard() else CurrentGoalCard(
                    goal = goal,
                    state = state,
                    onDeposit = { onAction(SavingsAction.Deposit(it)) },
                    onReach = { onAction(SavingsAction.ReachGoalClicked) }
                )
            }
            item {
                Text(
                    "На что копим",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 6.dp, top = 6.dp)
                )
            }
            items(state.goals, key = { it.id }) { goal ->
                GoalCard(
                    goal = goal,
                    selected = goal.id == state.goal?.id,
                    completed = goal.id in state.completedGoalIds,
                    onClick = { onAction(SavingsAction.GoalClicked(goal)) }
                )
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }

    state.switchTo?.let { goal ->
        AlertDialog(
            onDismissRequest = { onAction(SavingsAction.DismissSwitch) },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White,
            title = { Text("Поменять цель?", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Text(
                    "Копилка останется: ${state.savings} монет. Теперь ты будешь копить на «${goal.name}» за ${goal.cost}.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(onClick = { onAction(SavingsAction.ConfirmSwitch) }, shape = RoundedCornerShape(16.dp), modifier = Modifier.height(48.dp)) {
                    Text("Да, поменять", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(SavingsAction.DismissSwitch) }, modifier = Modifier.height(48.dp)) { Text("Оставить", style = MaterialTheme.typography.labelLarge) }
            }
        )
    }
    state.reached?.let { goal ->
        AlertDialog(
            onDismissRequest = { onAction(SavingsAction.DismissReached) },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White,
            icon = { Image(painterResource(goal.icon), null, Modifier.size(64.dp)) },
            title = { Text("Ура! Цель достигнута", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center) },
            text = {
                Text(
                    "«${goal.name}» теперь у Финни. Ты откладывал каждую неделю — и получилось. Настроение +30. Выбери новую цель!",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(onClick = { onAction(SavingsAction.DismissReached) }, shape = RoundedCornerShape(16.dp), modifier = Modifier.height(48.dp)) {
                    Text("Здорово", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }
}

// ---------- Шапка ----------

@Composable
private fun Header(balance: Int, onBack: () -> Unit) {
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
        Text("Копилка", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        WhiteCard(shape = RoundedCornerShape(20.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(painterResource(R.drawable.ic_coin), null, Modifier.size(26.dp))
                Text("$balance", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

// ---------- Текущая цель ----------

@Composable
private fun NoGoalCard() {
    WhiteCard(modifier = Modifier.fillMaxWidth(), color = FinniColors.CardPink) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_pig), null, Modifier.size(48.dp))
            Column(Modifier.padding(start = 12.dp)) {
                Text("Цель пока не выбрана", style = MaterialTheme.typography.titleMedium)
                Text("Выбери, на что копить, из списка ниже", style = MaterialTheme.typography.bodyMedium, color = FinniColors.NavyMuted)
            }
        }
    }
}

@Composable
private fun CurrentGoalCard(
    goal: SavingsGoal,
    state: SavingsUiState,
    onDeposit: (Int) -> Unit,
    onReach: () -> Unit,
) {
    val remaining = (goal.cost - state.savings).coerceAtLeast(0)
    val reachedGoal = state.savings >= goal.cost
    var amount by remember { mutableIntStateOf(10) }   // ввод суммы: пока не нажата «Отложить», это не состояние экрана

    WhiteCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(64.dp)
                        .background(FinniColors.BlueLight, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) { Image(painterResource(goal.icon), null, Modifier.size(42.dp)) }
                Column(Modifier.padding(start = 12.dp)) {
                    Text("Моя цель", style = MaterialTheme.typography.labelSmall, color = FinniColors.NavyMuted)
                    Text(goal.name, style = MaterialTheme.typography.titleLarge)
                    Text("Накоплено ${state.savings} из ${goal.cost}", style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.height(12.dp))
            ProgressBar(state.savings, goal.cost)
            Spacer(Modifier.height(8.dp))
            Row {
                Text(
                    if (reachedGoal) "Хватает на цель!" else "Осталось $remaining",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    when {
                        reachedGoal -> ""
                        state.weeksToGoal != null -> "≈ ${state.weeksToGoal} нед."
                        else -> "Начни откладывать — посчитаю срок"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = FinniColors.NavyMuted,
                    textAlign = TextAlign.End
                )
            }
            Spacer(Modifier.height(12.dp))
            if (reachedGoal) {
                Button(
                    onClick = onReach,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FinniColors.Green, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) { Text("Получить!", style = MaterialTheme.typography.titleMedium) }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StepButton("−") { amount = (amount - 5).coerceAtLeast(5) }
                    Text(
                        "$amount",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.size(width = 56.dp, height = 36.dp)
                    )
                    StepButton("+") { amount = (amount + 5).coerceAtMost(state.balance.coerceAtLeast(5)) }
                    Button(
                        onClick = { onDeposit(amount) },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) { Text("Отложить", style = MaterialTheme.typography.titleMedium) }
                }
                state.depositError?.let {
                    Text(it.text(), style = MaterialTheme.typography.labelMedium, color = FinniColors.Warning, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun StepButton(text: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = FinniColors.BlueLight, modifier = Modifier.size(48.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.headlineSmall, color = FinniColors.Blue)
        }
    }
}

@Composable
private fun ProgressBar(value: Int, max: Int) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(FinniColors.Track)
    ) {
        Box(
            Modifier
                .fillMaxWidth((value.toFloat() / max).coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(FinniColors.Care, RoundedCornerShape(6.dp))
        )
    }
}

// ---------- Список целей ----------

/** Карточка цели. Выбранная выделена рамкой и меткой «Цель», не только цветом. */
@Composable
private fun GoalCard(goal: SavingsGoal, selected: Boolean, completed: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (selected) FinniColors.BlueLight else FinniColors.Card,
        border = if (selected) BorderStroke(3.dp, FinniColors.Blue) else null,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, shape, ambientColor = FinniColors.Navy.copy(alpha = 0.15f), spotColor = FinniColors.Navy.copy(alpha = 0.15f))
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(56.dp)
                    .background(Color.White, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) { Image(painterResource(goal.icon), null, Modifier.size(38.dp)) }
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(goal.name, style = MaterialTheme.typography.titleMedium)
                Text(goal.hint, style = MaterialTheme.typography.labelSmall, color = FinniColors.NavyMuted)
                if (completed) Text("Уже получено", style = MaterialTheme.typography.labelSmall, color = FinniColors.Mood)
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${goal.cost}", style = MaterialTheme.typography.titleLarge)
                    Image(painterResource(R.drawable.ic_coin), null, Modifier.padding(start = 4.dp).size(20.dp))
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) FinniColors.Blue else FinniColors.Lavender,
                ) {
                    Text(
                        if (selected) "✓ Цель" else "Выбрать",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) Color.White else FinniColors.Navy,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WhiteCard(
    modifier: Modifier = Modifier,
    color: Color = FinniColors.Card,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.shadow(6.dp, shape, ambientColor = FinniColors.Navy.copy(alpha = 0.15f), spotColor = FinniColors.Navy.copy(alpha = 0.15f)),
        shape = shape,
        color = color,
        content = content
    )
}
