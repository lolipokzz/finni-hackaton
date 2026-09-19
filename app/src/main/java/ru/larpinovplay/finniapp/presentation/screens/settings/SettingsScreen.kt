package ru.larpinovplay.finniapp.presentation.screens.settings

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.settings.model.AppSettings
import ru.larpinovplay.finniapp.presentation.components.RoomBackground
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/** Три карточки знакомства с игрой (ТЗ 2.5.1): показываются при первом запуске и здесь по запросу. */
private data class IntroCard(val icon: Int, val title: String, val text: String)

private val introCards = listOf(
    IntroCard(R.drawable.ic_pig, "Это Финни", "Ему нужна твоя помощь. Ты решаешь, на что тратить монеты, и от этого зависит его настроение и рост."),
    IntroCard(R.drawable.ic_cart, "Три решения", "Потратить на нужное — еда. Потратить на желаемое — игрушки. Отложить — в копилку на цель."),
    IntroCard(R.drawable.ic_star, "Финни растёт", "Каждую неделю покупай еду и откладывай хоть немного. Так Финни станет Подростком, а потом Взрослым."),
)

/** Словарик простыми словами (ТЗ 2.5.11). */
private val glossary = listOf(
    "Монеты" to "Игровые деньги. Их дают за задания и каждую новую неделю.",
    "Нужное" to "Без этого Финни плохо: еда.",
    "Желаемое" to "Приятно, но можно подождать: лимонад, чипсы.",
    "Копилка" to "Монеты, которые ты откладываешь на цель. Тратить их нельзя, пока не решишь сам.",
    "Цель" to "То, на что ты копишь. У цели есть цена.",
    "Неделя" to "Игровой период. В конце недели Финни получает оценку за твои решения.",
    "Сытость" to "Показывает, поел ли Финни. Падает каждую неделю, растёт от еды.",
    "Настроение" to "Как Финни себя чувствует. Растёт от радости и хороших недель.",
)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreenContent(
        settings = settings,
        onSettingsChange = viewModel::onSettingsChange,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreenContent(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Раскрытие карточек «Как играть» и «Словарик» — вид одного экрана, в ViewModel ему делать нечего
    var introOpen by remember { mutableStateOf(false) }
    var glossaryOpen by remember { mutableStateOf(false) }

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
                Card {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        ToggleRow("Звуки", "Сигналы при покупках и наградах", settings.soundEnabled) {
                            onSettingsChange(settings.copy(soundEnabled = it))
                        }
                        ToggleRow("Анимации", "Финни двигается и машет", settings.animationsEnabled) {
                            onSettingsChange(settings.copy(animationsEnabled = it))
                        }
                        ToggleRow("Подсказки", "Облачко с советом на главном экране", settings.tipsEnabled) {
                            onSettingsChange(settings.copy(tipsEnabled = it))
                        }
                    }
                }
            }

            item {
                ExpandableCard(
                    icon = R.drawable.ic_bulb,
                    title = "Как играть",
                    subtitle = "Три карточки, которые ты видел в начале",
                    open = introOpen,
                    onToggle = { introOpen = !introOpen }
                ) {
                    introCards.forEach { card ->
                        Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .background(FinniColors.BlueLight, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) { Image(painterResource(card.icon), null, Modifier.size(28.dp)) }
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(card.title, style = MaterialTheme.typography.titleMedium)
                                Text(card.text, style = MaterialTheme.typography.bodyMedium, color = FinniColors.NavyMuted)
                            }
                        }
                    }
                }
            }

            item {
                ExpandableCard(
                    icon = R.drawable.ic_clipboard,
                    title = "Словарик",
                    subtitle = "Что значат слова в игре",
                    open = glossaryOpen,
                    onToggle = { glossaryOpen = !glossaryOpen }
                ) {
                    glossary.forEach { (term, text) ->
                        Column(Modifier.padding(vertical = 6.dp)) {
                            Text(term, style = MaterialTheme.typography.titleMedium)
                            Text(text, style = MaterialTheme.typography.bodyMedium, color = FinniColors.NavyMuted)
                        }
                    }
                }
            }

            item {
                Card(color = FinniColors.CardMint) {
                    Column(Modifier.padding(16.dp)) {
                        Text("О приложении", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "«Питомец Финни» учит планировать монеты, отличать нужное от желаемого и копить на цель. " +
                                "Здесь нет настоящих денег, рекламы и покупок. Все данные хранятся только на этом устройстве.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FinniColors.NavyMuted
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("Сброс или удаление профиля — в разделе «Для взрослых» (замок на главном экране).", style = MaterialTheme.typography.labelSmall, color = FinniColors.NavyMuted)
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

// ---------- Элементы ----------

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
        Text("Настройки", style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    // Вся строка переключает настройку: крупная цель для детского пальца
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = FinniColors.NavyMuted)
        }
        // Состояние продублировано словом: цвет не единственный носитель смысла (ТЗ 3.6)
        Text(if (checked) "Вкл" else "Выкл", style = MaterialTheme.typography.labelMedium, color = FinniColors.NavyMuted, modifier = Modifier.padding(end = 8.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = FinniColors.Blue, checkedThumbColor = Color.White)
        )
    }
}

@Composable
private fun ExpandableCard(
    icon: Int,
    title: String,
    subtitle: String,
    open: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Card(onClick = onToggle) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .background(FinniColors.BlueLight, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) { Image(painterResource(icon), null, Modifier.size(30.dp)) }
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, style = MaterialTheme.typography.labelMedium, color = FinniColors.NavyMuted)
                }
                Text(if (open) "Свернуть" else "Открыть", style = MaterialTheme.typography.labelMedium, color = FinniColors.Blue)
            }
            AnimatedVisibility(visible = open) {
                Column(Modifier.padding(top = 8.dp)) { content() }
            }
        }
    }
}

@Composable
private fun Card(color: Color = FinniColors.Card, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val base = Modifier
        .fillMaxWidth()
        .shadow(6.dp, shape, ambientColor = FinniColors.Navy.copy(alpha = 0.15f), spotColor = FinniColors.Navy.copy(alpha = 0.15f))
    if (onClick != null) Surface(onClick = onClick, modifier = base, shape = shape, color = color, content = content)
    else Surface(modifier = base, shape = shape, color = color, content = content)
}
