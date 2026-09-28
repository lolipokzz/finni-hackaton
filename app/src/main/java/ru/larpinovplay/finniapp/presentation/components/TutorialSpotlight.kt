package ru.larpinovplay.finniapp.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Места на экране, на которые Финни просит нажать во время обучения: прямоугольники в координатах окна.
 * Элементы отмечают себя через [spotlightTarget]; ушедший со экрана элемент (прокрутка) убирает себя сам.
 */
class SpotlightTargets {
    private val rects = mutableStateMapOf<Any, Rect>()

    operator fun get(key: Any): Rect? = rects[key]

    /** Все отмеченные места одним прямоугольником: «выбери любое из этих». */
    val all: Rect?
        get() = rects.values.reduceOrNull { a, b ->
            Rect(minOf(a.left, b.left), minOf(a.top, b.top), maxOf(a.right, b.right), maxOf(a.bottom, b.bottom))
        }

    internal fun put(key: Any, rect: Rect) { rects[key] = rect }
    internal fun remove(key: Any) { rects.remove(key) }
}

/** Отметить элемент как место для подсветки обучения. Видимая часть: обрезанное прокруткой не считается. */
fun Modifier.spotlightTarget(targets: SpotlightTargets, key: Any): Modifier = this then SpotlightTargetElement(targets, key)

private data class SpotlightTargetElement(val targets: SpotlightTargets, val key: Any) : ModifierNodeElement<SpotlightTargetNode>() {
    override fun create() = SpotlightTargetNode(targets, key)
    override fun update(node: SpotlightTargetNode) {
        node.targets.remove(node.key)
        node.targets = targets
        node.key = key
    }
}

private class SpotlightTargetNode(var targets: SpotlightTargets, var key: Any) : Modifier.Node(), GlobalPositionAwareModifierNode {
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) = targets.put(key, coordinates.boundsInWindow())
    override fun onDetach() = targets.remove(key)
}

/**
 * Подсветка шага обучения: экран затемнён, открыто только [hole] (в координатах окна) — туда и нужно нажать,
 * всё остальное не нажимается. Подсказка Финни — сверху или снизу, там, где не закрывает подсвеченное;
 * в ней же «Пропустить обучение». [round] — круг со стрелкой для одной кнопки, иначе скруглённая рамка
 * вокруг группы, из которой можно выбрать любое. Пока место не измерено, закрыто всё.
 * Затемнение не анимируется: под видом питомца постоянная перерисовка затемняет весь его квадрат.
 */
@Composable
fun TutorialSpotlight(text: String, hole: Rect?, onSkip: () -> Unit, modifier: Modifier = Modifier, round: Boolean = false) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val screen = Rect(0f, 0f, size.width.toFloat(), size.height.toFloat())
    // Рамка не уходит за край: обрезанная прокруткой группа видна целиком, с рамкой по краю экрана
    val visible = screen.deflate(with(density) { 6.dp.toPx() })
    val local = hole?.translate(-origin)
    val radius = local?.let { maxOf(it.width, it.height) / 2 + with(density) { 10.dp.toPx() } } ?: 0f
    // Открытая часть: круг вписан в квадрат, рамка — чуть шире самих элементов
    val open = local?.let {
        if (round) Rect(it.center, radius) else it.inflate(with(density) { 8.dp.toPx() })
    }?.intersect(visible)?.takeIf { it.width > 0f && it.height > 0f }

    Box(modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInWindow(); size = it.size }) {
        // До первого измерения ни размера экрана, ни места подсветки ещё нет: подсказка встала бы сверху
        // и через кадр прыгнула вниз. Место отмечается в том же проходе раскладки, так что ждём один кадр
        if (size == IntSize.Zero) return@Box
        Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            drawRect(Color(0xFF1E1438).copy(alpha = 0.5f))
            if (open == null) return@Canvas
            val ring = Stroke(4.dp.toPx())
            if (round) {
                val c = open.center
                drawCircle(Color.Transparent, radius, c, blendMode = BlendMode.Clear)
                drawCircle(Color.White, radius, c, style = ring)
                // Стрелка смотрит на то, что нажать: над кругом, а у кнопки в верхней половине — под ним
                val up = if (c.y < this.size.height / 2) -1f else 1f
                val tipY = c.y - up * (radius + 8.dp.toPx())
                val w = 14.dp.toPx()
                val h = 16.dp.toPx() * up
                drawPath(
                    Path().apply {
                        moveTo(c.x, tipY)
                        lineTo(c.x - w, tipY - h)
                        lineTo(c.x + w, tipY - h)
                        close()
                    },
                    Color.White,
                )
            } else {
                val corner = CornerRadius(30.dp.toPx())
                drawRoundRect(Color.Transparent, open.topLeft, open.size, corner, blendMode = BlendMode.Clear)
                drawRoundRect(Color.White, open.topLeft, open.size, corner, style = ring)
            }
        }
        // Нажатия мимо подсвеченного гасятся: четыре полосы вокруг открытой части
        if (open == null) Blocker(screen)
        else {
            Blocker(Rect(0f, 0f, screen.right, open.top))
            Blocker(Rect(0f, open.bottom, screen.right, screen.bottom))
            Blocker(Rect(0f, open.top, open.left, open.bottom))
            Blocker(Rect(open.right, open.top, screen.right, open.bottom))
        }
        val noteOnTop = open == null || open.center.y > screen.height / 2
        CoachNote(
            text,
            onSkip = onSkip,
            modifier = Modifier
                .align(if (noteOnTop) Alignment.TopCenter else Alignment.BottomCenter)
                .padding(start = 14.dp, end = 14.dp, top = 92.dp, bottom = 24.dp),
        )
    }
}

@Composable
private fun Blocker(r: Rect) {
    if (r.width <= 0f || r.height <= 0f) return
    val density = LocalDensity.current
    Box(
        Modifier
            .offset { IntOffset(r.left.roundToInt(), r.top.roundToInt()) }
            .size(with(density) { r.width.toDp() }, with(density) { r.height.toDp() })
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            },
    )
}
