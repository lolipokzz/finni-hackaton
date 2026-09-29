package ru.larpinovplay.finniapp.presentation.screens.tasks

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.data.content.defaultContent
import ru.larpinovplay.finniapp.domain.adventure.model.Adventure
import ru.larpinovplay.finniapp.domain.content.FeedbackKey
import ru.larpinovplay.finniapp.domain.game.engine.GameRules
import ru.larpinovplay.finniapp.domain.game.model.LevelStatus
import ru.larpinovplay.finniapp.domain.task.model.Level
import ru.larpinovplay.finniapp.domain.task.model.TaskTopic
import ru.larpinovplay.finniapp.presentation.adventure.look
import ru.larpinovplay.finniapp.presentation.components.BubbleTail
import ru.larpinovplay.finniapp.presentation.components.OnRoomLabel
import ru.larpinovplay.finniapp.presentation.components.RoomBackground
import ru.larpinovplay.finniapp.presentation.components.ScreenHeader
import ru.larpinovplay.finniapp.presentation.components.SoftButton
import ru.larpinovplay.finniapp.presentation.components.SpotlightTargets
import ru.larpinovplay.finniapp.presentation.components.TealButton
import ru.larpinovplay.finniapp.presentation.components.TutorialSpotlight
import ru.larpinovplay.finniapp.presentation.components.creamCard
import ru.larpinovplay.finniapp.presentation.components.spotlightTarget
import ru.larpinovplay.finniapp.presentation.feedback.LocalFeedback
import ru.larpinovplay.finniapp.presentation.task.title
import ru.larpinovplay.finniapp.presentation.theme.FinniColors
import kotlin.math.sin

/** Карта заданий, как тропинка уроков: недели сверху вниз, в каждой — три уровня по темам и приключение недели. */
@Composable
fun TasksScreen(
    onOpenLevel: (Level, challenge: Boolean) -> Unit,
    onOpenAdventure: (Adventure) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TasksViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TasksScreenContent(
        state = state,
        onOpenLevel = onOpenLevel,
        onOpenAdventure = onOpenAdventure,
        onBack = onBack,
        modifier = modifier,
        onSkipTutorialStep = { viewModel.onAction(TasksAction.SkipTutorialStep) },
    )
}

@Composable
fun TasksScreenContent(
    state: TasksUiState,
    onOpenLevel: (Level, challenge: Boolean) -> Unit,
    onOpenAdventure: (Adventure) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSkipTutorialStep: () -> Unit = {},
) {
    // Какой узел раскрыт — только вид. В обучении раскрыт первый уровень: Финни показывает на «Начать»
    var picked by rememberSaveable { mutableStateOf<String?>(null) }
    val expanded = if (state.coach) state.current else picked
    val start = remember { SpotlightTargets() }
    val list = rememberLazyListState()
    val visibleWeek by remember(state.weeks) { derivedStateOf { weekAtRow(state, list.firstVisibleItemIndex) } }
    // Где на экране кнопки видимых узлов: к кнопке раскрытого крепится карточка со стрелкой
    val anchors = remember { mutableStateMapOf<String, Rect>() }
    var mapOrigin by remember { mutableStateOf(Offset.Zero) }
    // Карта открывается на узле «Начни отсюда»: пройденные недели остаются выше
    LaunchedEffect(Unit) { rowIndex(state, state.current)?.let { list.scrollToItem((it - 1).coerceAtLeast(0)) } }
    BackHandler(enabled = picked != null && !state.coach) { picked = null }
    // Касание мимо кнопок закрывает карточку; касание другого узла открывает его карточку
    Box(modifier = modifier.fillMaxSize().dismissOnTap { picked = null }) {
        RoomBackground()
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
            Spacer(Modifier.height(10.dp))
            ScreenHeader("Задания", onBack) {
                visibleWeek?.takeIf { it.maxStars > 0 }?.let { WeekStars(it) }
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .weight(1f)
                    .clipToBounds()   // карточка не заезжает на шапку, когда её узел уходит вверх
                    .onGloballyPositioned { mapOrigin = it.positionInRoot() },
            ) {
                LazyColumn(
                    state = list,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    // Запас снизу: карточка последнего узла помещается над краем экрана
                    contentPadding = PaddingValues(bottom = 200.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    var step = 0
                    state.weeks.forEach { week ->
                        item(key = "week-${week.number}") { WeekBanner(week, current = week.number == state.week) }
                        week.nodes.forEach { node ->
                            val shift = zigzag(step++)
                            item(key = node.id) {
                                NodeRow(
                                    node = node,
                                    lock = week.lock,
                                    week = week.number,
                                    shift = shift,
                                    current = node.id == state.current,
                                    expanded = node.id == expanded,
                                    onToggle = { picked = if (picked == node.id) null else node.id },
                                    onAnchor = { bounds -> if (bounds == null) anchors -= node.id else anchors[node.id] = bounds },
                                )
                            }
                        }
                    }
                }
                val popover = state.weeks.firstNotNullOfOrNull { week ->
                    week.nodes.firstOrNull { it.id == expanded }?.let { node ->
                        anchors[node.id]?.let { NodePopover(week, node, it.translate(-mapOrigin)) }
                    }
                }
                NodePopoverLayer(
                    popover = popover,
                    onOpenLevel = onOpenLevel,
                    onOpenAdventure = onOpenAdventure,
                    spotlight = start.takeIf { state.coach },
                )
            }
        }
        if (state.coach) {
            TutorialSpotlight(
                "Это карта заданий. Каждую неделю тут открываются новые уровни. Нажми «Начать» — пройдём первый вместе!",
                start.all,
                onSkip = onSkipTutorialStep,
                scroll = list,
            )
        }
    }
}

/** Номер строки узла [id] в списке карты: баннер недели, затем её узлы. */
private fun rowIndex(state: TasksUiState, id: String?): Int? {
    if (id == null) return null
    var index = 0
    state.weeks.forEach { week ->
        index++   // баннер
        week.nodes.forEach { node ->
            if (node.id == id) return index
            index++
        }
    }
    return null
}

/** Сдвиг узла вбок: тропинка петляет, как змейка. */
private fun zigzag(step: Int): Dp = (sin(step * 0.9) * 70).dp

/** Неделя, чья часть карты сейчас сверху экрана: по первой видимой строке списка. */
private fun weekAtRow(state: TasksUiState, row: Int): MapWeek? {
    var index = 0
    state.weeks.forEach { week ->
        index += 1 + week.nodes.size   // баннер и узлы
        if (row < index) return week
    }
    return state.weeks.lastOrNull()
}

/**
 * Звёзды недели, которая сейчас на экране: «Неделя 2 · ★ 7/9». Прокрутил к другой неделе — счётчик сменился
 * вместе с ней, с мягкой сменой цифр.
 */
@Composable
private fun WeekStars(week: MapWeek) {
    Column(
        Modifier
            .height(52.dp)
            .creamCard(CircleShape, elevation = 8.dp, border = 3.dp)
            .padding(horizontal = 14.dp)
            .clearAndSetSemantics { contentDescription = "Звёзд за неделю ${week.number}: ${week.stars} из ${week.maxStars}" },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedContent(targetState = week, contentKey = { it.number to it.stars }, label = "week-stars") { shown ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Неделя ${shown.number}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Image(
                        painterResource(R.drawable.ic_star_small), null, Modifier.size(20.dp),
                        colorFilter = ColorFilter.tint(if (shown.stars > 0) FinniColors.PawNew else FinniColors.Dashed),
                    )
                    Text("${shown.stars}/${shown.maxStars}", fontSize = 17.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink)
                }
            }
        }
    }
}

// ---------- Неделя ----------

/** Плашка недели: сейчас — жёлтая, пройденная — с галочкой, закрытая — серая с луной и словами, когда откроется. */
@Composable
private fun WeekBanner(week: MapWeek, current: Boolean) {
    val lockText = week.lock?.let { lockText(it, week.number) }
    val allDone = week.done == week.nodes.size
    val (icon, tint) = when {
        lockText != null -> R.drawable.ic_moon to Color(0xFFE9E2FF)
        allDone -> R.drawable.ic_check to FinniColors.Teal
        else -> R.drawable.ic_week_sun to FinniColors.Sunny
    }
    val subtitle = when {
        lockText != null -> lockText
        allDone -> "Пройдена"
        current -> "Сейчас · пройдено ${week.done} из ${week.nodes.size}"
        else -> "Пройдено ${week.done} из ${week.nodes.size}"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .creamCard(RoundedCornerShape(24.dp), elevation = if (current) 10.dp else 6.dp)
            .background(if (current) FinniColors.CoinPill else if (lockText != null) FinniColors.Pebble else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(44.dp).creamCard(CircleShape, elevation = 3.dp, border = 3.dp).background(tint),
            contentAlignment = Alignment.Center,
        ) { Image(painterResource(icon), null, Modifier.size(22.dp)) }
        Column(Modifier.weight(1f)) {
            Text("Неделя ${week.number}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = FinniColors.Ink)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (current) FinniColors.CoinInk else FinniColors.InkMuted)
        }
    }
}

@Composable
private fun lockText(lock: WeekLock, week: Int): String {
    val feedback = LocalFeedback.current
    return when (lock) {
        WeekLock.TOMORROW -> feedback.text(FeedbackKey.LEVEL_LOCKED_TOMORROW)
        WeekLock.NEXT_WEEK -> feedback.text(FeedbackKey.LEVEL_LOCKED_NEXT_WEEK)
        WeekLock.LATER -> feedback.text(FeedbackKey.LEVEL_LOCKED_LATER, "n" to week)
    }
}

/** Почему закрыт узел: ждёт предыдущий уровень, уровни недели (приключение) или свою неделю. */
@Composable
private fun nodeLockText(node: MapNode, lock: WeekLock?, week: Int): String {
    val feedback = LocalFeedback.current
    val waitsFor = (node as? MapNode.LevelNode)?.waitsFor
    return when {
        waitsFor != null -> feedback.text(FeedbackKey.LEVEL_LOCKED_ORDER, "level" to waitsFor)
        node is MapNode.AdventureNode && node.waitsForLevels -> feedback.text(FeedbackKey.ADVENTURE_LOCKED_LEVELS)
        else -> lockText(lock ?: WeekLock.LATER, week)
    }
}

// ---------- Узел тропинки ----------

/**
 * Узел со сдвигом [shift], подпись и звёзды под ним. У раскрытого подпись прячется: её место занимает карточка поверх
 * карты.
 */
@Composable
private fun NodeRow(
    node: MapNode,
    lock: WeekLock?,
    week: Int,
    shift: Dp,
    current: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAnchor: (Rect?) -> Unit,
) {
    DisposableEffect(Unit) { onDispose { onAnchor(null) } }
    val captionAlpha by animateFloatAsState(if (expanded) 0f else 1f, label = "caption")
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.offset(x = shift), horizontalAlignment = Alignment.CenterHorizontally) {
            if (current) {
                Text(
                    "Начни отсюда!", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = FinniColors.Teal,
                    modifier = Modifier.padding(bottom = 10.dp).clip(CircleShape).background(Color.White).padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
            val size = nodeSize(node, current)
            Box(contentAlignment = Alignment.Center) {
                // Текущий узел — последний открытый — ещё и с мягко расходящимся кольцом, чтобы его было видно сразу
                if (current) PulseRing(size)
                NodeButton(
                    node, size, description = nodeDescription(node, lock, week), onClick = onToggle,
                    modifier = Modifier.onGloballyPositioned { onAnchor(it.boundsInRoot()) },
                )
            }
            Column(Modifier.graphicsLayer { alpha = captionAlpha }, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    node.title, style = OnRoomLabel.copy(fontSize = 15.sp), textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp).widthIn(max = 160.dp).clearAndSetSemantics {},
                )
                if (node is MapNode.LevelNode && node.status == LevelStatus.DONE) Stars(node.stars, Modifier.padding(top = 4.dp))
            }
        }
    }
}

/** Приключение — самый крупный узел, текущий — крупнее остальных. */
private fun nodeSize(node: MapNode, current: Boolean): Dp = when {
    node is MapNode.AdventureNode -> 96.dp
    current -> 84.dp
    else -> 72.dp
}

/** Жёлтый ореол и кольцо, которое расходится от узла и тает, — снова и снова. Только вид, TalkBack его не читает. */
@Composable
private fun PulseRing(size: Dp) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val progress by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_400, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "ring",
    )
    Box(Modifier.size(size + 18.dp).background(FinniColors.PawNew.copy(alpha = 0.35f), CircleShape))
    Box(
        Modifier
            .size(size)
            .graphicsLayer {
                val scale = 1f + 0.45f * progress
                scaleX = scale
                scaleY = scale
                alpha = 1f - progress
            }
            .border(5.dp, FinniColors.PawNew, CircleShape),
    )
}

/**
 * Круглая кнопка узла: доступный — цвет темы или приключения, пройденный — бирюзовый с галочкой,
 * золотой — со звездой, закрытый — серый с замком. Текущий и приключение — крупнее.
 */
@Composable
private fun NodeButton(node: MapNode, size: Dp, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val gold = node is MapNode.LevelNode && node.gold
    val (icon, container) = when {
        node.status == LevelStatus.LOCKED -> R.drawable.ic_lock to FinniColors.Pebble
        gold -> R.drawable.ic_star_small to FinniColors.Gold
        node.status == LevelStatus.DONE -> R.drawable.ic_check to FinniColors.Teal
        node is MapNode.AdventureNode -> node.adventure.look.emblem to node.adventure.look.tint
        node is MapNode.LevelNode -> node.level.topic.sticker
        else -> R.drawable.ic_lock to FinniColors.Pebble
    }
    FilledIconButton(
        onClick = onClick,
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = container),
        modifier = modifier.size(size).creamCard(CircleShape, elevation = if (node.status == LevelStatus.LOCKED) 4.dp else 10.dp),
    ) {
        Image(
            painterResource(icon),
            contentDescription = description,
            modifier = Modifier.size(size * 0.45f),
            colorFilter = when {
                node.status == LevelStatus.LOCKED -> ColorFilter.tint(FinniColors.InkMuted.copy(alpha = 0.55f))
                gold -> ColorFilter.tint(Color.White)
                else -> null
            },
        )
    }
}

/** Что прочитает TalkBack: название и статус словами. */
@Composable
private fun nodeDescription(node: MapNode, lock: WeekLock?, week: Int): String {
    val status = when (node.status) {
        LevelStatus.AVAILABLE -> "Можно пройти"
        LevelStatus.DONE -> when (node) {
            is MapNode.LevelNode if node.gold -> "Золотой уровень"
            is MapNode.LevelNode -> "Пройден, звёзд ${node.stars} из ${GameRules.MAX_STARS}"
            else -> "Пройдено"
        }
        LevelStatus.LOCKED -> nodeLockText(node, lock, week)
    }
    val kind = if (node is MapNode.AdventureNode) "Приключение недели: " else ""
    return "$kind${node.title}. $status"
}

/** Звёзды уровня: заработанные — жёлтые, остальные — бежевые. Число озвучено в описании узла. */
@Composable
private fun Stars(stars: Int, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    Row(modifier.clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(GameRules.MAX_STARS) { i ->
            Image(
                painterResource(R.drawable.ic_star_small), null, Modifier.size(size),
                colorFilter = ColorFilter.tint(if (i < stars) FinniColors.PawNew else FinniColors.Dashed),
            )
        }
    }
}

// ---------- Карточка узла ----------

/** Раскрытый узел и где его кнопка в координатах карты. */
private data class NodePopover(val week: MapWeek, val node: MapNode, val anchor: Rect)

private val PopoverWidth = 280.dp

/** Слой карточки поверх карты: карточка висит под кнопкой узла со стрелкой к ней и не сдвигает другие узлы. */
@Composable
private fun NodePopoverLayer(
    popover: NodePopover?,
    onOpenLevel: (Level, Boolean) -> Unit,
    onOpenAdventure: (Adventure) -> Unit,
    spotlight: SpotlightTargets?,
) {
    AnimatedContent(
        targetState = popover,
        contentKey = { it?.node?.id },   // прокрутка двигает карточку, а не показывает её заново
        // Без анимации размера: иначе при закрытии слой сжимается и уносит карточку вниз, а не к стрелке
        transitionSpec = { (EnterTransition.None togetherWith ExitTransition.None).using(null) },
        modifier = Modifier.fillMaxSize(),
        label = "popover",
    ) { shown ->
        if (shown == null) return@AnimatedContent
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val width = PopoverWidth.coerceAtMost(maxWidth)
            val center = with(density) { shown.anchor.center.x.toDp() }
            val left = (center - width / 2).coerceIn(0.dp, maxWidth - width)
            val top = with(density) { shown.anchor.bottom.toDp() }
            val arrow = (center - left).coerceIn(ArrowHalf + 16.dp, width - ArrowHalf - 16.dp)
            Column(
                Modifier
                    .offset(x = left, y = top)
                    .width(width)
                    .animateEnterExit(
                        enter = fadeIn() + scaleIn(initialScale = 0.6f, transformOrigin = TransformOrigin(arrow / width, 0f)),
                        exit = fadeOut() + scaleOut(targetScale = 0.6f, transformOrigin = TransformOrigin(arrow / width, 0f)),
                    )
                    // Касание по самой карточке её не закрывает
                    .pointerInput(Unit) { detectTapGestures { } },
            ) {
                // Стрелка заходит на белую рамку карточки и сливается с ней
                BubbleTail(
                    Modifier.offset(x = arrow - ArrowHalf, y = 4.dp).graphicsLayer { scaleY = -1f }.zIndex(1f),
                    pointsLeft = false,
                )
                NodeCard(shown.node, shown.week.lock, shown.week.number, onOpenLevel, onOpenAdventure, spotlight)
            }
        }
    }
}

/** Половина ширины стрелки [BubbleTail]. */
private val ArrowHalf = 13.dp

/**
 * Касание, которое никто не забрал себе (не кнопка, не прокрутка), вызывает [onDismiss]. Смотрит в последнем
 * проходе, поэтому нажатие на узел или кнопку карточки работает как обычно.
 */
private fun Modifier.dismissOnTap(onDismiss: () -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
        if (waitForUpOrCancellation(PointerEventPass.Final) != null) onDismiss()
    }
}

/** Раскрытый узел: что внутри и что можно сделать — начать, пройти испытание, или когда откроется. */
@Composable
private fun NodeCard(
    node: MapNode,
    lock: WeekLock?,
    week: Int,
    onOpenLevel: (Level, Boolean) -> Unit,
    onOpenAdventure: (Adventure) -> Unit,
    spotlight: SpotlightTargets?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .creamCard(RoundedCornerShape(26.dp), elevation = 14.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(node.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = FinniColors.Ink)
                Text(nodeSubtitle(node), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
            }
            val reward = when (node) {
                is MapNode.LevelNode -> node.level.reward
                is MapNode.AdventureNode -> node.adventure.reward
            }
            if (node.status == LevelStatus.AVAILABLE) RewardChip(reward)
        }
        when (node.status) {
            LevelStatus.LOCKED -> CardNote(nodeLockText(node, lock, week))
            LevelStatus.AVAILABLE -> when (node) {
                is MapNode.LevelNode -> TealButton("Начать", R.drawable.ic_arrow_right, { onOpenLevel(node.level, false) },
                    spotlight?.let { Modifier.spotlightTarget(it, node.id) } ?: Modifier)
                is MapNode.AdventureNode -> TealButton("Вперёд!", R.drawable.ic_adventure, { onOpenAdventure(node.adventure) })
            }
            LevelStatus.DONE -> when (node) {
                is MapNode.LevelNode -> {
                    CardNote(
                        (if (node.gold) "Уровень уже золотой! " else "") +
                            "Золотое испытание: без подсказок и без ошибок за ${node.challengeMinutes} мин. Монет за него нет — только золото.",
                    )
                    ChallengeButton { onOpenLevel(node.level, true) }
                }
                is MapNode.AdventureNode -> SoftButton("Пройти ещё раз", { onOpenAdventure(node.adventure) })
            }
        }
    }
}

@Composable
private fun nodeSubtitle(node: MapNode): String = when (node) {
    is MapNode.LevelNode -> "${node.level.topic.title()} · ${tasksText(node.taskCount)}"
    is MapNode.AdventureNode -> "Приключение недели"
}

@Composable
private fun CardNote(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
}

/** Кнопка золотого испытания: жёлтая, как золото. */
@Composable
private fun ChallengeButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = FinniColors.Gold, contentColor = FinniColors.GoldInk),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Image(painterResource(R.drawable.ic_star_small), null, Modifier.padding(end = 10.dp).size(22.dp), colorFilter = ColorFilter.tint(FinniColors.GoldInk))
        Text("Золотое испытание", fontSize = 17.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun RewardChip(reward: Int) {
    Row(
        Modifier.clip(CircleShape).background(FinniColors.CoinPill).padding(start = 4.dp, end = 10.dp, top = 3.dp, bottom = 3.dp)
            .clearAndSetSemantics { contentDescription = "Награда: $reward" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(painterResource(R.drawable.ic_coin), null, Modifier.size(22.dp))
        Text("+$reward", fontSize = 15.sp, fontWeight = FontWeight.Black, color = FinniColors.CoinInk)
    }
}

// ---------- Темы ----------

internal val TaskTopic.sticker: Pair<Int, Color>
    get() = when (this) {
        TaskTopic.BUDGET -> R.drawable.ic_deed_plan to Color(0xFFE6EEFF)
        TaskTopic.SAVINGS -> R.drawable.ic_deed_pig to FinniColors.DreamTint
        TaskTopic.PAYMENTS -> R.drawable.ic_coin to FinniColors.CoinPill
    }

/** «1 задание», «4 задания», «5 заданий». */
private fun tasksText(n: Int): String = "$n " + when {
    n % 10 == 1 && n % 100 != 11 -> "задание"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "задания"
    else -> "заданий"
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun TasksScreenPreview() {
    val content = defaultContent()
    val weeks = (1..3).map { number ->
        val lock = if (number == 3) WeekLock.TOMORROW else null
        val nodes = content.levels.filter { it.week == number }.mapIndexed { i, level ->
            val status = when {
                number == 1 || (number == 2 && i == 0) -> LevelStatus.DONE
                number == 3 -> LevelStatus.LOCKED
                else -> LevelStatus.AVAILABLE
            }
            MapNode.LevelNode(level, status, stars = if (status == LevelStatus.DONE) 3 - i % 2 else 0, gold = number == 1 && i == 0, taskCount = 4, challengeMinutes = 2)
        } + MapNode.AdventureNode(content.adventures[number - 1], if (number == 1) LevelStatus.DONE else if (number == 2) LevelStatus.AVAILABLE else LevelStatus.LOCKED)
        MapWeek(number, lock, nodes, stars = nodes.filterIsInstance<MapNode.LevelNode>().sumOf { it.stars }, maxStars = 9)
    }
    TasksScreenContent(
        state = TasksUiState(weeks = weeks, week = 2, current = "w2_savings"),
        onOpenLevel = { _, _ -> },
        onOpenAdventure = {},
        onBack = {},
    )
}
