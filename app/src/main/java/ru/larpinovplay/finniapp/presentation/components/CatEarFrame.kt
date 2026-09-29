package ru.larpinovplay.finniapp.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * Рамка-мордочка: кремовая карточка приложения (как creamCard — белая обводка, мягкая тень) с двумя кошачьими ушками по
 * верхним углам, розовыми вставками в ушках и полосками на лбу.
 */
@Composable
fun CatEarFrame(
    modifier: Modifier = Modifier,
    colors: CatEarColors = CatEarColors.Cream,
    earHeight: Dp = CatEarShape.EAR_HEIGHT,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = CatEarShape(earHeight)
    Box(
        modifier
            .shadow(20.dp, shape, ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.3f))
            .drawBehind {
                val outline = shape.createOutline(size, layoutDirection, this) as Outline.Generic
                clipPath(outline.path) {
                    drawRect(colors.background)
                    val w = size.width
                    val ear = earHeight.toPx()
                    // Внутренняя часть ушек: мягкий розовый «треугольник» со скруглённой вершиной
                    val tip = w * CatEarShape.TIP
                    val base = w * CatEarShape.BASE
                    // Треугольник целиком внутри уха: вершина под вершиной уха, низ чуть выше лба, углы скруглены
                    val bottom = ear * 0.92f
                    val left = tip * 0.55f
                    val right = base * 0.68f
                    val round = 6.dp.toPx()
                    fun innerEar(mirror: Boolean) = Path().apply {
                        fun x(v: Float) = if (mirror) w - v else v
                        moveTo(x(left + round), bottom)
                        quadraticTo(x(left), bottom, x(left + round * 0.25f), bottom - round)
                        cubicTo(x(left + (tip - left) * 0.35f), ear * 0.5f, x(tip * 0.9f), ear * 0.28f, x(tip * 1.05f), ear * 0.28f)
                        cubicTo(x(tip * 1.25f), ear * 0.28f, x(right - (right - tip) * 0.35f), ear * 0.55f, x(right - round * 0.3f), bottom - round)
                        quadraticTo(x(right), bottom, x(right - round), bottom)
                        close()
                    }
                    drawPath(innerEar(false), colors.innerEar)
                    drawPath(innerEar(true), colors.innerEar)
                    // Полоски на лбу, между ушками
                    val stripeW = 7.dp.toPx()
                    val stripeH = 16.dp.toPx()
                    listOf(-1, 0, 1).forEach { i ->
                        val cx = w / 2 + i * 18.dp.toPx()
                        drawRoundRect(
                            colors.stripe,
                            topLeft = Offset(cx - stripeW / 2, ear + 4.dp.toPx() - if (i == 0) 3.dp.toPx() else 0f),
                            size = Size(stripeW, stripeH + if (i == 0) 4.dp.toPx() else 0f),
                            cornerRadius = CornerRadius(stripeW / 2),
                        )
                    }
                }
            }
            .border(4.dp, Color.White, shape),
        content = content,
    )
}

/** Отступ содержимого сверху: ушки и полоски на лбу. */
val CatEarFrameContentTop: Dp = CatEarShape.EAR_HEIGHT + 24.dp

@Immutable
data class CatEarColors(val background: Color, val innerEar: Color, val stripe: Color) {
    companion object {
        /** Как остальные карточки: кремовая, розовые ушки, тёплые полоски. */
        val Cream = CatEarColors(
            background = FinniColors.Cream,
            innerEar = Color(0xFFFFDCE8),
            stripe = Color(0xFFF6E3C1),
        )
    }
}

/**
 * Контур мордочки: скруглённая карточка, у которой верхние углы вытянуты в мягкие кошачьи ушки.
 * Внешний край уха продолжает бок карточки, внутренний плавно спускается ко лбу.
 */
class CatEarShape(private val earHeight: Dp = EAR_HEIGHT, private val corner: Dp = 38.dp) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val e = with(density) { earHeight.toPx() }
        val r = with(density) { corner.toPx() }
        val tip = w * TIP
        val base = w * BASE
        val path = Path().apply {
            moveTo(0f, e + r)
            lineTo(0f, h - r)
            quadraticTo(0f, h, r, h)
            lineTo(w - r, h)
            quadraticTo(w, h, w, h - r)
            lineTo(w, e + r)
            // правое ухо: наружный край вверх к вершине, внутренний — вниз ко лбу
            cubicTo(w, e * 0.25f, w - tip * 0.45f, 0f, w - tip, 0f)
            cubicTo(w - tip * 1.55f, 0f, w - base * 0.85f, e * 0.8f, w - base, e)
            lineTo(base, e)
            // левое ухо, зеркально
            cubicTo(base * 0.85f, e * 0.8f, tip * 1.55f, 0f, tip, 0f)
            cubicTo(tip * 0.45f, 0f, 0f, e * 0.25f, 0f, e + r)
            close()
        }
        return Outline.Generic(path)
    }

    companion object {
        val EAR_HEIGHT = 46.dp

        /** Вершина уха от края и место, где ухо переходит в лоб, — доли ширины. */
        const val TIP = 0.12f
        const val BASE = 0.29f
    }
}
