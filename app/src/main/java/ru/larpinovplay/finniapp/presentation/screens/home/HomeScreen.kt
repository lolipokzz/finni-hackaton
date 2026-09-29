package ru.larpinovplay.finniapp.presentation.screens.home

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import ru.larpinovplay.finniapp.presentation.components.TealButton
import ru.larpinovplay.finniapp.presentation.components.CardSticker
import ru.larpinovplay.finniapp.presentation.components.CardTitle
import ru.larpinovplay.finniapp.presentation.components.CardDialog
import ru.larpinovplay.finniapp.presentation.components.spotlightTarget
import ru.larpinovplay.finniapp.presentation.components.TutorialSpotlight
import ru.larpinovplay.finniapp.presentation.components.SpotlightTargets
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import ru.larpinovplay.finniapp.presentation.components.petShield
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
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.content.Feedback
import ru.larpinovplay.finniapp.domain.content.FeedbackKey
import ru.larpinovplay.finniapp.domain.game.model.Deed
import ru.larpinovplay.finniapp.domain.game.model.FinishBlock
import ru.larpinovplay.finniapp.domain.game.model.TutorialStep
import ru.larpinovplay.finniapp.domain.game.model.WeekDeeds
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetLook
import ru.larpinovplay.finniapp.presentation.components.BubbleTail
import ru.larpinovplay.finniapp.presentation.components.CoinButton
import ru.larpinovplay.finniapp.presentation.components.MeterButton
import ru.larpinovplay.finniapp.presentation.components.MeterRing
import ru.larpinovplay.finniapp.presentation.components.OnRoomLabel
import ru.larpinovplay.finniapp.presentation.components.PebbleButton
import ru.larpinovplay.finniapp.presentation.components.PetHostOwner
import ru.larpinovplay.finniapp.presentation.components.PetHostState
import ru.larpinovplay.finniapp.presentation.components.PetSpec
import ru.larpinovplay.finniapp.presentation.components.RoomAnchor
import ru.larpinovplay.finniapp.presentation.components.RoomBackground
import ru.larpinovplay.finniapp.presentation.components.Sticker
import ru.larpinovplay.finniapp.presentation.components.creamCard
import ru.larpinovplay.finniapp.presentation.components.rememberRoomAnchor
import ru.larpinovplay.finniapp.presentation.components.roomOrigin
import ru.larpinovplay.finniapp.presentation.components.roomPetSlot
import ru.larpinovplay.finniapp.presentation.feedback.LocalFeedback
import ru.larpinovplay.finniapp.presentation.pet.PetHits
import ru.larpinovplay.finniapp.presentation.pet.PetPettingAnimation
import ru.larpinovplay.finniapp.presentation.pet.PetTapAnimation
import ru.larpinovplay.finniapp.presentation.pet.accessoryNodes
import ru.larpinovplay.finniapp.presentation.pet.idleAnimation
import ru.larpinovplay.finniapp.presentation.pet.modelAsset
import ru.larpinovplay.finniapp.presentation.pet.modelScale
import ru.larpinovplay.finniapp.presentation.pet.skinAsset
import ru.larpinovplay.finniapp.presentation.theme.FinniAppTheme
import ru.larpinovplay.finniapp.presentation.theme.FinniColors
import androidx.compose.foundation.BorderStroke

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
    // Обучение: где на экране то, на что Финни просит нажать
    val targets = remember { SpotlightTargets() }
    val coachStep = state.tutorial
    // Облачко открыто при входе на экран и при каждом новом деле; потом само садится в значок над Финни
    var speechOpen by remember(state.speech) { mutableStateOf(true) }
    if (petHost != null) {
        DisposableEffect(petHost) {
            petHost.onTap = { speechOpen = true }
            onDispose { petHost.onTap = {} }
        }
    }
    Box(modifier = modifier.fillMaxSize().roomOrigin(room)) {
        RoomBackground(anchor = room)
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
            Spacer(Modifier.height(10.dp))
            TopRow(state, onAction)
            Spacer(Modifier.height(10.dp))
            StatusRow(state, onAction)
            Spacer(Modifier.height(10.dp))
            // Слева под кольцами — редкая кнопка «всё о Финни» и кнопка плана; реплика Финни — над его головой
            Column(Modifier.width(62.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PetButton(state.pet, onClick = { onAction(HomeAction.OpenSection(HomeSection.PROGRESS)) })
                // Кнопка плана: до начала недели — составить план (окно открывается только по ней), потом — как он идёт
                if (state.planDraft != null || state.activePlan != null) {
                    PlanButton(Modifier.spotlightTarget(targets, TutorialStep.PLAN)) { onAction(HomeAction.OpenPlan) }
                }
                if (state.demoMode) DemoChip()
            }
            val speech = state.speech.takeIf { coachStep == null }
            // Облачко висит [SPEECH_SHOWN_MS] и садится в «…» у головы
            LaunchedEffect(speechOpen, speech) {
                if (speechOpen && speech != null) {
                    delay(SPEECH_SHOWN_MS)
                    speechOpen = false
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                PetArea(
                    state, petHost, room,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    // Реплика — облачко точно над головой, хвостиком к ней
                    speech = speech?.takeIf { speechOpen }?.let { shown ->
                        { SpeechBubble(shown, petHost, onClose = { speechOpen = false }, onAction = onAction) }
                    },
                    // Финни молчит, но ему есть что сказать: маленькое облачко «…» у головы
                    hint = state.speech?.takeIf { !speechOpen && coachStep == null }?.let { speech ->
                        {
                            val open = { speechOpen = true }
                            // Облачко «…» закрывает собой кусочек питомца: нажатие на него открывает реплику без его анимации
                            TypingBubble(important = speech.important, onClick = open, modifier = Modifier.petShield(petHost, open))
                        }
                    },
                )
            }
            BottomMenu(state, onAction, target = { step -> Modifier.spotlightTarget(targets, step) })
            Spacer(Modifier.height(8.dp))
        }
        // Окна важнее подсказки: пока открыто окно, подсветки нет
        val dialogOpen = state.info != null || state.deedsOpen || state.weekSummary != null || state.planOpen || state.tutorialDone
        if (coachStep != null && !dialogOpen) {
            TutorialSpotlight(
                coachStep.coachText, targets[coachStep],
                onSkip = { onAction(HomeAction.SkipTutorialStep) }.takeIf { coachStep != TutorialStep.PLAN },
                round = true,
                petHost = petHost,
            )
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
    if (state.tutorialDone) TutorialDoneDialog(onDone = { onAction(HomeAction.FinishTutorial) })
    // Сначала итоги прошлой недели, потом план новой
    val summary = state.weekSummary
    val draft = state.planDraft
    when {
        summary != null -> WeekSummaryDialog(summary, onDismiss = { onAction(HomeAction.DismissWeekSummary) })
        draft != null && state.planOpen -> WeekPlanDialog(
            draft = draft,
            coach = state.tutorial == TutorialStep.PLAN,
            onChange = { direction, increase -> onAction(HomeAction.ChangePlan(direction, increase)) },
            onSet = { direction, amount -> onAction(HomeAction.SetPlan(direction, amount)) },
            onConfirm = { onAction(HomeAction.ConfirmPlan) },
            onDismiss = { onAction(HomeAction.ClosePlan) },
        )
        state.planOpen -> state.activePlan?.let { ActivePlanDialog(it, onDismiss = { onAction(HomeAction.ClosePlan) }) }
    }
}

// ---------- Обучение первой недели ----------

/** Что Финни просит сделать на шаге обучения, пока ребёнок на главном экране. */
private val TutorialStep.coachText: String
    get() = when (this) {
        TutorialStep.PLAN -> "Каждую неделю мне дают монеты. Сначала решим, на что их потратить. Нажми на «План»!"
        TutorialStep.GOAL -> "Давай выберем цель, на которую будем копить! Нажми на копилку."
        TutorialStep.SHOP -> "Мур, я проголодался! Пойдём в магазин — купим мне еды."
        TutorialStep.TASKS -> "Монеты можно заработать! Нажми на «Задания» — там задачки про деньги."
        TutorialStep.DEEDS -> "Это солнышко недели. Нажми — покажу дела, от которых я расту!"
    }

/**
 * Конец обучения: коротко всё, что узнали, — теми же наклейками, что в меню, — и вопрос «Всё понятно?».
 * Закрыть можно только ответом: так ясно, что обучение закончилось и дальше ребёнок играет сам.
 */
@Composable
private fun TutorialDoneDialog(onDone: () -> Unit) {
    CardDialog {
        CardTitle("Обучение пройдено!", "Мур! Спасибо, что помогаешь мне с монетами")
        listOf(
            Triple(R.drawable.ic_week_plan, Color(0xFFE6EEFF), "План — в начале недели раздели монеты"),
            Triple(R.drawable.ic_nav_savings, Color(0xFFFFE6F0), "Копилка — копим на цель"),
            Triple(R.drawable.ic_nav_shop, Color(0xFFFFF0E6), "Магазин — еда и радости для меня"),
            Triple(R.drawable.ic_nav_tasks, Color(0xFFE6F8F2), "Задания — решай и зарабатывай монеты"),
            Triple(R.drawable.ic_week_sun, Color(0xFFFFF5C9), "Солнышко — дела недели, от них я расту"),
        ).forEach { (icon, tint, text) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CardSticker(icon, tint, size = 44.dp)
                Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.ExtraBold, color = FinniColors.Ink)
            }
        }
        Text(
            "Всё понятно?",
            fontSize = 20.sp, fontWeight = FontWeight.Black, color = FinniColors.Teal, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
        TealButton("Да, всё понятно!", R.drawable.ic_check, onDone)
    }
}

// ---------- Верх: самочувствие, монеты, настройки ----------

@Composable
private fun TopRow(state: HomeUiState, onAction: (HomeAction) -> Unit) {
    val pet = state.pet
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MeterButton(
            value = pet.satiety.value,
            color = FinniColors.SatietyRing,
            track = FinniColors.SatietyTrack,
            icon = R.drawable.ic_meter_apple,
            description = "Сытость: ${pet.satiety.value} из 100" + if (pet.isHungry) ", Финни голоден" else "",
            onClick = { onAction(HomeAction.ShowInfo(HomeInfo.SATIETY)) },
        )
        Spacer(Modifier.width(10.dp))
        MeterButton(
            value = pet.mood.value,
            color = FinniColors.MoodRing,
            track = FinniColors.MoodTrack,
            icon = R.drawable.ic_meter_smile,
            description = "Настроение: ${pet.mood.value} из 100",
            onClick = { onAction(HomeAction.ShowInfo(HomeInfo.MOOD)) },
        )
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            CoinButton(state.balance, onClick = { onAction(HomeAction.ShowInfo(HomeInfo.COINS)) })
        }
        PebbleButton(
            icon = R.drawable.ic_gear_line,
            description = "Настройки",
            color = FinniColors.Cream.copy(alpha = 0.85f),
            onClick = { onAction(HomeAction.OpenSection(HomeSection.SETTINGS)) },
        )
    }
}

/**
 * Копилка и активное задание числом и словами (ТЗ 2.5.3): видно сразу, без перехода в разделы.
 * Каждая карточка ведёт туда, где с этим работают.
 */
@Composable
private fun StatusRow(state: HomeUiState, onAction: (HomeAction) -> Unit) {
    val goal = state.goal
    val task = state.activeTask
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatusCard(
            icon = R.drawable.ic_nav_savings,
            tint = Color(0xFFFFE6F0),
            title = "В копилке ${state.savings}",
            detail = goal?.let { "${it.name}: ${state.savings} из ${it.cost}" } ?: "Цель не выбрана",
            description = "Копилка: ${state.savings} монет. " + (goal?.let { "Цель: ${it.name}, накоплено ${state.savings} из ${it.cost}" } ?: "Цель ещё не выбрана"),
            modifier = Modifier.weight(1f),
            onClick = { onAction(HomeAction.OpenSection(HomeSection.SAVINGS)) },
        )
        StatusCard(
            icon = if (task?.adventure == true) R.drawable.ic_adventure else R.drawable.ic_nav_tasks,
            tint = Color(0xFFE6F8F2),
            title = task?.title ?: "Задания сделаны",
            detail = task?.let { (if (it.adventure) "Приключение" else "Задание") + " · +${it.reward}" } ?: "Новые — на следующей неделе",
            description = task?.let { (if (it.adventure) "Приключение недели: " else "Задание: ") + "${it.title}, награда ${it.reward} монет" }
                ?: "Задания этой недели сделаны",
            modifier = Modifier.weight(1f),
            onClick = { onAction(HomeAction.OpenSection(HomeSection.TASKS)) },
        )
    }
}

@Composable
private fun StatusCard(
    icon: Int,
    tint: Color,
    title: String,
    detail: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    // Кликабельная карточка: Surface(onClick) — сам даёт отклик и нажатие, роль задаём явно (Surface её не ставит)
    val shape = RoundedCornerShape(22.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = Color.Transparent,
        modifier = modifier
            .heightIn(min = 56.dp)
            .creamCard(shape, elevation = 6.dp, border = 3.dp)
            .clearAndSetSemantics { contentDescription = description; role = Role.Button },
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
                Image(painterResource(icon), null, Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(detail, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.ExtraBold, color = FinniColors.InkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Всё о Финни: рост и прогресс. Нажимают редко, поэтому это маленькая круглая наклейка со столбиками роста. */
@Composable
private fun PetButton(pet: Pet, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp).creamCard(CircleShape, elevation = 8.dp, border = 3.dp)) {
        Image(
            painterResource(R.drawable.ic_progress),
            contentDescription = "${pet.name}: рост и прогресс",
            modifier = Modifier.size(26.dp),
            colorFilter = ColorFilter.tint(FinniColors.TealBright),
        )
    }
}

/** «План недели»: голубая наклейка с календарём, как соседняя кнопка прогресса — без подписи. */
@Composable
private fun PlanButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        // Отступ сверху — чтобы круг подсветки обучения не задевал кнопку питомца
        modifier = Modifier
            .padding(top = 6.dp)
            .then(modifier)
            .size(48.dp)
            .creamCard(CircleShape, elevation = 8.dp, border = 3.dp)
            .background(Color(0xFFE6EEFF)),
    ) {
        Image(painterResource(R.drawable.ic_week_plan), contentDescription = "План недели", modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun DemoChip() {
    Surface(shape = CircleShape, color = FinniColors.Sunny, border = BorderStroke(2.dp, Color.White)) {
        Text("Демо", fontSize = 13.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

// ---------- Реплика Финни ----------

/** Сколько облачко висит само, прежде чем сесть в значок. */
private const val SPEECH_SHOWN_MS = 6_000L

/**
 * Реплика Финни. Сначала — облачко над головой: он говорит от себя и зовёт к одному делу текстовой ссылкой.
 * Через [SPEECH_SHOWN_MS] или по нажатию на облачко оно прячется, а у головы остаётся маленькое «…»
 * ([TypingBubble]); оно или сам Финни открывают реплику снова.
 */
private val HomeUiState.Speech.important: Boolean
    get() = this == HomeUiState.Speech.PLAN_WEEK || this == HomeUiState.Speech.HUNGRY || this == HomeUiState.Speech.ADVENTURE ||
        this == HomeUiState.Speech.WEEK_READY

/** Облачко: короткая фраза и текстовая ссылка на дело. Нажатие мимо ссылки прячет облачко. */
@Composable
private fun SpeechBubble(speech: HomeUiState.Speech, petHost: PetHostState?, onClose: () -> Unit, onAction: (HomeAction) -> Unit) {
    val button = speech.button
    Column(Modifier.widthIn(max = SpeechBubbleMaxWidth), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier
                // Облачко частично под прозрачным видом питомца: нажатия туда пересылает вид (см. petShield)
                .petShield(petHost, onClose)
                .creamCard(RoundedCornerShape(22.dp), elevation = 8.dp)
                .clickable(role = Role.Button, onClickLabel = "Спрятать", onClick = onClose)
                .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = if (button == null) 10.dp else 0.dp),
        ) {
            Text(speech.text(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.ExtraBold, color = FinniColors.Ink)
            button?.let { (label, action) ->
                TextButton(
                    onClick = { onAction(action) },
                    modifier = Modifier.petShield(petHost) { onAction(action) },
                    colors = ButtonDefaults.textButtonColors(contentColor = FinniColors.Teal),
                    contentPadding = PaddingValues(start = 0.dp, end = 6.dp),
                ) {
                    Text(label, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Image(
                        painterResource(R.drawable.ic_arrow_right), null, Modifier.padding(start = 4.dp).size(16.dp),
                        colorFilter = ColorFilter.tint(FinniColors.Teal),
                    )
                }
            }
        }
        // Хвостик по центру, вниз — к голове Финни
        BubbleTail(Modifier.offset(y = (-4).dp), pointsLeft = false)
    }
}

// ---------- Питомец ----------

/**
 * Место питомца. Сама 3D-модель рисуется не здесь, а в PetHost поверх графа навигации:
 * экран только резервирует под неё слот и сообщает хосту, какую модель показать.
 */
@Composable
private fun PetArea(
    state: HomeUiState,
    petHost: PetHostState?,
    room: RoomAnchor,
    modifier: Modifier = Modifier,
    speech: (@Composable () -> Unit)? = null,
    hint: (@Composable () -> Unit)? = null,
) {
    val pet = state.pet
    // Повторять слова можно, только если это включено в настройках, звук включён и доступ к микрофону уже выдан.
    // Сам главный экран доступ никогда не спрашивает: его запрашивает переключатель в настройках при включении
    val wantsVoice = state.voiceRepeatEnabled && state.soundEnabled
    val micGranted = petHost != null && rememberMicrophoneGranted(wantsVoice)
    val spec = PetSpec(
        assetName = pet.growthStage.modelAsset,
        tintArgb = pet.look.color.argb,
        modelScale = pet.growthStage.modelScale,
        skin = pet.look.skinAsset,
        animationsEnabled = state.animationsEnabled,
        soundEnabled = state.soundEnabled,
        voiceEnabled = wantsVoice && micGranted,
        idleAnimation = pet.idleAnimation,
        tapAnimation = PetTapAnimation,
        hitAnimations = PetHits,
        pettingAnimation = PetPettingAnimation,
        accessories = pet.accessoryNodes,
    )
    SideEffect { petHost?.spec = spec }

    // Квадрат по высоте свободного места: 3D-питомец рисуется в квадратный буфер (см. PetModel3D)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .aspectRatio(1f, matchHeightConstraintsFirst = true)
            .onGloballyPositioned { petHost?.setSlot(PetHostOwner.HOME, it) }
            .roomPetSlot(room)
    ) {
        // Облачко справа от головы. Питомец уменьшается от пола, поэтому макушка тем ниже, чем он меньше.
        // Слот накрыт видом питомца: нажатие приходит питомцу, а он и открывает реплику (PetHostState.onTap)
        val side = maxHeight
        val headTop = side * (FLOOR_IN_SLOT - PET_HEIGHT_IN_SLOT * pet.growthStage.modelScale).coerceAtLeast(0f)
        // Реплика: облачко по центру над макушкой, низ облачка — у ушей. Место над слотом свободно, поэтому
        // облачко выше головы может выходить за верх слота
        AnimatedVisibility(
            visible = speech != null,
            enter = if (state.animationsEnabled) fadeIn() + scaleIn(initialScale = 0.6f, transformOrigin = TransformOrigin(0.5f, 1f)) else EnterTransition.None,
            exit = if (state.animationsEnabled) fadeOut() + scaleOut(targetScale = 0.6f, transformOrigin = TransformOrigin(0.5f, 1f)) else ExitTransition.None,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .height(headTop)
                .wrapContentHeight(align = Alignment.Bottom, unbounded = true),
        ) {
            speech?.invoke()
        }
        AnimatedVisibility(
            visible = hint != null,
            enter = if (state.animationsEnabled) fadeIn() + scaleIn(initialScale = 0.5f, transformOrigin = TransformOrigin(0f, 1f)) else EnterTransition.None,
            exit = if (state.animationsEnabled) fadeOut() else ExitTransition.None,
            modifier = Modifier.align(Alignment.TopCenter).offset(x = side * 0.2f, y = headTop),
        ) {
            hint?.invoke()
        }
    }
}

/** Облачко реплики не шире этого: над головой, не заезжая на кнопки слева. */
private val SpeechBubbleMaxWidth = 260.dp

/**
 * Где кончики ушей, в долях стороны слота от его верха: FLOOR_IN_SLOT − PET_HEIGHT_IN_SLOT × масштаб стадии.
 * Питомец уменьшается от пола, а пол — не низ слота. Замерено на экране в спокойной позе на всех трёх стадиях
 * (малыш 0,366, подросток 0,239, взрослый 0,129 — ровно на прямой). По ней садятся облачко реплики и «…».
 */
private const val FLOOR_IN_SLOT = 0.868f
private const val PET_HEIGHT_IN_SLOT = 0.739f

/**
 * Маленькое облачко «• • •» с хвостиком из двух кружков к голове: Финни есть что сказать.
 * У важного дела точки тёплые. Точки не мигают: облачко лежит под прозрачным видом питомца, и постоянная
 * перерисовка под ним затемняет весь слот.
 */
@Composable
private fun TypingBubble(important: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dot = if (important) FinniColors.DeedPending else FinniColors.InkMuted
    Box(
        modifier
            .size(width = 64.dp, height = 44.dp)

            .clickable(role = Role.Button, onClickLabel = "Послушать Финни", onClick = onClick)
            .semantics { contentDescription = if (important) "Финни хочет сказать что-то важное" else "Финни хочет что-то сказать" },
    ) {
        // Хвостик: два кружка вниз-влево, к голове
        Box(Modifier.align(Alignment.BottomStart).offset(x = 2.dp).size(7.dp).creamCard(CircleShape, elevation = 2.dp, border = 1.5.dp))
        Box(Modifier.align(Alignment.BottomStart).offset(x = 9.dp, y = (-8).dp).size(10.dp).creamCard(CircleShape, elevation = 2.dp, border = 2.dp))
        Row(
            Modifier
                .align(Alignment.TopEnd)
                .size(width = 50.dp, height = 28.dp)
                .creamCard(CircleShape, elevation = 4.dp, border = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) { Box(Modifier.size(6.dp).clip(CircleShape).background(dot)) }
        }
    }
}

/**
 * Выдан ли доступ к микрофону. Только проверка, без запроса: запрос — у переключателя в настройках.
 * Проверяется заново, когда меняется [key] — например, повтор слов только что включили.
 */
@Composable
private fun rememberMicrophoneGranted(key: Boolean): Boolean {
    val context = LocalContext.current
    return remember(key) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
}

// ---------- Меню и солнышко недели ----------

private val MenuHeight = 124.dp

@Composable
private fun BottomMenu(state: HomeUiState, onAction: (HomeAction) -> Unit, target: (TutorialStep) -> Modifier = { Modifier }) {
    val open = { section: HomeSection -> onAction(HomeAction.OpenSection(section)) }
    Row(Modifier.fillMaxWidth().height(MenuHeight), verticalAlignment = Alignment.Bottom) {
        MenuItem("Задания", Modifier.weight(1f), badge = state.tasksBadge, iconModifier = target(TutorialStep.TASKS), onClick = { open(HomeSection.TASKS) }) {
            Sticker(R.drawable.ic_nav_tasks, Color(0xFFE6F8F2))
        }
        MenuItem("Магазин", Modifier.weight(1f), iconModifier = target(TutorialStep.SHOP), onClick = { open(HomeSection.SHOP) }) {
            Sticker(R.drawable.ic_nav_shop, Color(0xFFFFF0E6))
        }
        WeekSun(state, Modifier.weight(1f), sunModifier = target(TutorialStep.DEEDS)) { onAction(HomeAction.ShowDeeds) }
        MenuItem("Гардероб", Modifier.weight(1f), onClick = { open(HomeSection.WARDROBE) }) {
            Sticker(R.drawable.ic_nav_wardrobe, Color(0xFFE4F3FF))
        }
        val goal = state.goal
        MenuItem(
            "Копилка",
            Modifier.weight(1f),
            description = goal?.let { "Копилка. Цель: ${it.name}, ${state.savings} из ${it.cost}" },
            iconModifier = target(TutorialStep.GOAL),
            onClick = { open(HomeSection.SAVINGS) },
        ) {
            if (goal == null) {
                Sticker(R.drawable.ic_nav_savings, Color(0xFFFFE6F0))
            } else {
                // Кольцо цели: сколько уже накоплено
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
    iconModifier: Modifier = Modifier,
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
            Box(Modifier.padding(top = 8.dp).then(iconModifier)) {
                icon()
                if (badge > 0) {
                    Surface(
                        shape = CircleShape,
                        color = FinniColors.ActionPeach,
                        border = BorderStroke(3.dp, Color.White),
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
private fun WeekSun(state: HomeUiState, modifier: Modifier = Modifier, sunModifier: Modifier = Modifier, onClick: () -> Unit) {
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
                .then(sunModifier)
                .semantics {
                    role = Role.Button
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
            border = BorderStroke(3.dp, Color.White),
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
            border = BorderStroke(2.dp, Color.White),
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
        pet = Pet.newborn("Финни", PetLook(PetColor.MINT)).grow(3),   // в превью 3D-питомец не рисуется: petHost = null
        balance = 45,
        savings = 20,
        goal = HomeUiState.Goal(name = "Домик", cost = 60),
        week = 3,
        speech = HomeUiState.Speech.HUNGRY,
        tasksBadge = 1,
        activeTask = HomeUiState.ActiveTask(title = "Раздели 60 монет", reward = 5, adventure = false),
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
