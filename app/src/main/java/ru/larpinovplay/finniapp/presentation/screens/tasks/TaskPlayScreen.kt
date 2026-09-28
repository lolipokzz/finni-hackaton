package ru.larpinovplay.finniapp.presentation.screens.tasks

import ru.larpinovplay.finniapp.presentation.events.ObserveAsEvents
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.task.model.Task
import ru.larpinovplay.finniapp.domain.task.model.TaskAnswer
import ru.larpinovplay.finniapp.domain.task.model.TaskOutcome
import ru.larpinovplay.finniapp.domain.task.model.TaskPayload
import ru.larpinovplay.finniapp.presentation.components.RoomBackground
import ru.larpinovplay.finniapp.presentation.components.ScreenHeader
import ru.larpinovplay.finniapp.presentation.components.StepButton
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

import ru.larpinovplay.finniapp.presentation.task.title
import ru.larpinovplay.finniapp.presentation.theme.FinniColors
import androidx.compose.ui.text.style.TextAlign

/**
 * Прохождение одного задания. [onCompleted] вызывается после ответа с итогом
 * (null — задание уже недоступно); куда идти дальше, решает навигация.
 */
@Composable
fun TaskPlayScreen(
    viewModel: TaskPlayViewModel,   // без значения по умолчанию: ему нужен id задания из ключа маршрута
    onCompleted: (TaskOutcome?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is TaskPlayEffect.Completed -> onCompleted(effect.outcome)
        }
    }
    val task = viewModel.task
    if (task == null) {
        LaunchedEffect(Unit) { onBack() }
    } else {
        TaskPlayScreenContent(
            task = task,
            onSubmit = { answer -> viewModel.onAction(TaskPlayAction.Submit(answer)) },
            onBack = onBack,
            modifier = modifier,
        )
    }
}

/** Интро, виджет по типу, подсказка, кнопка «Готово». */
@Composable
fun TaskPlayScreenContent(task: Task, onSubmit: (TaskAnswer) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        RoomBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ScreenHeader(task.title, onBack, Modifier.padding(top = 8.dp))
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text(task.topic.title(), style = MaterialTheme.typography.labelMedium, color = FinniColors.NavyMuted)
                    Text(task.intro, style = MaterialTheme.typography.bodyLarge)
                    HintButton(task.hint)
                }
            }
            when (val p = task.payload) {
                is TaskPayload.Choice -> ChoiceWidget(p, onSubmit)
                is TaskPayload.Allocate -> AllocateWidget(p, onSubmit)
                is TaskPayload.ShopList -> ShopListWidget(p, onSubmit)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------- Выбор действия ----------

@Composable
private fun ChoiceWidget(p: TaskPayload.Choice, onSubmit: (TaskAnswer) -> Unit) {
    var selected by remember { mutableStateOf<String?>(null) }
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        p.options.forEach { option ->
            val isSelected = option.id == selected
            Surface(
                selected = isSelected,
                onClick = { selected = option.id },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) FinniColors.Blue else FinniColors.Card,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = FinniColors.Navy.copy(alpha = 0.12f), spotColor = FinniColors.Navy.copy(alpha = 0.12f))
                    .semantics { role = Role.RadioButton },
            ) {
                Text(
                    option.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) Color.White else FinniColors.Navy,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }
        }
        DoneButton(enabled = selected != null) { onSubmit(TaskAnswer.Choice(selected!!)) }
    }
}

// ---------- Раскладка суммы ----------

@Composable
private fun AllocateWidget(p: TaskPayload.Allocate, onSubmit: (TaskAnswer) -> Unit) {
    var amounts by remember { mutableStateOf(p.buckets.associate { it.id to 0 }) }
    val total = amounts.values.sum()
    val remaining = p.total - total
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            p.buckets.forEach { bucket ->
                val value = amounts[bucket.id] ?: 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(bucket.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    StepButton("−", "Меньше: ${bucket.label}", enabled = value > 0, color = FinniColors.BlueLight, ink = FinniColors.Blue,
                        disabledColor = FinniColors.Track, disabledInk = FinniColors.NavyMuted) { amounts = amounts + (bucket.id to value - p.step) }
                    Text("$value", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.size(width = 56.dp, height = 36.dp), textAlign = TextAlign.Center)
                    StepButton("+", "Больше: ${bucket.label}", enabled = remaining >= p.step, color = FinniColors.BlueLight, ink = FinniColors.Blue,
                        disabledColor = FinniColors.Track, disabledInk = FinniColors.NavyMuted) { amounts = amounts + (bucket.id to value + p.step) }
                }
            }
            Row {
                Text("Разложено $total из ${p.total}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(
                    if (remaining == 0) "Всё разложено ✓" else "Осталось $remaining",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (remaining == 0) FinniColors.Mood else FinniColors.Navy
                )
            }
            DoneButton(enabled = remaining == 0) { onSubmit(TaskAnswer.Allocation(amounts)) }
        }
    }
}


// ---------- Список покупок ----------

@Composable
private fun ShopListWidget(p: TaskPayload.ShopList, onSubmit: (TaskAnswer) -> Unit) {
    var selected by remember { mutableStateOf(setOf<String>()) }
    val total = p.items.filter { it.id in selected }.sumOf { it.price }
    val over = total > p.budget
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            p.items.forEach { item ->
                ShopListRow(item, checked = item.id in selected) {
                    selected = if (item.id in selected) selected - item.id else selected + item.id
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Итого", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(
                    "$total из ${p.budget}" + if (over) " — больше бюджета" else "",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (over) FinniColors.Warning else FinniColors.Navy
                )
                Image(painterResource(R.drawable.ic_coin), null, Modifier.padding(start = 4.dp).size(18.dp))
            }
            // Кнопка активна и при перерасходе: ошибка — учебная ситуация с объяснением
            DoneButton(enabled = selected.isNotEmpty()) { onSubmit(TaskAnswer.Selection(selected)) }
        }
    }
}

// ---------- Общее ----------

@Composable
private fun DoneButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) { Text("Готово", style = MaterialTheme.typography.titleMedium) }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, shape, ambientColor = FinniColors.Navy.copy(alpha = 0.15f), spotColor = FinniColors.Navy.copy(alpha = 0.15f)),
        shape = shape,
        color = FinniColors.Card,
        content = content
    )
}

/** Строка списка покупок: нажимается вся строка, флажок только показывает состояние. */
@Composable
private fun ShopListRow(item: TaskPayload.ShopList.Item, checked: Boolean, onToggle: () -> Unit) {
    Surface(
        checked = checked,
        onCheckedChange = { onToggle() },
        shape = RoundedCornerShape(16.dp),
        color = if (checked) FinniColors.BlueLight else Color.White,
        modifier = Modifier.semantics { role = Role.Checkbox },
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = null)
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
private fun HintButton(hint: String) {
    var shown by remember { mutableStateOf(false) }
    TextButton(onClick = { shown = !shown }) { Text(if (shown) "Скрыть подсказку" else "Подсказка", style = MaterialTheme.typography.labelLarge) }
    if (shown) {
        Text(hint, style = MaterialTheme.typography.bodyLarge, color = FinniColors.NavyMuted, modifier = Modifier.padding(horizontal = 8.dp))
    }
}
