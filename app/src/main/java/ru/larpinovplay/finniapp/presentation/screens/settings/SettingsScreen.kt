package ru.larpinovplay.finniapp.presentation.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.settings.model.AppSettings
import ru.larpinovplay.finniapp.presentation.components.CardSticker
import ru.larpinovplay.finniapp.presentation.components.DashedDivider
import ru.larpinovplay.finniapp.presentation.components.RoomBackground
import ru.larpinovplay.finniapp.presentation.components.ScreenHeader
import ru.larpinovplay.finniapp.presentation.components.creamCard
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/** Три карточки знакомства с игрой: показываются при первом запуске и здесь по запросу. */
private data class IntroCard(val icon: Int, val tint: Color, val title: String, val text: String)

private val introCards = listOf(
    IntroCard(
        R.drawable.ic_meter_smile, Color(0xFFFFF5C9), "Это Финни",
        "Ему нужна твоя помощь. Ты решаешь, на что тратить монеты, и от этого зависят его сытость, настроение и рост.",
    ),
    IntroCard(
        R.drawable.ic_deed_plan, Color(0xFFE6EEFF), "Три решения",
        "Обязательное — еда. Необязательное — радости: лимонад, мыло, щётка. Копилка — откладываешь на цель.",
    ),
    IntroCard(
        R.drawable.ic_sun_small, Color(0xFFFFF0E6), "Финни растёт",
        "Каждое дело недели — шаг роста: Финни сыт, не скучает, копилка и траты по плану. Так он станет подростком, а потом взрослым.",
    ),
)

/** Словарик простыми словами. Слова те же, что на экранах игры. */
private val glossary = listOf(
    "Монеты" to "Игровые деньги. Каждую неделю приходят карманные, немного — за задания и приключения.",
    "Обязательное" to "Без этого Финни плохо: еда.",
    "Необязательное" to "Приятно, но можно подождать: лимонад, чипсы, мыло.",
    "Копилка" to "Монеты, которые ты откладываешь на цель. Забрать их можно, но цель отодвинется.",
    "Цель" to "То, на что ты копишь. У цели есть цена.",
    "План недели" to "В начале недели ты раскладываешь монеты: на обязательное, необязательное и в копилку.",
    "Неделя" to "Игровой период. В конце — итоги: какие из четырёх дел получились.",
    "Сытость" to "Поел ли Финни. Падает каждую неделю, растёт от еды.",
    "Настроение" to "Как Финни себя чувствует. Растёт от радостей, падает понемногу каждую неделю.",
)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAdult: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreenContent(
        settings = settings,
        onAction = viewModel::onAction,
        onBack = onBack,
        onOpenAdult = onOpenAdult,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreenContent(
    settings: AppSettings,
    onAction: (SettingsAction) -> Unit,
    onBack: () -> Unit,
    onOpenAdult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Раскрытие карточек «Как играть» и «Словарик» — вид одного экрана, в ViewModel ему делать нечего
    var introOpen by rememberSaveable { mutableStateOf(false) }
    var glossaryOpen by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        RoomBackground()
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
            Spacer(Modifier.height(10.dp))
            ScreenHeader("Настройки", onBack)
            Spacer(Modifier.height(14.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
                modifier = Modifier.weight(1f),
            ) {
                item {
                    Column(
                        Modifier.fillMaxWidth().creamCard(RoundedCornerShape(28.dp), elevation = 8.dp).padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        ToggleRow(R.drawable.ic_sound, Color(0xFFE6EEFF), "Звуки", "Сигналы при покупках и наградах", settings.soundEnabled) {
                            onAction(SettingsAction.SetSound(it))
                        }
                        DashedDivider()
                        ToggleRow(R.drawable.ic_sparkles, Color(0xFFFFF5C9), "Анимации", "Финни двигается и машет", settings.animationsEnabled) {
                            onAction(SettingsAction.SetAnimations(it))
                        }
                        DashedDivider()
                        ToggleRow(R.drawable.ic_bulb, Color(0xFFFFF0E6), "Подсказки", "Финни говорит, что сделать дальше", settings.tipsEnabled) {
                            onAction(SettingsAction.SetTips(it))
                        }
                        DashedDivider()
                        VoiceRepeatRow(settings.voiceRepeatEnabled) { onAction(SettingsAction.SetVoiceRepeat(it)) }
                    }
                }

                item {
                    ExpandableCard(
                        icon = R.drawable.ic_scene_story,
                        tint = Color(0xFFE6F8F2),
                        title = "Как играть",
                        subtitle = "Три карточки, которые ты видел в начале",
                        open = introOpen,
                        onToggle = { introOpen = !introOpen },
                    ) {
                        introCards.forEach { card ->
                            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CardSticker(card.icon, card.tint, size = 44.dp, iconScale = 0.6f)
                                Column(Modifier.weight(1f)) {
                                    Text(card.title, fontSize = 16.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink)
                                    Text(card.text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
                                }
                            }
                        }
                    }
                }

                item {
                    ExpandableCard(
                        icon = R.drawable.ic_scene_question,
                        tint = Color(0xFFFFF5C9),
                        title = "Словарик",
                        subtitle = "Что значат слова в игре",
                        open = glossaryOpen,
                        onToggle = { glossaryOpen = !glossaryOpen },
                    ) {
                        glossary.forEach { (term, text) ->
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    term, fontSize = 14.sp, fontWeight = FontWeight.Black, color = FinniColors.Teal,
                                    modifier = Modifier.clip(CircleShape).background(FinniColors.CardMint).padding(horizontal = 10.dp, vertical = 3.dp),
                                )
                                Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
                            }
                        }
                    }
                }

                item {
                    Row(
                        Modifier.fillMaxWidth().creamCard(RoundedCornerShape(28.dp), elevation = 8.dp).background(FinniColors.CardMint).padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CardSticker(R.drawable.ic_coin, Color.White, size = 44.dp, iconScale = 0.62f)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("О приложении", fontSize = 16.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink)
                            Text(
                                "«Питомец Финни» учит планировать монеты, отличать обязательное от необязательного и копить на цель. " +
                                    "Здесь нет настоящих денег, рекламы и покупок. Все данные хранятся только на этом устройстве.",
                                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color(0xFF0B5E4F),
                            )
                        }
                    }
                }

                // Вход для взрослых спрятан здесь, а не на главном экране: дальше — арифметический барьер
                item {
                    Surface(
                        onClick = onOpenAdult,
                        shape = RoundedCornerShape(28.dp),
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth().creamCard(RoundedCornerShape(28.dp), elevation = 8.dp).semantics { role = Role.Button },
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CardSticker(R.drawable.ic_lock, FinniColors.Pebble, size = 48.dp, iconScale = 0.58f)
                            Column(Modifier.weight(1f)) {
                                Text("Для взрослых", fontSize = 16.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink)
                                Text("Прогресс ребёнка, сброс профиля, демо-режим", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
                            }
                            Image(painterResource(R.drawable.ic_arrow_right), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(FinniColors.InkMuted))
                        }
                    }
                }
            }
        }
    }
}

// ---------- Элементы ----------

/** Настройка: наклейка, что это и зачем, переключатель. Вся строка — одна большая цель для пальца. */
@Composable
private fun ToggleRow(icon: Int, tint: Color, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .semantics { stateDescription = if (checked) "Включено" else "Выключено" }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CardSticker(icon, tint, size = 44.dp, iconScale = 0.6f)
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink)
            Text(subtitle, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
        }
        // Состояние продублировано словом: цвет не единственный носитель смысла
        Text(
            if (checked) "Вкл" else "Выкл", fontSize = 13.sp, fontWeight = FontWeight.Black,
            color = if (checked) FinniColors.Teal else FinniColors.InkMuted,
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = FinniColors.Teal,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = FinniColors.Pebble,
                uncheckedThumbColor = FinniColors.InkMuted,
                uncheckedBorderColor = FinniColors.Dashed,
            ),
        )
    }
}

/**
 * Повтор слов — единственное, чему нужен микрофон. По умолчанию выключен; доступ к микрофону спрашивается только в
 * момент включения этого переключателя, а не сам по себе на главном экране.
 */
@Composable
private fun VoiceRepeatRow(setting: Boolean, onChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    fun granted() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    var hasMic by remember { mutableStateOf(granted()) }
    var denied by remember { mutableStateOf(false) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        hasMic = ok
        denied = !ok
        if (ok) onChange(true)
    }
    ToggleRow(
        R.drawable.ic_mic, Color(0xFFFFE6F0), "Кот повторяет слова",
        if (denied) "Нужен доступ к микрофону: его можно выдать в настройках телефона" else "Слушает микрофон, звук никуда не уходит",
        setting && hasMic,
    ) { on ->
        when {
            !on -> onChange(false)
            granted() -> { hasMic = true; denied = false; onChange(true) }
            else -> request.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

/** Карточка, которая раскрывается по нажатию: наклейка, заголовок, стрелка вниз или вверх. */
@Composable
private fun ExpandableCard(
    icon: Int,
    tint: Color,
    title: String,
    subtitle: String,
    open: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    Surface(
        onClick = onToggle,
        shape = shape,
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .creamCard(shape, elevation = 8.dp)
            .semantics {
                role = Role.Button
                stateDescription = if (open) "Открыто" else "Свёрнуто"
            },

    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CardSticker(icon, tint, size = 48.dp, iconScale = 0.6f)
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 17.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink)
                    Text(subtitle, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
                }
                Box(Modifier.size(36.dp).clip(CircleShape).background(FinniColors.Pebble), contentAlignment = Alignment.Center) {
                    Image(
                        painterResource(R.drawable.ic_arrow_right), null,
                        Modifier.size(16.dp).rotate(if (open) -90f else 90f),
                        colorFilter = ColorFilter.tint(FinniColors.Ink),
                    )
                }
            }
            AnimatedVisibility(visible = open) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashedDivider()
                    content()
                }
            }
        }
    }
}
