package ru.larpinovplay.finniapp.presentation.screens.tasks

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.game.model.TaskStatus
import ru.larpinovplay.finniapp.domain.task.model.Task
import ru.larpinovplay.finniapp.domain.task.model.TaskOutcome
import ru.larpinovplay.finniapp.domain.task.model.TaskPayload
import ru.larpinovplay.finniapp.domain.task.model.TaskTopic
import ru.larpinovplay.finniapp.presentation.components.RoomBackground
import ru.larpinovplay.finniapp.presentation.task.title
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * Список заданий по темам (ТЗ 2.5.8). Статус задания — словом, не только цветом.
 * Само прохождение и итог — отдельные маршруты TaskPlay и TaskResult.
 */
@Composable
fun TasksScreen(
    onOpenTask: (Task) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TasksViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TasksScreenContent(state = state, onOpenTask = onOpenTask, onBack = onBack, modifier = modifier)
}

@Composable
fun TasksScreenContent(
    state: TasksUiState,
    onOpenTask: (Task) -> Unit,
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
            item { Header(onBack) }
            item {
                Text(
                    "На этой неделе: ${state.doneThisWeek} из ${state.perWeek} заданий",
                    style = MaterialTheme.typography.bodyLarge,
                    color = FinniColors.NavyMuted,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            TaskTopic.entries.forEach { topic ->
                item {
                    Text(topic.title(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 6.dp, top = 6.dp))
                }
                items(state.items.filter { it.task.topic == topic }, key = { it.task.id }) { item ->
                    TaskCard(item.task, item.status) { onOpenTask(item.task) }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

// ---------- Список ----------

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
        Text("Задания", style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun TaskCard(task: Task, status: TaskStatus, onOpen: () -> Unit) {
    val enabled = status == TaskStatus.AVAILABLE
    val (statusText, statusColor) = when (status) {
        TaskStatus.AVAILABLE -> "Доступно" to FinniColors.Blue
        TaskStatus.DONE -> "Выполнено ✓" to FinniColors.Mood
        TaskStatus.RETRY_NEXT_WEEK -> "Попробуй на следующей неделе" to FinniColors.NavyMuted
        TaskStatus.LIMIT_REACHED -> "На этой неделе хватит" to FinniColors.NavyMuted
    }
    val shape = RoundedCornerShape(24.dp)
    Surface(
        onClick = onOpen,
        enabled = enabled,
        shape = shape,
        color = if (enabled) FinniColors.Card else FinniColors.Card.copy(alpha = 0.7f),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, shape, ambientColor = FinniColors.Navy.copy(alpha = 0.15f), spotColor = FinniColors.Navy.copy(alpha = 0.15f))
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(56.dp)
                    .background(FinniColors.BlueLight, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) { Image(painterResource(R.drawable.ic_target), null, Modifier.size(34.dp)) }
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(task.title, style = MaterialTheme.typography.titleMedium)
                Text(statusText, style = MaterialTheme.typography.labelMedium, color = statusColor)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("+${task.reward}", style = MaterialTheme.typography.titleLarge)
                Image(painterResource(R.drawable.ic_coin), null, Modifier.padding(start = 4.dp).size(20.dp))
            }
        }
    }
}

// ---------- Результат ----------

/**
 * Содержимое диалога итога. Само окно (Dialog) создаёт навигация: маршрут TaskResult
 * помечен как диалог, поэтому здесь только карточка, без AlertDialog.
 */
@Composable
fun TaskResultCard(result: TaskOutcome, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(28.dp), color = Color.White, modifier = modifier) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (result.success) "Верно!" else "Почти получилось",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            result.consequence?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            Text(result.explanation, style = MaterialTheme.typography.bodyLarge, color = FinniColors.Navy)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Монеты", style = MaterialTheme.typography.bodyMedium, color = FinniColors.NavyMuted, modifier = Modifier.weight(1f))
                Text("+${result.reward}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Image(painterResource(R.drawable.ic_coin), null, Modifier.padding(start = 4.dp).size(20.dp))
            }
            if (!result.success) {
                Text("Это задание можно попробовать снова на следующей неделе", style = MaterialTheme.typography.labelMedium, color = FinniColors.NavyMuted)
            }
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.End)
                    .height(48.dp)
            ) {
                Text("Понятно", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Нужен для превью и тестов виджета списка покупок. */
@Composable
internal fun ShopListRow(item: TaskPayload.ShopList.Item, checked: Boolean, onToggle: () -> Unit) {
    Surface(onClick = onToggle, shape = RoundedCornerShape(16.dp), color = if (checked) FinniColors.BlueLight else Color.White) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.bodyLarge)
                Text(if (item.mandatory) "нужное" else "желаемое", style = MaterialTheme.typography.labelSmall, color = FinniColors.NavyMuted)
            }
            Text("${item.price}", style = MaterialTheme.typography.titleMedium)
            Image(painterResource(R.drawable.ic_coin), null, Modifier.padding(start = 4.dp).size(18.dp))
        }
    }
}

@Composable
internal fun HintButton(hint: String) {
    var shown by remember { mutableStateOf(false) }
    TextButton(onClick = { shown = !shown }) { Text(if (shown) "Скрыть подсказку" else "Подсказка", style = MaterialTheme.typography.labelLarge) }
    if (shown) {
        Text(hint, style = MaterialTheme.typography.bodyMedium, color = FinniColors.NavyMuted, modifier = Modifier.padding(horizontal = 8.dp))
    }
}
