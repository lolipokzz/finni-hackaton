package ru.larpinovplay.finniapp.presentation.screens.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.task.model.TaskAnswer
import ru.larpinovplay.finniapp.domain.task.model.TaskPayload
import ru.larpinovplay.finniapp.presentation.components.StepButton
import ru.larpinovplay.finniapp.presentation.components.TealButton
import ru.larpinovplay.finniapp.presentation.components.creamCard
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * Виджет упражнения по его типу: выбор действия, раскладка суммы, список покупок. Ввод ребёнка живёт в
 * rememberSaveable, поэтому вызывающий оборачивает виджет в `key(task.id)`: у каждого упражнения свой ввод.
 * [enabled] = false — ответ уже проверен: виджет показывает выбор, но менять его нельзя.
 * Цвета — как у вопросов приключения: белые варианты, выбранный — мятный с бирюзовой рамкой.
 */
@Composable
fun TaskWidget(payload: TaskPayload, enabled: Boolean, onSubmit: (TaskAnswer) -> Unit) {
    when (payload) {
        is TaskPayload.Choice -> ChoiceWidget(payload, enabled, onSubmit)
        is TaskPayload.Allocate -> AllocateWidget(payload, enabled, onSubmit)
        is TaskPayload.ShopList -> ShopListWidget(payload, enabled, onSubmit)
    }
}

/** Карточка упражнения: кремовая наклейка, как все карточки игры. */
@Composable
fun TaskCard(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().creamCard(RoundedCornerShape(26.dp), elevation = 8.dp)) { content() }
}

/** «Подсказка»: раскрывает строку-подсказку под вопросом. Раскрыта ли — только вид, поэтому в экране. */
@Composable
fun HintButton(hint: String) {
    var shown by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { shown = !shown }) {
        Text(if (shown) "Скрыть подсказку" else "Подсказка", style = MaterialTheme.typography.labelLarge, color = FinniColors.Teal)
    }
    if (shown) {
        Text(hint, style = MaterialTheme.typography.bodyLarge, color = FinniColors.InkMuted, modifier = Modifier.padding(horizontal = 8.dp))
    }
}

// ---------- Выбор действия ----------

@Composable
private fun ChoiceWidget(p: TaskPayload.Choice, enabled: Boolean, onSubmit: (TaskAnswer) -> Unit) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        p.options.forEachIndexed { i, option ->
            val isSelected = option.id == selected
            Surface(
                selected = isSelected,
                onClick = { selected = option.id },
                enabled = enabled,
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) FinniColors.CardMint else Color.White,
                border = BorderStroke(if (isSelected) 3.dp else 2.dp, if (isSelected) FinniColors.Teal else FinniColors.Dashed),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .semantics { role = Role.RadioButton },
            ) {
                Row(
                    Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Буква варианта — только вид: TalkBack читает сам вариант
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(if (isSelected) FinniColors.Teal else FinniColors.Sunny)
                            .clearAndSetSemantics {},
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Image(painterResource(R.drawable.ic_check), null, Modifier.size(16.dp))
                        } else {
                            Text(LETTERS.getOrElse(i) { "${i + 1}" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = FinniColors.Ink)
                        }
                    }
                    Text(option.text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.ExtraBold, color = FinniColors.Ink, modifier = Modifier.weight(1f))
                }
            }
        }
        CheckButton(enabled = enabled && selected != null) { selected?.let { onSubmit(TaskAnswer.Choice(it)) } }
    }
}

private val LETTERS = listOf("А", "Б", "В", "Г", "Д")

// ---------- Раскладка суммы ----------

@Composable
private fun AllocateWidget(p: TaskPayload.Allocate, enabled: Boolean, onSubmit: (TaskAnswer) -> Unit) {
    // Ввод ребёнка переживает пересоздание экрана (шрифт, тема) и выгрузку процесса
    var amounts by rememberSaveable(stateSaver = mapSaver({ it }, { saved -> saved.mapValues { (_, v) -> v as Int } })) {
        mutableStateOf(p.buckets.associate { it.id to 0 })
    }
    val total = amounts.values.sum()
    val remaining = p.total - total
    TaskCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            p.buckets.forEach { bucket ->
                val value = amounts[bucket.id] ?: 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(bucket.label, style = MaterialTheme.typography.titleMedium, color = FinniColors.Ink, modifier = Modifier.weight(1f))
                    StepButton("−", "Меньше: ${bucket.label}", enabled = enabled && value > 0) { amounts = amounts + (bucket.id to value - p.step) }
                    Text(
                        "$value", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = FinniColors.Ink,
                        textAlign = TextAlign.Center, modifier = Modifier.size(width = 56.dp, height = 36.dp),
                    )
                    StepButton("+", "Больше: ${bucket.label}", enabled = enabled && remaining >= p.step) { amounts = amounts + (bucket.id to value + p.step) }
                }
            }
            Row {
                Text("Разложено $total из ${p.total}", style = MaterialTheme.typography.bodyLarge, color = FinniColors.InkMuted, modifier = Modifier.weight(1f))
                Text(
                    if (remaining == 0) "Всё разложено ✓" else "Осталось $remaining",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (remaining == 0) FinniColors.Teal else FinniColors.Ink,
                )
            }
            CheckButton(enabled = enabled && remaining == 0) { onSubmit(TaskAnswer.Allocation(amounts)) }
        }
    }
}

// ---------- Список покупок ----------

@Composable
private fun ShopListWidget(p: TaskPayload.ShopList, enabled: Boolean, onSubmit: (TaskAnswer) -> Unit) {
    var selected by rememberSaveable(stateSaver = listSaver<Set<String>, String>({ it.toList() }, { it.toSet() })) {
        mutableStateOf(emptySet())
    }
    val total = p.items.filter { it.id in selected }.sumOf { it.price }
    val over = total > p.budget
    TaskCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            p.items.forEach { item ->
                ShopListRow(item, checked = item.id in selected, enabled = enabled) {
                    selected = if (item.id in selected) selected - item.id else selected + item.id
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Итого", style = MaterialTheme.typography.bodyLarge, color = FinniColors.InkMuted, modifier = Modifier.weight(1f))
                Text(
                    "$total из ${p.budget}" + if (over) " — больше бюджета" else "",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (over) FinniColors.WarnInk else FinniColors.Ink,
                )
                Image(painterResource(R.drawable.ic_coin), null, Modifier.padding(start = 4.dp).size(18.dp))
            }
            // Кнопка активна и при перерасходе: ошибка — учебная ситуация с объяснением
            CheckButton(enabled = enabled && selected.isNotEmpty()) { onSubmit(TaskAnswer.Selection(selected)) }
        }
    }
}

/** Строка списка покупок: нажимается вся строка, флажок только показывает состояние. */
@Composable
private fun ShopListRow(item: TaskPayload.ShopList.Item, checked: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    Surface(
        checked = checked,
        onCheckedChange = { onToggle() },
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = if (checked) FinniColors.CardMint else Color.White,
        border = BorderStroke(2.dp, if (checked) FinniColors.Teal else FinniColors.Dashed),
        modifier = Modifier.semantics { role = Role.Checkbox },
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = checked, onCheckedChange = null, enabled = enabled,
                colors = CheckboxDefaults.colors(checkedColor = FinniColors.Teal, uncheckedColor = FinniColors.InkMuted),
            )
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.Ink)
                Text(if (item.mandatory) "обязательное" else "необязательное", style = MaterialTheme.typography.labelSmall, color = FinniColors.InkMuted)
            }
            Text("${item.price}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = FinniColors.Ink)
            Image(painterResource(R.drawable.ic_coin), null, Modifier.padding(start = 4.dp).size(18.dp))
        }
    }
}

// ---------- Общее ----------

@Composable
private fun CheckButton(enabled: Boolean, onClick: () -> Unit) {
    TealButton("Проверить", R.drawable.ic_check, onClick, enabled = enabled)
}
