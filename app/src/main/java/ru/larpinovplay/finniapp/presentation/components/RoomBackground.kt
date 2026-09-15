package ru.larpinovplay.finniapp.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * Фон «детская комната»: стена, окно, пол, коврик, кресло-мешок и растение.
 * Рисуется примитивами, чтобы не тянуть большую картинку и легко менять цвета.
 * Когда появится иллюстрация художника, заменить на Image с той же композицией:
 * линия пола на ~60 % высоты, коврик под ногами питомца.
 */
@Composable
fun RoomBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val floorTop = h * 0.60f

        // Стена
        drawRect(
            brush = Brush.verticalGradient(listOf(FinniColors.Wall, FinniColors.WallBottom), endY = floorTop),
            size = Size(w, floorTop)
        )

        // Окно-арка справа: белая рама, небо и холм, обрезанные по форме окна
        val winW = w * 0.30f
        val winH = h * 0.24f
        val winLeft = w * 0.64f
        val winTop = h * 0.25f
        val frame = 10f * density
        fun arch(left: Float, top: Float, width: Float, height: Float) = Path().apply {
            val r = width / 2
            moveTo(left, top + height)
            lineTo(left, top + r)
            arcTo(Rect(left, top, left + width, top + 2 * r), 180f, 180f, false)
            lineTo(left + width, top + height)
            close()
        }
        drawPath(arch(winLeft - frame, winTop - frame, winW + 2 * frame, winH + 2 * frame), Color.White)
        clipPath(arch(winLeft, winTop, winW, winH)) {
            drawRect(
                brush = Brush.verticalGradient(listOf(FinniColors.WindowSkyTop, FinniColors.WindowSkyBottom), startY = winTop, endY = winTop + winH),
                topLeft = Offset(winLeft, winTop),
                size = Size(winW, winH)
            )
            drawCircle(FinniColors.Plant.copy(alpha = 0.7f), radius = winW * 0.55f, center = Offset(winLeft + winW * 0.55f, winTop + winH * 1.05f))
            drawCircle(Color.White.copy(alpha = 0.8f), radius = winW * 0.09f, center = Offset(winLeft + winW * 0.3f, winTop + winH * 0.3f))
        }
        // Подоконник
        drawRoundRect(Color.White, topLeft = Offset(winLeft - frame * 1.6f, winTop + winH), size = Size(winW + frame * 3.2f, frame), cornerRadius = CornerRadius(frame / 2))

        // Пол
        drawRect(
            brush = Brush.verticalGradient(listOf(FinniColors.FloorTop, FinniColors.FloorBottom), startY = floorTop, endY = h),
            topLeft = Offset(0f, floorTop),
            size = Size(w, h - floorTop)
        )
        // Плинтус
        drawRect(Color.White.copy(alpha = 0.7f), topLeft = Offset(0f, floorTop - 6f * density), size = Size(w, 6f * density))

        // Кресло-мешок слева
        drawCircle(FinniColors.Beanbag, radius = w * 0.20f, center = Offset(w * 0.0f, floorTop + h * 0.03f))
        drawCircle(Color.White.copy(alpha = 0.25f), radius = w * 0.08f, center = Offset(-w * 0.02f, floorTop - h * 0.02f))

        // Коврик
        val rugW = w * 0.92f
        val rugH = h * 0.12f
        val rugTopLeft = Offset((w - rugW) / 2, h * 0.655f - rugH / 2)
        drawOval(FinniColors.RugLight, topLeft = rugTopLeft, size = Size(rugW, rugH))
        drawOval(
            FinniColors.RugRing,
            topLeft = Offset(rugTopLeft.x + rugW * 0.06f, rugTopLeft.y + rugH * 0.12f),
            size = Size(rugW * 0.88f, rugH * 0.76f),
            style = Stroke(width = 8f * density)
        )
    }
}
