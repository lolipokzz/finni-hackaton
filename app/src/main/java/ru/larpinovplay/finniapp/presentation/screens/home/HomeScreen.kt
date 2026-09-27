package ru.larpinovplay.finniapp.presentation.screens.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.content.Feedback
import ru.larpinovplay.finniapp.domain.content.FeedbackKey
import ru.larpinovplay.finniapp.domain.game.model.Deed
import ru.larpinovplay.finniapp.domain.game.model.FinishBlock
import ru.larpinovplay.finniapp.domain.game.model.WeekDeeds
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetLook
import ru.larpinovplay.finniapp.domain.pet.model.PetSpecies
import ru.larpinovplay.finniapp.presentation.components.creamCard
import ru.larpinovplay.finniapp.presentation.components.BubbleTail
import ru.larpinovplay.finniapp.presentation.components.CoinPill
import ru.larpinovplay.finniapp.presentation.components.MeterRing
import ru.larpinovplay.finniapp.presentation.components.OnRoomLabel
import ru.larpinovplay.finniapp.presentation.components.PebbleButton
import ru.larpinovplay.finniapp.presentation.components.PetHostOwner
import ru.larpinovplay.finniapp.presentation.components.PetHostState
import ru.larpinovplay.finniapp.presentation.components.PetSpec
import ru.larpinovplay.finniapp.presentation.components.PillButton
import ru.larpinovplay.finniapp.presentation.components.RoomAnchor
import ru.larpinovplay.finniapp.presentation.components.RoomBackground
import ru.larpinovplay.finniapp.presentation.components.Sticker
import ru.larpinovplay.finniapp.presentation.components.rememberRoomAnchor
import ru.larpinovplay.finniapp.presentation.components.roomOrigin
import ru.larpinovplay.finniapp.presentation.components.roomPetSlot
import ru.larpinovplay.finniapp.presentation.feedback.LocalFeedback
import ru.larpinovplay.finniapp.presentation.pet.accessoryNodes
import ru.larpinovplay.finniapp.presentation.pet.hitAnimations
import ru.larpinovplay.finniapp.presentation.pet.idleAnimation
import ru.larpinovplay.finniapp.presentation.pet.modelAsset
import ru.larpinovplay.finniapp.presentation.pet.pettingAnimation
import ru.larpinovplay.finniapp.presentation.pet.tapAnimation
import ru.larpinovplay.finniapp.presentation.theme.FinniAppTheme
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * Главный экран: комната с питомцем и немного «наклеек» вокруг (PRODUCT.md, макет v6).
 * Сверху — самочувствие Финни, монеты и его табличка с шагами роста; посередине Финни и одна его реплика
 * с кнопкой следующего дела; снизу меню и солнышко недели, по которому открываются дела и конец недели.
 *
 * Правила UX (docs/07-screens.md): тапаемые элементы ≥ 48 dp, у каждого показателя есть значок и описание
 * для TalkBack; подробности по нажатию.
 *
 * Питомец рисуется в SurfaceView поверх Compose с прозрачным фоном, поэтому наклейки не должны заходить
 * на его слот: реплика стоит над слотом, а не на нём.
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
    val room = rememberRoomAnchor()
    Box(modifier = modifier.fillMaxSize().roomOrigin(room)) {
        RoomBackground(anchor = room)
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
            Spacer(Modifier.height(10.dp))
            TopRow(state, onAction)
            Spacer(Modifier.height(10.dp))
            // Слева под кольцами — редкая кнопка «всё о Финни», справа от неё реплика: одна строка на двоих
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.width(62.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PetButton(state.pet, onClick = { onAction(HomeAction.OpenSection(HomeSection.PROGRESS)) })
                    if (state.demoMode) DemoChip()
                }
                state.speech?.let { speech ->
                    SpeechBubble(speech, onAction, Modifier.padding(start = 14.dp).weight(1f))
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                PetArea(state, petHost, room, modifier = Modifier.align(Alignment.BottomCenter))
            }
            BottomMenu(state, onAction)
            Spacer(Modifier.height(8.dp))
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
    if (state.deedsOpen) DeedsCard(state, onAction)
    // Сначала итоги прошлой недели, потом план новой
    val summary = state.weekSummary
    val draft = state.planDraft
    when {
        summary != null -> WeekSummaryDialog(summary, onDismiss = { onAction(HomeAction.DismissWeekSummary) })
        draft != null -> WeekPlanDialog(
            draft = draft,
            onChange = { direction, increase -> onAction(HomeAction.ChangePlan(direction, increase)) },
            onConfirm = { onAction(HomeAction.ConfirmPlan) },
        )
    }
}

// ---------- Верх: самочувствие, монеты, настройки ----------

@Composable
private fun TopRow(state: HomeUiState, onAction: (HomeAction) -> Unit) {
    val pet = state.pet
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MeterRing(
            value = pet.satiety.value,
            color = FinniColors.SatietyRing,
            track = FinniColors.SatietyTrack,
            icon = R.drawable.ic_meter_apple,
            description = "Сытость: ${pet.satiety.value} из 100" + if (pet.isHungry) ", Финни голоден" else "",
            onClick = { onAction(HomeAction.ShowInfo(HomeInfo.SATIETY)) },
        )
        Spacer(Modifier.width(10.dp))
        MeterRing(
            value = pet.mood.value,
            color = FinniColors.MoodRing,
            track = FinniColors.MoodTrack,
            icon = R.drawable.ic_meter_smile,
            description = "Настроение: ${pet.mood.value} из 100",
            onClick = { onAction(HomeAction.ShowInfo(HomeInfo.MOOD)) },
        )
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            CoinPill(state.balance, onClick = { onAction(HomeAction.ShowInfo(HomeInfo.COINS)) })
        }
        PebbleButton(
            icon = R.drawable.ic_gear_line,
            description = "Настройки",
            color = FinniColors.Cream.copy(alpha = 0.85f),
            onClick = { onAction(HomeAction.OpenSection(HomeSection.SETTINGS)) },
        )
    }
}

/** Всё о Финни: рост и прогресс. Нажимают редко, поэтому это маленькая круглая наклейка с лапкой. */
@Composable
private fun PetButton(pet: Pet, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = Modifier
            .size(48.dp)
            .creamCard(CircleShape, elevation = 8.dp, border = 3.dp)
            .clearAndSetSemantics { contentDescription = "${pet.name}: рост и прогресс" },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.ic_paw), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(FinniColors.TealBright))
        }
    }
}

@Composable
private fun DemoChip() {
    Surface(shape = CircleShape, color = FinniColors.Sunny, border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)) {
        Text("Демо", fontSize = 13.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

// ---------- Реплика Финни ----------

/** Облачко над Финни: он говорит от себя и зовёт к одному делу. Хвостик смотрит вниз, на питомца. */
@Composable
private fun SpeechBubble(speech: HomeUiState.Speech, onAction: (HomeAction) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .creamCard(RoundedCornerShape(24.dp), elevation = 8.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(speech.text(), fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.ExtraBold, color = FinniColors.Ink)
            speech.button?.let { (label, action) ->
                PillButton(label, onClick = { onAction(action) }, modifier = Modifier.fillMaxWidth(), arrow = true)
            }
        }
        BubbleTail(Modifier.padding(start = 22.dp).offset(y = (-4).dp))
    }
}

// ---------- Питомец ----------

/**
 * Место питомца. Сама 3D-модель рисуется не здесь, а в PetHost поверх графа навигации:
 * экран только резервирует под неё слот и сообщает хосту, какую модель показать.
 * Если модели для вида нет, рисуется кружок-заглушка.
 */
@Composable
private fun PetArea(state: HomeUiState, petHost: PetHostState?, room: RoomAnchor, modifier: Modifier = Modifier) {
    val pet = state.pet
    val asset = pet.look.modelAsset(pet.growthStage)
    // Повторять слова можно, только если это разрешено взрослым, звук включён и есть доступ к микрофону.
    // Спрашиваем доступ лишь при живом питомце: в превью нет механизма разрешений
    val wantsVoice = state.voiceRepeatEnabled && state.soundEnabled
    val micGranted = petHost != null && asset != null && rememberMicrophone(ask = wantsVoice)
    val spec = asset?.let {
        PetSpec(
            assetName = it,
            tintArgb = pet.look.color.argb,
            animationsEnabled = state.animationsEnabled,
            soundEnabled = state.soundEnabled,
            voiceEnabled = wantsVoice && micGranted,
            idleAnimation = pet.idleAnimation,
            tapAnimation = pet.look.tapAnimation,
            hitAnimations = pet.look.hitAnimations,
            pettingAnimation = pet.look.pettingAnimation,
            accessories = pet.accessoryNodes,
        )
    }
    SideEffect { petHost?.spec = spec }

    if (spec != null) {
        // Квадрат по высоте свободного места: 3D-питомец рисуется в квадратный буфер (см. PetModel3D)
        Box(
            modifier = modifier
                .fillMaxHeight()
                .aspectRatio(1f, matchHeightConstraintsFirst = true)
                .onGloballyPositioned { petHost?.setSlot(PetHostOwner.HOME, it) }
                .roomPetSlot(room)
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

/**
 * Есть ли доступ к микрофону. Если [ask] и доступа нет — один раз за показ экрана спрашивает у системы
 * (после двух отказов Android сам перестаёт показывать запрос, и питомец просто не повторяет слова).
 */
@Composable
private fun rememberMicrophone(ask: Boolean): Boolean {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(ask) {
        if (ask && !granted) request.launch(Manifest.permission.RECORD_AUDIO)
    }
    return granted
}

// ---------- Меню и солнышко недели ----------

private val MenuHeight = 124.dp

@Composable
private fun BottomMenu(state: HomeUiState, onAction: (HomeAction) -> Unit) {
    val open = { section: HomeSection -> onAction(HomeAction.OpenSection(section)) }
    Row(Modifier.fillMaxWidth().height(MenuHeight), verticalAlignment = Alignment.Bottom) {
        MenuItem("Задания", Modifier.weight(1f), badge = state.tasksBadge, onClick = { open(HomeSection.TASKS) }) {
            Sticker(R.drawable.ic_nav_tasks, Color(0xFFE6F8F2))
        }
        MenuItem("Магазин", Modifier.weight(1f), onClick = { open(HomeSection.SHOP) }) {
            Sticker(R.drawable.ic_nav_shop, Color(0xFFFFF0E6))
        }
        WeekSun(state, Modifier.weight(1f)) { onAction(HomeAction.ShowDeeds) }
        MenuItem("Гардероб", Modifier.weight(1f), onClick = { open(HomeSection.WARDROBE) }) {
            Sticker(R.drawable.ic_nav_wardrobe, Color(0xFFE4F3FF))
        }
        val goal = state.goal
        MenuItem(
            "Копилка",
            Modifier.weight(1f),
            description = goal?.let { "Копилка. Мечта: ${it.name}, ${state.savings} из ${it.cost}" },
            onClick = { open(HomeSection.SAVINGS) },
        ) {
            if (goal == null) {
                Sticker(R.drawable.ic_nav_savings, Color(0xFFFFE6F0))
            } else {
                // Кольцо мечты: сколько уже накоплено на цель
                MeterRing(
                    value = (state.savings * 100 / goal.cost.coerceAtLeast(1)),
                    color = FinniColors.DreamRing,
                    track = FinniColors.DreamTrack,
                    icon = R.drawable.ic_nav_savings,
                    description = "",
                    size = 64.dp,
                ) {
                    Box(Modifier.size(46.dp).clip(CircleShape).background(Color(0xFFFFE6F0)), contentAlignment = Alignment.Center) {
                        Image(painterResource(R.drawable.ic_nav_savings), null, Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(
    label: String,
    modifier: Modifier = Modifier,
    badge: Int = 0,
    description: String? = null,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    // Без обрезки по форме: значок новых заданий выходит за край наклейки
    Box(
        modifier = modifier
            .clickable(
                interactionSource = null,
                indication = ripple(bounded = false, radius = 40.dp),
                role = Role.Button,
                onClick = onClick,
            )
            .clearAndSetSemantics {
                contentDescription = description ?: if (badge > 0) "$label, новых: $badge" else label
                role = Role.Button
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.padding(top = 8.dp)) {
                icon()
                if (badge > 0) {
                    Surface(
                        shape = CircleShape,
                        color = FinniColors.ActionPeach,
                        border = androidx.compose.foundation.BorderStroke(3.dp, Color.White),
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-8).dp).wrapContentWidth(unbounded = true),
                    ) {
                        Row(Modifier.padding(horizontal = 7.dp, vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(R.drawable.ic_star_small), null, Modifier.size(11.dp))
                            Text("$badge", fontSize = 12.sp, fontWeight = FontWeight.Black, color = FinniColors.ActionPeachInk, modifier = Modifier.padding(start = 2.dp))
                        }
                    }
                }
            }
            Text(label, style = OnRoomLabel, maxLines = 1, softWrap = false, modifier = Modifier.wrapContentWidth(unbounded = true))
        }
    }
}

/**
 * Солнышко недели в центре меню: четыре дуги — четыре дела недели, число сделанных и номер недели.
 * Когда неделю можно закончить, вместо номера — «Готово!». Нажатие открывает дела недели.
 */
@Composable
private fun WeekSun(state: HomeUiState, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val deeds = state.deeds
    val ready = state.finishBlock == null
    // Солнышко шире своей ячейки и чуть заходит на соседей, поэтому рисуется поверх них
    Box(modifier.height(MenuHeight).zIndex(1f), contentAlignment = Alignment.TopCenter) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .requiredSize(88.dp)
                .semantics {
                    contentDescription = "Неделя ${state.week}: сделано ${deeds.steps} из ${WeekDeeds.MAX_STEPS} дел" +
                        if (ready) ". Неделю можно завершить" else ". Открыть дела недели"
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = size.minDimension * 0.067f
                    val inset = size.minDimension * 0.067f
                    Deed.entries.forEachIndexed { i, deed ->
                        drawArc(
                            color = if (deeds[deed]) FinniColors.TealBright else Color(0xFFFFD9B8),
                            startAngle = -84f + i * 90f,
                            sweepAngle = 74f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - 2 * inset, size.height - 2 * inset),
                            style = Stroke(stroke, cap = StrokeCap.Round),
                        )
                    }
                }
                Image(painterResource(R.drawable.ic_week_sun), null, Modifier.fillMaxSize())
            }
        }
        // Число сделанных дел — наклейка на краю солнышка
        Surface(
            shape = CircleShape,
            color = FinniColors.Teal,
            border = androidx.compose.foundation.BorderStroke(3.dp, Color.White),
            modifier = Modifier.align(Alignment.TopCenter).offset(x = 34.dp, y = (-4).dp).wrapContentWidth(unbounded = true),
        ) {
            Text(
                "${deeds.steps}/${WeekDeeds.MAX_STEPS}",
                fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Surface(
            shape = CircleShape,
            color = FinniColors.Teal,
            border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
            shadowElevation = 3.dp,
            modifier = Modifier.padding(top = 74.dp).wrapContentWidth(unbounded = true),
        ) {
            Text(
                if (ready) "Готово!" else "Неделя ${state.week}",
                fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1, softWrap = false,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
            )
        }
    }
}

// ---------- Превью ----------

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeScreenPreview() {
    val state = HomeUiState(
        pet = Pet.newborn("Финни", PetLook(PetSpecies.CAT, PetColor.MINT)).grow(3),   // без 3D-модели, чтобы превью рисовалось
        balance = 45,
        savings = 20,
        goal = HomeUiState.Goal(name = "Домик", cost = 60),
        week = 3,
        speech = HomeUiState.Speech.HUNGRY,
        tasksBadge = 1,
        finishBlock = FinishBlock.SAME_DAY,
        deeds = WeekDeeds(fed = false, notBored = true, savingsOnPlan = true, spendingOnPlan = true),
    )
    FinniAppTheme {
        // В превью нет контента: показываем сами ключи вместо текстов
        CompositionLocalProvider(LocalFeedback provides Feedback(FeedbackKey.entries.associateWith { it.id })) {
            HomeScreenContent(state = state, onAction = {})
        }
    }
}
