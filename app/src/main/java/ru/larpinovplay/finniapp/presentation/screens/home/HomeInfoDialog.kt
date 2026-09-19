package ru.larpinovplay.finniapp.presentation.screens.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.pet.model.MoodLevel
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

private data class InfoContent(
    @DrawableRes val icon: Int,
    val title: String,
    val current: String,
    val lines: List<String>,
    val action: Pair<String, HomeSection>?,   // кнопка действия и раздел, куда она ведёт
)

/**
 * Пояснение показателя простыми словами (ТЗ 2.2 «объяснимость», 2.5.4, 2.5.9):
 * что это, сколько сейчас, от чего растёт и падает, что можно сделать.
 * Числа совпадают с правилами GameEngine и docs/04-rules-and-formulas.md.
 */
private fun content(info: HomeInfo, state: HomeUiState): InfoContent = when (info) {
    HomeInfo.COINS -> InfoContent(
        icon = R.drawable.ic_coin,
        title = "Монеты",
        current = "Сейчас у тебя ${state.balance}",
        lines = listOf(
            "Это игровые монеты. Настоящих денег здесь нет.",
            "Откуда берутся: за задания дают от 15 до 20, а каждую новую неделю приходит 60 карманных.",
            "Куда уходят: на еду и желаемое в магазине или в копилку на цель.",
            "Потратить больше, чем есть, нельзя. Если не хватает, выполни задание или подожди неделю.",
        ),
        action = "Заработать" to HomeSection.TASKS,
    )
    HomeInfo.SATIETY -> InfoContent(
        icon = R.drawable.ic_apple,
        title = "Сытость",
        current = "Сейчас ${state.pet.satiety.value} из 100" + if (state.pet.isHungry) " — Финни голоден" else "",
        lines = listOf(
            "Показывает, поел ли Финни.",
            "Растёт от еды из магазина: овощи +25, фрукты +30, мясо +45.",
            "Каждую неделю падает на 35, поэтому еда нужна каждую неделю.",
            "Если меньше 30, Финни голоден и грустит. Это нужное, а не желаемое: покупай еду первой.",
        ),
        action = "В магазин" to HomeSection.SHOP,
    )
    HomeInfo.MOOD -> InfoContent(
        icon = R.drawable.ic_smile,
        title = "Настроение",
        current = "Сейчас ${state.pet.mood.value} из 100 — " + when (state.pet.mood.level) {
            MoodLevel.HAPPY -> "радостный"
            MoodLevel.NEUTRAL -> "спокойный"
            MoodLevel.SAD -> "грустный"
        },
        lines = listOf(
            "Показывает, как Финни себя чувствует.",
            "Растёт от еды, от желаемого вроде лимонада и чипсов и на 30, когда ты достигаешь цели.",
            "В конце недели: +20, если ты купил еду и отложил в копилку; −15, если не сделал ни того, ни другого. Ещё −10 просто за прошедшую неделю.",
            "Радостный от 70, спокойный от 40, ниже — грустный. Плохое настроение легко поправить на следующей неделе.",
        ),
        action = null,
    )
}

@Composable
fun HomeInfoDialog(info: HomeInfo, state: HomeUiState, onOpenSection: (HomeSection) -> Unit, onDismiss: () -> Unit) {
    val c = content(info, state)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White,
        icon = { Image(painterResource(c.icon), null, Modifier.size(48.dp)) },
        title = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(c.title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Text(c.current, style = MaterialTheme.typography.titleMedium, color = FinniColors.Blue, textAlign = TextAlign.Center)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                c.lines.forEach { line ->
                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                        Text("•", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.Blue)
                        Text(line, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        },
        confirmButton = {
            val action = c.action
            if (action != null) {
                Button(
                    onClick = { onDismiss(); onOpenSection(action.second) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(48.dp)
                ) { Text(action.first, style = MaterialTheme.typography.labelLarge) }
            } else {
                Button(onClick = onDismiss, shape = RoundedCornerShape(16.dp), modifier = Modifier.height(48.dp)) {
                    Text("Понятно", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        dismissButton = if (c.action != null) {
            { TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) { Text("Понятно", style = MaterialTheme.typography.labelLarge) } }
        } else null
    )
}
