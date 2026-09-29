package ru.larpinovplay.finniapp.presentation.screens.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.pet.model.MoodLevel
import ru.larpinovplay.finniapp.presentation.components.CardDialog
import ru.larpinovplay.finniapp.presentation.components.CardSticker
import ru.larpinovplay.finniapp.presentation.components.DashedDivider
import ru.larpinovplay.finniapp.presentation.components.MeterRing
import ru.larpinovplay.finniapp.presentation.components.SoftButton
import ru.larpinovplay.finniapp.presentation.components.TealButton
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/** Кольцо показателя в окне: то же, что на главном экране, только крупнее. */
private data class Meter(val value: Int, val color: Color, val track: Color)

private data class InfoContent(
    @DrawableRes val icon: Int,
    val meter: Meter?,                        // null — не шкала, а число (монеты)
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
        meter = null,
        title = "Монеты",
        current = "Сейчас у тебя ${state.balance}",
        lines = listOf(
            "Это игровые монеты. Настоящих денег здесь нет.",
            "Откуда берутся: каждую неделю приходят карманные — 50, а когда Финни подрастёт, больше. В начале недели ты раскладываешь их по плану.",
            "Немного сверху: за уровни заданий и приключение недели. Главные деньги — карманные, поэтому важнее всего план.",
            "Куда уходят: на еду и радости в магазине или в копилку на цель.",
            "Потратить больше, чем есть, нельзя. Если не хватает, можно взять из копилки, пройти уровень заданий или подождать неделю.",
        ),
        action = "Заработать" to HomeSection.TASKS,
    )
    HomeInfo.SATIETY -> InfoContent(
        icon = R.drawable.ic_meter_apple,
        meter = Meter(state.pet.satiety.value, FinniColors.SatietyRing, FinniColors.SatietyTrack),
        title = "Сытость",
        current = "Сейчас ${state.pet.satiety.value} из 100" + if (state.pet.isHungry) " — Финни голоден" else "",
        lines = listOf(
            "Показывает, поел ли Финни.",
            "Растёт от еды из магазина: овощи +25, фрукты +30, мясо +45.",
            "Каждую неделю падает на 35, поэтому столько сытости нужно покупать каждую неделю: например, две порции овощей или мясо.",
            "Если 30 и меньше, Финни голоден. Еда — обязательное, а не радость: покупай её первой.",
        ),
        action = "В магазин" to HomeSection.SHOP,
    )
    HomeInfo.MOOD -> InfoContent(
        icon = R.drawable.ic_meter_smile,
        meter = Meter(state.pet.mood.value, FinniColors.MoodRing, FinniColors.MoodTrack),
        title = "Настроение",
        current = "Сейчас ${state.pet.mood.value} из 100 — " + when (state.pet.mood.level) {
            MoodLevel.HAPPY -> "радостный"
            MoodLevel.NEUTRAL -> "спокойный"
            MoodLevel.BORED -> "скучает"
        },
        lines = listOf(
            "Показывает, как Финни себя чувствует.",
            "Растёт от приятных покупок: лимонада, чипсов, мыла, мяса, одежды из гардероба. Одежда и достигнутые цели — кроватка, велосипед, ремонт — поднимают его и дальше, каждую неделю понемногу.",
            "За неделю настроение падает на 15, поэтому время от времени покупай Финни что-нибудь приятное.",
            "Радостный от 70, спокойный от 40, ниже — скучает. Если Финни не скучает, это одно из дел недели.",
        ),
        action = "Купить приятное" to HomeSection.SHOP,
    )
}

@Composable
fun HomeInfoDialog(info: HomeInfo, state: HomeUiState, onOpenSection: (HomeSection) -> Unit, onDismiss: () -> Unit) {
    val c = content(info, state)
    CardDialog(onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val meter = c.meter
            if (meter != null) {
                MeterRing(meter.value, meter.color, meter.track, c.icon, description = c.current, size = 84.dp)
            } else {
                CardSticker(c.icon, FinniColors.CoinPill, size = 80.dp, iconScale = 0.66f)
            }
            Text(c.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = FinniColors.Ink, modifier = Modifier.semantics { heading() })
            Text(c.current, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = FinniColors.Teal, textAlign = TextAlign.Center)
        }
        DashedDivider()
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            c.lines.forEach { line ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.padding(top = 7.dp).size(8.dp).clip(CircleShape).background(c.meter?.color ?: Color(0xFFFFB020)))
                    Text(line, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
                }
            }
        }
        val action = c.action
        if (action != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TealButton(action.first, null, { onDismiss(); onOpenSection(action.second) })
                SoftButton("Понятно", onDismiss)
            }
        } else {
            TealButton("Понятно", R.drawable.ic_check, onDismiss)
        }
    }
}
