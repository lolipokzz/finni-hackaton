package ru.larpinovplay.finniapp.presentation.screens.adventure

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.adventure.model.AdventureScene
import ru.larpinovplay.finniapp.presentation.adventure.AdventureLook
import ru.larpinovplay.finniapp.presentation.components.TealButton
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/** Купюра или монета, которую сейчас тащат: где её левый верхний угол (в координатах окна) и откуда её взяли. */
private data class Drag(val index: Int, val topLeft: Offset, val size: IntSize, val fromCounter: Boolean) {
    val center: Offset get() = topLeft + Offset(size.width / 2f, size.height / 2f)
}

/**
 * Оплата: купюры и монеты перетаскивают из кошелька на прилавок пальцем, как в настоящем магазине.
 * Отпустил над прилавком — положил; стащил с прилавка — вернул в кошелёк; отпустил мимо — вернулась на место.
 * Нажатием не перекладываются, но у TalkBack есть действие «положить» и «вернуть», чтобы оплатить можно было и без жеста.
 * Где сейчас палец — состояние самого жеста, оно живёт здесь; что лежит на прилавке — в [AdventureUiState].
 */
@Composable
internal fun PayScene(scene: AdventureScene.Pay, state: AdventureUiState, look: AdventureLook, onAction: (AdventureAction) -> Unit) {
    val locked = state.check != null
    var drag by remember { mutableStateOf<Drag?>(null) }
    var origin by remember { mutableStateOf(Offset.Zero) }       // где эта карточка в окне
    var counterBounds by remember { mutableStateOf(Rect.Zero) }
    val overCounter = drag?.let { counterBounds.contains(it.center) } == true

    val onDragStart: (Int, LayoutCoordinates, Boolean) -> Unit = { index, coordinates, fromCounter ->
        drag = Drag(index, coordinates.positionInRoot(), coordinates.size, fromCounter)
    }
    val onDragBy: (Offset) -> Unit = { amount -> drag = drag?.let { it.copy(topLeft = it.topLeft + amount) } }
    val onDragEnd: () -> Unit = {
        drag?.let { d ->
            val onCounter = counterBounds.contains(d.center)
            // Переложить, только если бросили в другое место: с кошелька на прилавок или с прилавка прочь
            if (onCounter != d.fromCounter) onAction(AdventureAction.TogglePiece(d.index))
        }
        drag = null
    }
    val handlers = DragHandlers(enabled = !locked, onDragStart, onDragBy, onDragEnd, onCancel = { drag = null })

    Box(Modifier.onGloballyPositioned { origin = it.positionInRoot() }) {
        SceneCard(R.drawable.ic_coin, "Оплата", look) {
                BodyText(scene.text)
                Hint(scene.hint)

                Counter(
                    pieces = state.onCounter,
                    wallet = scene.wallet,
                    sum = state.counterSum,
                    highlighted = overCounter && drag?.fromCounter == false,
                    dragging = drag?.index,
                    handlers = handlers,
                    onToggle = { onAction(AdventureAction.TogglePiece(it)) },
                    modifier = Modifier.onGloballyPositioned { counterBounds = it.boundsInRoot() },
                )
                Wallet(
                    pieces = scene.wallet.indices.filter { it !in state.onCounter },
                    wallet = scene.wallet,
                    dragging = drag?.index,
                    handlers = handlers,
                    onToggle = { onAction(AdventureAction.TogglePiece(it)) },
                )

                if (!locked) {
                    TealButton("Заплатить ${state.counterSum}", R.drawable.ic_coin, { onAction(AdventureAction.Pay) }, enabled = state.onCounter.isNotEmpty())
                }
        }

            // Купюра под пальцем рисуется поверх всей карточки, чтобы её не закрывали ни прилавок, ни кошелёк
            drag?.let { d ->
                val topLeft = d.topLeft - origin
                MoneyFace(
                    value = scene.wallet[d.index],
                    modifier = Modifier
                        .zIndex(1f)
                        .offset { IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()) }
                        .shadow(10.dp, if (scene.wallet[d.index] >= BILL_MIN) RoundedCornerShape(10.dp) else CircleShape),
                )
            }
    }
}

/**
 * Прилавок: деревянная зона под полосатым навесом, как у лавки. Подсвечивается бирюзовой рамкой,
 * когда над ней несут купюру.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Counter(
    pieces: List<Int>,
    wallet: List<Int>,
    sum: Int,
    highlighted: Boolean,
    dragging: Int?,
    handlers: DragHandlers,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp)
            .clip(shape)
            .background(FinniColors.Counter)
            .border(if (highlighted) 4.dp else 2.dp, if (highlighted) FinniColors.Teal else FinniColors.CounterEdge, shape),
    ) {
        Awning()
        Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Прилавок", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF7A4B1F), modifier = Modifier.weight(1f))
                Row(
                    Modifier.clip(CircleShape).background(Color.White).padding(start = 4.dp, end = 12.dp, top = 3.dp, bottom = 3.dp)
                        .semantics(mergeDescendants = true) { contentDescription = "На прилавке $sum" },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Image(painterResource(R.drawable.ic_coin), null, Modifier.size(24.dp))
                    Text("$sum", fontSize = 19.sp, fontWeight = FontWeight.Black, color = FinniColors.CoinInk)
                }
            }
            if (pieces.isEmpty()) {
                Text("Перетащи сюда купюры и монеты из кошелька", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9A7040))
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pieces.forEach { index ->
                        DraggablePiece(index, wallet[index], fromCounter = true, dragging == index, handlers, "вернуть в кошелёк") { onToggle(index) }
                    }
                }
            }
        }
    }
}

/** Полосатый навес лавки с фестонами по нижнему краю. */
@Composable
private fun Awning() {
    Canvas(Modifier.fillMaxWidth().height(26.dp)) {
        // Фестон — полукруг под каждой полосой: полос столько, чтобы он целиком уместился в высоту навеса
        val scallop = size.height * 0.45f
        val stripes = (size.width / (scallop * 2)).toInt().coerceAtLeast(1)
        val w = size.width / stripes
        val band = size.height - w / 2
        repeat(stripes) { i ->
            val color = if (i % 2 == 0) Color(0xFFFF8A7A) else Color.White
            drawRect(color, topLeft = Offset(i * w, 0f), size = Size(w, band))
            drawCircle(color, radius = w / 2, center = Offset(i * w + w / 2, band))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Wallet(
    pieces: List<Int>,
    wallet: List<Int>,
    dragging: Int?,
    handlers: DragHandlers,
    onToggle: (Int) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0xFFEEF0FF)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Кошелёк", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF2A2F6B))
        if (pieces.isEmpty()) {
            Text("Пусто: всё на прилавке", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pieces.forEach { index ->
                    DraggablePiece(index, wallet[index], fromCounter = false, dragging == index, handlers, "положить на прилавок") { onToggle(index) }
                }
            }
        }
    }
}

private class DragHandlers(
    val enabled: Boolean,
    val onStart: (index: Int, coordinates: LayoutCoordinates, fromCounter: Boolean) -> Unit,
    val onDrag: (Offset) -> Unit,
    val onEnd: () -> Unit,
    val onCancel: () -> Unit,
)

/**
 * Купюра, которую можно тащить. Пока её несут, на месте остаётся бледный след, а сама она рисуется
 * поверх карточки (см. [PayScene]).
 */
@Composable
private fun DraggablePiece(
    index: Int,
    value: Int,
    fromCounter: Boolean,
    dragged: Boolean,
    handlers: DragHandlers,
    accessibilityAction: String,
    onAccessibilityMove: () -> Unit,
) {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val current by rememberUpdatedState(handlers)
    val name = if (value >= BILL_MIN) "Купюра $value" else "Монета $value"
    MoneyFace(
        value = value,
        modifier = Modifier
            .onGloballyPositioned { coordinates = it }
            .alpha(if (dragged) 0.3f else 1f)
            .semantics {
                contentDescription = name
                if (handlers.enabled) onClick(label = accessibilityAction) { onAccessibilityMove(); true }
            }
            .pointerInput(index, fromCounter, handlers.enabled) {
                if (!handlers.enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { coordinates?.let { current.onStart(index, it, fromCounter) } },
                    onDrag = { change, amount -> change.consume(); current.onDrag(amount) },
                    onDragEnd = { current.onEnd() },
                    onDragCancel = { current.onCancel() },
                )
            },
    )
}

/**
 * Купюра — зелёная бумажка с рамкой-узором, монета — золотой кружок с ободком:
 * отличаются формой, а не только цветом.
 */
@Composable
private fun MoneyFace(value: Int, modifier: Modifier = Modifier) {
    if (value >= BILL_MIN) {
        Box(
            modifier.size(width = 80.dp, height = 52.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFCFF3E2))
                .border(2.dp, Color(0xFF4CC38A), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.padding(5.dp).fillMaxSize().border(1.5.dp, Color(0xFF9FE3C2), RoundedCornerShape(7.dp)))
            Text("$value", fontSize = 21.sp, fontWeight = FontWeight.Black, color = Color(0xFF0B5E4F))
        }
    } else {
        Box(
            modifier.size(54.dp).clip(CircleShape).background(Color(0xFFFFC23D)).border(2.dp, Color(0xFFF0A31A), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFFFDD7A)))
            Text("$value", fontSize = 19.sp, fontWeight = FontWeight.Black, color = FinniColors.CoinInk)
        }
    }
}

/** С какого номинала деньги — купюра, а не монета. */
private const val BILL_MIN = 10
