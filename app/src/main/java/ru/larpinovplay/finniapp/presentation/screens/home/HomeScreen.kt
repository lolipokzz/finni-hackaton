package ru.larpinovplay.finniapp.presentation.screens.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetGrowthStage
import ru.larpinovplay.finniapp.domain.pet.model.PetLook
import ru.larpinovplay.finniapp.domain.pet.model.PetMood
import ru.larpinovplay.finniapp.domain.pet.model.PetSpecies
import ru.larpinovplay.finniapp.presentation.components.PetModel3D
import ru.larpinovplay.finniapp.presentation.components.RoomBackground
import ru.larpinovplay.finniapp.presentation.pet.modelAsset
import ru.larpinovplay.finniapp.presentation.theme.FinniAppTheme
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * Главный экран, ТЗ 2.5.3, в стиле референса: комната, белые карточки, крупный питомец.
 *
 * Правила UX (docs/07-screens.md): тапаемые элементы ≥ 48 dp, текст ≥ 16 sp,
 * каждый показатель — иконка + подпись + число, цвет не единственный носитель смысла.
 *
 * Питомец рисуется в SurfaceView поверх Compose с прозрачным фоном: всё, что лежит под
 * прозрачными пикселями, видно, поэтому облачко-подсказку можно класть рядом с ним.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        RoomBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            TopResourcesRow(state, onAction)
            Spacer(Modifier.height(12.dp))
            StatsRow(state.stats)
            WeekLine(state)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                PetArea(state, modifier = Modifier.align(Alignment.BottomCenter))
                state.tip?.let {
                    TipBubble(
                        text = it,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 12.dp)
                    )
                }
            }

            NameCard(state)
            Spacer(Modifier.height(10.dp))
            state.activeTask?.let { TaskCard(it) { onAction(HomeAction.OpenSection(HomeSection.TASKS)) } }
            if (state.planConfirmed) {
                FinishWeekButton { onAction(HomeAction.FinishWeek) }
            }
            Spacer(Modifier.height(10.dp))
            BottomMenu(selected = state.suggestedSection, onOpen = { onAction(HomeAction.OpenSection(it)) })
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ---------- Общие элементы ----------

private val CardShape = RoundedCornerShape(24.dp)

@Composable
private fun WhiteCard(
    modifier: Modifier = Modifier,
    color: Color = FinniColors.Card,
    shape: RoundedCornerShape = CardShape,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val base = modifier.shadow(6.dp, shape, ambientColor = FinniColors.Navy.copy(alpha = 0.15f), spotColor = FinniColors.Navy.copy(alpha = 0.15f))
    if (onClick != null) {
        Surface(onClick = onClick, modifier = base, shape = shape, color = color, content = content)
    } else {
        Surface(modifier = base, shape = shape, color = color, content = content)
    }
}

@Composable
private fun RoundIconButton(@DrawableRes icon: Int, contentDescription: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = FinniColors.Lavender,
        modifier = Modifier
            .size(48.dp)
            .shadow(6.dp, CircleShape, ambientColor = FinniColors.Navy.copy(alpha = 0.12f), spotColor = FinniColors.Navy.copy(alpha = 0.12f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(painterResource(icon), contentDescription = contentDescription, modifier = Modifier.size(26.dp))
        }
    }
}

// ---------- Шапка: настройки, монеты, копилка и цель, раздел взрослого ----------

@Composable
private fun TopResourcesRow(state: HomeUiState, onAction: (HomeAction) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundIconButton(R.drawable.ic_gear, "Подсказка и настройки") {
            onAction(HomeAction.OpenSection(HomeSection.HELP))
        }
        ResourceCard(
            icon = R.drawable.ic_coin,
            label = "Монеты",
            value = "${state.balance}",
            modifier = Modifier.weight(1f),
            onClick = { onAction(HomeAction.OpenSection(HomeSection.TASKS)) }
        )
        ResourceCard(
            icon = R.drawable.ic_pig,
            label = state.goal?.name ?: "Выбери цель",
            value = state.goal?.let { "${state.savings} / ${it.cost}" } ?: "${state.savings}",
            modifier = Modifier.weight(1f),
            onClick = { onAction(HomeAction.OpenSection(HomeSection.SAVINGS)) }
        )
        RoundIconButton(R.drawable.ic_lock, "Для взрослых") {
            onAction(HomeAction.OpenSection(HomeSection.ADULT))
        }
    }
}

/** Карточка ресурса: иконка слева, подпись и значение. Ширина на 360 dp около 110 dp, поэтому всё компактно. */
@Composable
private fun ResourceCard(
    @DrawableRes icon: Int,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    WhiteCard(modifier = modifier.height(60.dp), onClick = onClick) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(icon), null, Modifier.size(32.dp))
            Column(Modifier.weight(1f).padding(start = 6.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = FinniColors.NavyMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1, softWrap = false)
            }
            Image(painterResource(R.drawable.ic_chevron), null, Modifier.size(18.dp))
        }
    }
}

// ---------- Показатели состояния ----------

@Composable
private fun StatsRow(stats: PetStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(R.drawable.ic_apple, "Сытость", stats.satiety, FinniColors.Satiety, FinniColors.CardPeach, Modifier.weight(1f))
        StatCard(R.drawable.ic_heart, "Уход", stats.care, FinniColors.Care, FinniColors.CardPink, Modifier.weight(1f))
        StatCard(R.drawable.ic_smile, "Настроение", stats.mood, FinniColors.Mood, FinniColors.CardMint, Modifier.weight(1f))
    }
}

/** Иконка + подпись + число + полоса. Низкое значение помечается словом «мало», не только цветом. */
@Composable
private fun StatCard(
    @DrawableRes icon: Int,
    label: String,
    value: Int,
    barColor: Color,
    cardColor: Color,
    modifier: Modifier = Modifier,
) {
    val low = value < 30
    WhiteCard(modifier = modifier, color = cardColor, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(icon), null, Modifier.size(24.dp))
                Spacer(Modifier.weight(1f))
                Text("$value", style = MaterialTheme.typography.titleMedium)
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = FinniColors.NavyMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(FinniColors.Track)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(value.coerceIn(0, 100) / 100f)
                        .fillMaxHeight()
                        .background(if (low) FinniColors.Warning else barColor, RoundedCornerShape(5.dp))
                )
            }
            if (low) {
                Text("мало", style = MaterialTheme.typography.labelSmall, color = FinniColors.Warning)
            }
        }
    }
}

@Composable
private fun WeekLine(state: HomeUiState) {
    val status = if (state.planConfirmed) "план составлен" else "составь план"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(12.dp), color = FinniColors.Card) {
            Text(
                "Неделя ${state.week} · $status",
                style = MaterialTheme.typography.labelMedium,
                color = FinniColors.NavyMuted,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
        if (state.demoMode) {
            Spacer(Modifier.width(8.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = FinniColors.Sunny) {
                Text("Демо", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }
    }
}

// ---------- Питомец и подсказка ----------

@Composable
private fun PetArea(state: HomeUiState, modifier: Modifier = Modifier) {
    val asset = state.petLook.modelAsset()
    if (asset != null) {
        PetModel3D(
            assetName = asset,
            tintArgb = state.petLook.color.argb,
            modifier = modifier
                .fillMaxWidth(0.72f)
                .fillMaxHeight(),
        )
    } else {
        Box(
            modifier = modifier
                .padding(bottom = 16.dp)
                .size(180.dp)
                .clip(CircleShape)
                .background(Color(state.petLook.color.argb)),
            contentAlignment = Alignment.Center
        ) {
            Text(state.petName, style = MaterialTheme.typography.headlineSmall, color = Color.White)
        }
    }
}

/** Белое облачко с лампочкой и хвостиком снизу слева. */
@Composable
private fun TipBubble(text: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.widthIn(max = 170.dp)) {
        WhiteCard(shape = RoundedCornerShape(18.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Image(painterResource(R.drawable.ic_bulb), null, Modifier.size(24.dp))
                Text(
                    text,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
        Canvas(Modifier.padding(start = 18.dp).size(16.dp, 12.dp)) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width * 0.35f, size.height)
                close()
            }
            drawPath(path, FinniColors.Card)
        }
    }
}

// ---------- Имя и настроение ----------

@Composable
private fun NameCard(state: HomeUiState) {
    WhiteCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.petName, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Text(
                    state.moodExplanation,
                    style = MaterialTheme.typography.bodyLarge,
                    color = FinniColors.NavyMuted,
                    textAlign = TextAlign.Center
                )
            }
            Surface(shape = CircleShape, color = FinniColors.BlueLight, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.ic_pencil), contentDescription = "Переименовать", Modifier.size(22.dp))
                }
            }
        }
    }
}

// ---------- Задание ----------

@Composable
private fun TaskCard(task: TaskUi, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, ambientColor = FinniColors.Blue.copy(alpha = 0.35f), spotColor = FinniColors.Blue.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .background(Brush.horizontalGradient(listOf(Color(0xFF7D95FF), Color(0xFF5B7BFF))))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(R.drawable.ic_target), null, Modifier.size(34.dp))
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text("Задание", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                Text(task.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("+${task.reward}", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Image(painterResource(R.drawable.ic_coin), null, Modifier.padding(start = 4.dp).size(24.dp))
            Image(painterResource(R.drawable.ic_chevron), null, Modifier.size(28.dp), colorFilter = ColorFilter.tint(Color.White))
        }
    }
}

@Composable
private fun FinishWeekButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .height(52.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = FinniColors.Green, contentColor = Color.White)
    ) { Text("Завершить неделю", fontSize = 18.sp) }
}

// ---------- Нижняя панель ----------

private data class MenuItem(val section: HomeSection, @DrawableRes val icon: Int, val label: String)

private val menuItems = listOf(
    MenuItem(HomeSection.PLAN, R.drawable.ic_clipboard, "План"),
    MenuItem(HomeSection.TASKS, R.drawable.ic_target, "Задания"),
    MenuItem(HomeSection.SHOP, R.drawable.ic_cart, "Магазин"),
    MenuItem(HomeSection.SAVINGS, R.drawable.ic_pig, "Копилка"),
    MenuItem(HomeSection.PROGRESS, R.drawable.ic_star, "Прогресс"),
)

@Composable
private fun BottomMenu(selected: HomeSection?, onOpen: (HomeSection) -> Unit) {
    WhiteCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            menuItems.forEach { item ->
                val isSelected = item.section == selected
                Surface(
                    onClick = { onOpen(item.section) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) FinniColors.BlueLight else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .height(78.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Image(painterResource(item.icon), null, Modifier.size(32.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            item.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) FinniColors.Blue else FinniColors.Navy,
                            maxLines = 1
                        )
                        Box(
                            Modifier
                                .padding(top = 3.dp)
                                .size(width = 26.dp, height = 5.dp)
                                .background(if (isSelected) FinniColors.Blue else Color.Transparent, RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
        }
    }
}

// ---------- Превью ----------

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HomeScreenPreview() {
    val pet = Pet(
        name = "Финни",
        look = PetLook(PetSpecies.CAT, PetColor.MINT),   // без 3D-модели, чтобы превью рисовалось
        mood = PetMood(75),
        growthStage = PetGrowthStage.BABY,
        growthProgress = 0
    )
    FinniAppTheme {
        HomeScreen(state = HomeUiState.sample(pet).copy(planConfirmed = true), onAction = {})
    }
}
