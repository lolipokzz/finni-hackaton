package ru.larpinovplay.finniapp.presentation.screens.adult

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.larpinovplay.finniapp.presentation.components.RoomBackground
import ru.larpinovplay.finniapp.presentation.pet.title
import ru.larpinovplay.finniapp.presentation.task.title
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

@Composable
fun AdultScreen(onBack: () -> Unit, viewModel: AdultViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        RoomBackground()
        Column(
            Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(onClick = onBack, enabled = !state.busy) { Text("← Назад") }
            Text("Для взрослых", style = MaterialTheme.typography.headlineMedium)
            if (!state.unlocked) {
                AdultCard("Вход в родительский раздел") {
                    Text("Решите пример, чтобы открыть настройки профиля и учебный прогресс.")
                    Text("${state.first} + ${state.second} = ?", style = MaterialTheme.typography.headlineMedium)
                    OutlinedTextField(
                        value = state.answer,
                        onValueChange = viewModel::changeAnswer,
                        label = { Text("Ответ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = state.error != null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(onClick = viewModel::unlock, enabled = state.answer.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                        Text("Открыть")
                    }
                }
            } else {
                AdultCard("Чему учится ребёнок") {
                    listOf(
                        "Понимать бюджет и тратить не больше доступной суммы.",
                        "Отличать необходимое от желаемого.",
                        "Планировать покупки при ограниченном бюджете.",
                        "Ставить цель и регулярно откладывать монеты.",
                        "Объяснять последствия своих решений.",
                        "Обсуждать финансовые решения со взрослым.",
                    ).forEach { Text("• $it") }
                    Text("Обсуждайте выбор ребёнка, помогая ему принимать решения самостоятельно.")
                }
                state.snapshot?.let { snapshot ->
                    AdultCard("Общий прогресс") {
                        Text("${snapshot.pet.name} · ${snapshot.pet.growthStage.title()}", style = MaterialTheme.typography.titleMedium)
                        Text("Завершено недель: ${snapshot.state.history.size}")
                        Text("Достигнуто целей: ${snapshot.state.completedGoals.size}")
                        Text("Монеты: ${snapshot.state.balance} · В копилке: ${snapshot.state.savings}")
                        Text("Текущая цель: ${snapshot.state.goal?.name ?: "ещё не выбрана"}")
                        if (snapshot.state.demoMode) Text("Демонстрационный профиль", color = FinniColors.Blue)
                    }
                }
                AdultCard("Пройденные темы") {
                    state.topics.forEach { progress ->
                        Text(progress.topic.title(), style = MaterialTheme.typography.titleMedium)
                        Text("Выполнено ${progress.done} из ${progress.total} заданий")
                    }
                }
                AdultCard("Настройки") {
                    PreferenceSwitch("Звук", state.settings.soundEnabled, !state.busy, viewModel::setSound)
                    PreferenceSwitch("Анимации", state.settings.animationsEnabled, !state.busy, viewModel::setAnimations)
                }
                AdultCard("Демонстрационный режим") {
                    Text("Тестовый питомец, 100 монет, первая неделя. Игровые недели завершаются кнопкой, без ожидания реального времени.")
                    Text("Включение демо заменит текущий профиль и его прогресс.")
                    OutlinedButton(onClick = { viewModel.request(AdultConfirmation.DEMO) }, enabled = !state.busy) {
                        Text(if (state.snapshot?.state?.demoMode == true) "Сбросить демо" else "Включить демо")
                    }
                }
                AdultCard("Управление данными") {
                    Text("Сброс удаляет питомца и игровой прогресс. Удаление всех данных дополнительно возвращает настройки к исходным.")
                    OutlinedButton(onClick = { viewModel.request(AdultConfirmation.RESET_PROFILE) }, enabled = !state.busy) { Text("Сбросить профиль") }
                    OutlinedButton(onClick = { viewModel.request(AdultConfirmation.DELETE_ALL) }, enabled = !state.busy) { Text("Удалить все данные") }
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
    state.pending?.let { action ->
        val title = when (action) {
            AdultConfirmation.RESET_PROFILE -> "Сбросить профиль?"
            AdultConfirmation.DELETE_ALL -> "Удалить все данные?"
            AdultConfirmation.DEMO -> "Начать демо заново?"
        }
        val explanation = when (action) {
            AdultConfirmation.RESET_PROFILE -> "Питомец, монеты, покупки, накопления и учебный прогресс будут удалены. Настройки сохранятся. Откроется создание нового питомца."
            AdultConfirmation.DELETE_ALL -> "Питомец и весь игровой прогресс будут удалены. Настройки звука, анимаций и подсказок вернутся к исходным. Откроется создание нового питомца."
            AdultConfirmation.DEMO -> "Текущий питомец и весь игровой прогресс будут заменены тестовым профилем: Финни Демо, 100 монет, неделя 1."
        }
        AlertDialog(
            onDismissRequest = viewModel::dismissConfirmation,
            title = { Text(title) },
            text = { Text("$explanation Отменить это действие после подтверждения нельзя.") },
            confirmButton = { TextButton(onClick = { viewModel.confirm(onBack) }, enabled = !state.busy) { Text(if (state.busy) "Подождите…" else "Подтвердить") } },
            dismissButton = { TextButton(onClick = viewModel::dismissConfirmation, enabled = !state.busy) { Text("Отмена") } },
        )
    }
}

@Composable
private fun AdultCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = FinniColors.Card, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun PreferenceSwitch(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
