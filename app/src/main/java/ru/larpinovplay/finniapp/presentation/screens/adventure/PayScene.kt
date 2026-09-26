package ru.larpinovplay.finniapp.presentation.screens.adventure

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ru.larpinovplay.finniapp.domain.adventure.model.AdventureScene
import ru.larpinovplay.finniapp.presentation.theme.FinniColors
import kotlin.math.roundToInt

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
internal fun PayScene(scene: AdventureScene.Pay, state: AdventureUiState, onAction: (AdventureAction) -> Unit) {
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

    Card {
        Box(Modifier.onGloballyPositioned { origin = it.positionInRoot() }) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(scene.text, style = MaterialTheme.typography.bodyLarge)
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
                    Button(
                        onClick = { onAction(AdventureAction.Pay) },
                        enabled = state.onCounter.isNotEmpty(),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text("Заплатить ${state.counterSum}", style = MaterialTheme.typography.titleMedium) }
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
                        .shadow(8.dp, if (scene.wallet[d.index] >= BILL_MIN) RoundedCornerShape(10.dp) else CircleShape),
                )
            }
        }
    }
}

/** Прилавок: заметная деревянная зона. Подсвечивается рамкой, когда над ней несут купюру. */
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
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = FinniColors.FloorTop,
        border = BorderStroke(if (highlighted) 4.dp else 2.dp, if (highlighted) FinniColors.Blue else FinniColors.FloorBottom),
        modifier = modifier.fillMaxWidth().heightIn(min = 140.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Прилавок", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("$sum", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            if (pieces.isEmpty()) {
                Text(
                    "Перетащи сюда купюры и монеты из кошелька",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FinniColors.NavyMuted,
                )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Wallet(
    pieces: List<Int>,
    wallet: List<Int>,
    dragging: Int?,
    handlers: DragHandlers,
    onToggle: (Int) -> Unit,
) {
    Surface(shape = RoundedCornerShape(18.dp), color = FinniColors.Lavender, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Кошелёк", style = MaterialTheme.typography.titleMedium)
            if (pieces.isEmpty()) {
                Text("Пусто: всё на прилавке", style = MaterialTheme.typography.bodyMedium, color = FinniColors.NavyMuted)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pieces.forEach { index ->
                        DraggablePiece(index, wallet[index], fromCounter = false, dragging == index, handlers, "положить на прилавок") { onToggle(index) }
                    }
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

/** Купюра — зелёный прямоугольник, монета — жёлтый кружок: отличаются формой, а не только цветом. */
@Composable
private fun MoneyFace(value: Int, modifier: Modifier = Modifier) {
    val bill = value >= BILL_MIN
    Surface(
        shape = if (bill) RoundedCornerShape(10.dp) else CircleShape,
        color = if (bill) FinniColors.CardMint else FinniColors.Sunny,
        border = BorderStroke(2.dp, if (bill) FinniColors.Green else FinniColors.Satiety),
        modifier = modifier.size(width = if (bill) 76.dp else 52.dp, height = 52.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("$value", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = FinniColors.Navy)
        }
    }
}

/** С какого номинала деньги — купюра, а не монета. */
private const val BILL_MIN = 10
