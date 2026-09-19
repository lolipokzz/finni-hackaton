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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
 * Главный экран: комната, крупный питомец и компактные игровые значки ресурсов.
 *
 * Правила UX (docs/07-screens.md): тапаемые элементы ≥ 48 dp, текст ≥ 16 sp,
 * каждый показатель — иконка + число + описание для TalkBack; подробности по нажатию.
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

            Spacer(Modifier.height(6.dp))
            FinishWeekButton { onAction(HomeAction.FinishWeek) }
            Spacer(Modifier.height(6.dp))
            BottomMenu(petName = state.pet.name, selected = state.suggestedSection, onOpen = { onAction(HomeAction.OpenSection(it)) })
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
            .size(48.dp)
            .shadow(6.dp, CircleShape, ambientColor = FinniColors.Navy.copy(alpha = 0.12f), spotColor = FinniColors.Navy.copy(alpha = 0.12f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(painterResource(icon), contentDescription = contentDescription, modifier = Modifier.size(22.dp))
        }
    }
}

// ---------- Игровые показатели: заполнение внутри картинки, число поверх ----------

@Composable
private fun TopResourcesRow(state: HomeUiState, onAction: (HomeAction) -> Unit, onInfo: (HomeInfo) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundIconButton(R.drawable.ic_gear, "Настройки") {
            onAction(HomeAction.OpenSection(HomeSection.SETTINGS))
        }
        StatusIcon(
            icon = R.drawable.ic_apple,
            label = "Сытость",
            value = state.pet.satiety.value,
            modifier = Modifier.weight(1f),
            onClick = { onInfo(HomeInfo.SATIETY) }
        )
        StatusIcon(
            icon = R.drawable.ic_smile,
            label = "Настроение",
            value = state.pet.mood.value,
            modifier = Modifier.weight(1f),
            onClick = { onInfo(HomeInfo.MOOD) }
        )
        StatusIcon(
            icon = R.drawable.ic_coin,
            label = "Монеты",
            value = state.balance,
            isMeter = false,
            modifier = Modifier.weight(1f),
            onClick = { onInfo(HomeInfo.COINS) }
        )
        RoundIconButton(R.drawable.ic_lock, "Для взрослых") {
            onAction(HomeAction.OpenSection(HomeSection.ADULT))
        }
    }
}

/** Цвет заполняет сам значок снизу вверх; точное значение остаётся читаемым на плашке. */
@Composable
private fun StatusIcon(
    @DrawableRes icon: Int,
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    isMeter: Boolean = true,
    onClick: () -> Unit,
) {
    val amount = if (isMeter) value.coerceIn(0, 100) else value
    val low = isMeter && amount < 30
    // Границы рисунка внутри viewport 48 × 48: прозрачные поля не входят в шкалу.
    val iconTop = if (icon == R.drawable.ic_apple) 7f else 4f
    val iconBottom = if (icon == R.drawable.ic_apple) 41f else 44f
    val emptyIcon = when (icon) {
        R.drawable.ic_apple -> R.drawable.ic_apple_empty
        R.drawable.ic_smile -> R.drawable.ic_smile_empty
        else -> icon
    }
    val description = if (isMeter) "$label: $amount из 100" else "$label: $amount"
    Surface(
        onClick = onClick,
        modifier = modifier.padding(top = 12.dp).semantics {
            contentDescription = if (low) "$description, мало" else description
        },
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier.padding(vertical = 4.dp).height(76.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            // Контуры и детали видны даже при нулевом значении.
            Image(
                painter = painterResource(if (isMeter) emptyIcon else icon),
                contentDescription = null,
                modifier = Modifier.size(60.dp),
            )
            if (isMeter) {
                Image(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(60.dp).drawWithContent {
                        val fillTop = (iconBottom - (iconBottom - iconTop) * amount / 100f) / 48f
                        clipRect(top = size.height * fillTop) {
                            this@drawWithContent.drawContent()
                        }
                    },
                )
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter),
                shape = RoundedCornerShape(12.dp),
                color = if (low) FinniColors.Warning else FinniColors.Card,
                shadowElevation = 2.dp,
            ) {
                Text(
                    text = if (isMeter) "$amount%" else "$amount",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 16.sp,
                    color = if (low) Color.White else FinniColors.Navy,
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
            cameraDistance = 3.1f,
            animationsEnabled = state.animationsEnabled,
        )
    }
    SideEffect { petHost?.spec = spec }

    if (spec != null) {
        Box(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight()
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
private fun BottomMenu(petName: String, selected: HomeSection?, onOpen: (HomeSection) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        menuItems.forEach { item ->
            val isSelected = item.section == selected
            Surface(
                onClick = { onOpen(item.section) },
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent,
                modifier = Modifier.weight(1f).height(88.dp).semantics {
                    if (item.section == HomeSection.PROGRESS) {
                        contentDescription = "$petName, открыть прогресс питомца"
                    }
                }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Image(painterResource(item.icon), null, Modifier.size(42.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (item.section == HomeSection.PROGRESS) petName else item.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) FinniColors.Blue else FinniColors.Navy,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
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
