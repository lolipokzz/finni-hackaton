package ru.larpinovplay.finniapp.presentation.screens.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.content.Feedback
import ru.larpinovplay.finniapp.domain.content.FeedbackKey
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetLook
import ru.larpinovplay.finniapp.domain.pet.model.PetSpecies
import ru.larpinovplay.finniapp.presentation.components.PetHostState
import ru.larpinovplay.finniapp.presentation.components.PetSpec
import ru.larpinovplay.finniapp.presentation.components.RoomBackground
import ru.larpinovplay.finniapp.presentation.feedback.LocalFeedback
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
    onOpenSection: (HomeSection) -> Unit,
    petHost: PetHostState,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    state?.let {
        HomeScreenContent(
            state = it,
            onAction = { action ->
                if (action is HomeAction.OpenSection) onOpenSection(action.section) else viewModel.onAction(action)
            },
            petHost = petHost,
            modifier = modifier,
        )
    }
}

@Composable
fun HomeScreenContent(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
    petHost: PetHostState? = null,   // null — превью и тесты: 3D-питомец не рисуется
) {
    Box(modifier = modifier.fillMaxSize()) {
        RoomBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            Spacer(Modifier.height(6.dp))
            TopResourcesRow(state, onAction, onInfo = { onAction(HomeAction.ShowInfo(it)) })
            Spacer(Modifier.height(8.dp))
            StatsRow(state.pet, onInfo = { onAction(HomeAction.ShowInfo(it)) })

            // Питомец занимает всё место между шапкой и действиями; плашка недели и подсказка лежат поверх
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                PetArea(state, petHost, modifier = Modifier.align(Alignment.BottomCenter))
                WeekLine(state, modifier = Modifier.align(Alignment.TopStart).padding(top = 8.dp))
                state.tip?.let {
                    TipBubble(
                        text = it.text(),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp)
                    )
                }
            }

            NameCard(state)
            Spacer(Modifier.height(6.dp))
            state.activeTask?.let { TaskCard(it) { onAction(HomeAction.OpenSection(HomeSection.TASKS)) } }
            FinishWeekButton { onAction(HomeAction.FinishWeek) }
            Spacer(Modifier.height(6.dp))
            BottomMenu(selected = state.suggestedSection, onOpen = { onAction(HomeAction.OpenSection(it)) })
            Spacer(Modifier.height(6.dp))
        }
    }
    state.info?.let {
        HomeInfoDialog(
            info = it,
            state = state,
            onOpenSection = { section -> onAction(HomeAction.OpenSection(section)) },
            onDismiss = { onAction(HomeAction.DismissInfo) }
        )
    }
    state.weekSummary?.let { WeekSummaryDialog(it, onDismiss = { onAction(HomeAction.DismissWeekSummary) }) }
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
            .size(44.dp)
            .shadow(6.dp, CircleShape, ambientColor = FinniColors.Navy.copy(alpha = 0.12f), spotColor = FinniColors.Navy.copy(alpha = 0.12f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(painterResource(icon), contentDescription = contentDescription, modifier = Modifier.size(22.dp))
        }
    }
}

// ---------- Шапка: настройки, монеты, копилка и цель, раздел взрослого ----------

@Composable
private fun TopResourcesRow(state: HomeUiState, onAction: (HomeAction) -> Unit, onInfo: (HomeInfo) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundIconButton(R.drawable.ic_gear, "Настройки") {
            onAction(HomeAction.OpenSection(HomeSection.SETTINGS))
        }
        ResourceCard(
            icon = R.drawable.ic_coin,
            label = "Монеты",
            value = "${state.balance}",
            modifier = Modifier.weight(1f),
            onClick = { onInfo(HomeInfo.COINS) }
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
    WhiteCard(modifier = modifier.height(44.dp), shape = RoundedCornerShape(22.dp), onClick = onClick) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(icon), null, Modifier.size(26.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = FinniColors.NavyMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp))
            Spacer(Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1, softWrap = false)
            Image(painterResource(R.drawable.ic_chevron), null, Modifier.size(18.dp))
        }
    }
}

// ---------- Показатели состояния ----------

@Composable
private fun StatsRow(pet: Pet, onInfo: (HomeInfo) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(R.drawable.ic_apple, "Сытость", pet.satiety.value, FinniColors.Satiety, FinniColors.CardPeach, Modifier.weight(1f)) { onInfo(HomeInfo.SATIETY) }
        StatCard(R.drawable.ic_smile, "Настроение", pet.mood.value, FinniColors.Mood, FinniColors.CardMint, Modifier.weight(1f)) { onInfo(HomeInfo.MOOD) }
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
    onClick: () -> Unit,
) {
    val low = value < 30
    WhiteCard(modifier = modifier, color = cardColor, shape = RoundedCornerShape(16.dp), onClick = onClick) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(icon), null, Modifier.size(20.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = FinniColors.NavyMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp))
                Spacer(Modifier.weight(1f))
                Text(if (low) "$value · мало" else "$value", style = MaterialTheme.typography.labelLarge, color = if (low) FinniColors.Warning else FinniColors.Navy)
            }
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(7.dp)
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
        }
    }
}

@Composable
private fun WeekLine(state: HomeUiState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(12.dp), color = FinniColors.Card) {
            Text(
                "Неделя ${state.week}",
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

/**
 * Место питомца. Сама 3D-модель рисуется не здесь, а в PetHost поверх графа навигации:
 * экран только резервирует под неё слот и сообщает хосту, какую модель показать.
 * Если модели для вида нет, рисуется кружок-заглушка.
 */
@Composable
private fun PetArea(state: HomeUiState, petHost: PetHostState?, modifier: Modifier = Modifier) {
    val pet = state.pet
    val asset = pet.look.modelAsset(pet.growthStage)
    val spec = asset?.let {
        PetSpec(
            assetName = it,
            tintArgb = pet.look.color.argb,
            animationsEnabled = state.animationsEnabled,
        )
    }
    SideEffect { petHost?.spec = spec }

    if (spec != null) {
        // Квадрат по высоте свободного места: 3D-питомец рисуется в квадратный буфер (см. PetModel3D)
        Box(
            modifier = modifier
                .fillMaxHeight()
                .aspectRatio(1f, matchHeightConstraintsFirst = true)
                .onGloballyPositioned { petHost?.slot = it }
        )
    } else {
        Box(
            modifier = modifier
                .padding(bottom = 16.dp)
                .size(180.dp)
                .clip(CircleShape)
                .background(Color(pet.look.color.argb)),
            contentAlignment = Alignment.Center
        ) {
            Text(pet.name, style = MaterialTheme.typography.headlineSmall, color = Color.White)
        }
    }
}

/** Белое облачко с лампочкой и хвостиком снизу слева. */
@Composable
private fun TipBubble(text: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.widthIn(max = 150.dp)) {
        WhiteCard(shape = RoundedCornerShape(18.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Image(painterResource(R.drawable.ic_bulb), null, Modifier.size(20.dp))
                Text(
                    text,
                    style = MaterialTheme.typography.labelMedium,
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
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(state.pet.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    state.moodExplanation.text(),
                    style = MaterialTheme.typography.labelMedium,
                    color = FinniColors.NavyMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(shape = CircleShape, color = FinniColors.BlueLight, modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.ic_pencil), contentDescription = "Переименовать", Modifier.size(18.dp))
                }
            }
        }
    }
}

// ---------- Задание ----------

@Composable
private fun TaskCard(task: HomeUiState.ActiveTask, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(R.drawable.ic_target), null, Modifier.size(26.dp))
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text("Задание", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                Text(task.title, style = MaterialTheme.typography.labelLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("+${task.reward}", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Image(painterResource(R.drawable.ic_coin), null, Modifier.padding(start = 4.dp).size(20.dp))
            Image(painterResource(R.drawable.ic_chevron), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(Color.White))
        }
    }
}

@Composable
private fun FinishWeekButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .height(44.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = FinniColors.Green, contentColor = Color.White)
    ) { Text("Завершить неделю", fontSize = 16.sp) }
}

// ---------- Нижняя панель ----------

private data class MenuItem(val section: HomeSection, @DrawableRes val icon: Int, val label: String)

private val menuItems = listOf(
    MenuItem(HomeSection.TASKS, R.drawable.ic_target, "Задания"),
    MenuItem(HomeSection.SHOP, R.drawable.ic_cart, "Магазин"),
    MenuItem(HomeSection.SAVINGS, R.drawable.ic_pig, "Копилка"),
    MenuItem(HomeSection.PROGRESS, R.drawable.ic_star, "Прогресс"),
)

@Composable
private fun BottomMenu(selected: HomeSection?, onOpen: (HomeSection) -> Unit) {
    WhiteCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            menuItems.forEach { item ->
                val isSelected = item.section == selected
                Surface(
                    onClick = { onOpen(item.section) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) FinniColors.BlueLight else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Image(painterResource(item.icon), null, Modifier.size(26.dp))
                        Spacer(Modifier.height(2.dp))
                        Text(
                            item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) FinniColors.Blue else FinniColors.Navy,
                            maxLines = 1
                        )
                        Box(
                            Modifier
                                .padding(top = 2.dp)
                                .size(width = 22.dp, height = 4.dp)
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
    val state = HomeUiState(
        pet = Pet.newborn("Финни", PetLook(PetSpecies.CAT, PetColor.MINT)),   // без 3D-модели, чтобы превью рисовалось
        moodExplanation = HomeUiState.MoodExplanation.Waiting,
        balance = 100,
        savings = 15,
        goal = HomeUiState.Goal(name = "Поход в парк", cost = 60),
        week = 1,
        activeTask = HomeUiState.ActiveTask(title = "Раздели 60 монет", reward = 20),
        tip = HomeUiState.Tip.ChooseGoal,
        suggestedSection = HomeSection.TASKS,
    )
    FinniAppTheme {
        // В превью нет контента: показываем сами ключи вместо текстов
        CompositionLocalProvider(LocalFeedback provides Feedback(FeedbackKey.entries.associateWith { it.id })) {
            HomeScreenContent(state = state, onAction = {})
        }
    }
}
