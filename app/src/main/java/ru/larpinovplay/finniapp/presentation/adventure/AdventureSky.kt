package ru.larpinovplay.finniapp.presentation.adventure

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Небо приключения: градиент из [AdventureLook], мягкие лучи из точки [sunX]·[sunY] (доли ширины и высоты)
 * и редкое конфетти. Рисуется за содержимым; ничего не анимирует — праздник не должен мешать читать.
 */
@Composable
fun AdventureSky(look: AdventureLook, modifier: Modifier = Modifier, sunX: Float = 0.5f, sunY: Float = 0.35f, confetti: Boolean = true) {
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(look.skyTop, look.skyBottom)))
        val center = Offset(size.width * sunX, size.height * sunY)
        val reach = size.maxDimension * 1.2f
        // Двенадцать широких лучей: чередуются с промежутками, поэтому читаются как солнце, а не как полосы
        repeat(12) { i ->
            rotate(i * 30f, center) {
                val half = (PI / 24).toFloat()
                val ray = Path().apply {
                    moveTo(center.x, center.y)
                    lineTo(center.x + reach * cos(-half), center.y + reach * sin(-half))
                    lineTo(center.x + reach * cos(half), center.y + reach * sin(half))
                    close()
                }
                drawPath(ray, Color.White.copy(alpha = 0.16f))
            }
        }
        if (confetti) {
            Confetti.forEach { (x, y, r, color) ->
                drawCircle(color.copy(alpha = 0.55f), radius = r * density, center = Offset(size.width * x, size.height * y))
            }
        }
    }
}

/** Где конфетти: доли ширины и высоты, радиус в dp и цвет. Одно и то же место на каждом кадре. */
private val Confetti = listOf(
    Dot(0.08f, 0.12f, 4f, Color(0xFFFF6FA8)),
    Dot(0.22f, 0.78f, 3f, Color(0xFF2EC4A6)),
    Dot(0.9f, 0.18f, 5f, Color(0xFFFFB020)),
    Dot(0.78f, 0.72f, 3.5f, Color(0xFF8FA4FF)),
    Dot(0.14f, 0.45f, 2.5f, Color(0xFFFFB020)),
    Dot(0.66f, 0.1f, 3f, Color(0xFF2EC4A6)),
    Dot(0.94f, 0.52f, 2.5f, Color(0xFFFF6FA8)),
    Dot(0.4f, 0.9f, 3f, Color(0xFFFF8A7A)),
)

private data class Dot(val x: Float, val y: Float, val r: Float, val color: Color)
