package ru.larpinovplay.finniapp.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/** Какого питомца рисовать. Главный экран собирает это из состояния и отдаёт в [PetHostState]. */
data class PetSpec(
    val assetName: String,
    val tintArgb: Long?,
    val cameraDistance: Float,
    val animationsEnabled: Boolean,
)

/**
 * Мост между главным экраном и [PetHost]. Экран пишет сюда, что показать ([spec]) и где ([slot]),
 * и говорит, когда питомца можно показывать ([shown]); хост читает.
 *
 * Питомец живёт вне записи back stack'а: экран Home уходит из композиции, когда сверху другой экран,
 * а модель (движок Filament, загруженный glTF, фаза анимации) должна пережить это.
 */
@Stable
class PetHostState {
    var spec by mutableStateOf<PetSpec?>(null)

    /** Место под питомца на главном экране. */
    var slot by mutableStateOf<LayoutCoordinates?>(null)

    /** true, когда Home наверху и переход закончился; иначе питомец скрыт и стоит на паузе. */
    var shown by mutableStateOf(false)
}

/**
 * Рисует питомца поверх всего графа навигации. SurfaceView питомца лежит поверх окна и не следует
 * за анимацией переходов Compose, поэтому он появляется, только когда Home стоит на месте,
 * и скрывается сразу, как только начинается переход.
 * Кладётся вторым слоем после NavDisplay в контейнер того же размера.
 */
@Composable
fun PetHost(state: PetHostState, modifier: Modifier = Modifier) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    val lastBounds = remember { BoundsHolder() }

    Box(modifier = modifier
        .fillMaxSize()
        .onGloballyPositioned { origin = it.positionInWindow() }
    ) {
        val spec = state.spec ?: return@Box

        // Слот читаем заново на каждом показе: во время анимации перехода его координаты не итоговые.
        // Когда Home ушёл, слот отсоединён; берём последнее известное место, питомец всё равно скрыт.
        val live = if (state.shown) state.slot?.takeIf { it.isAttached }?.boundsInWindow() else null
        if (live != null) lastBounds.value = live
        val bounds = live ?: lastBounds.value ?: return@Box

        val density = LocalDensity.current
        PetModel3D(
            assetName = spec.assetName,
            tintArgb = spec.tintArgb,
            cameraDistance = spec.cameraDistance,
            animationsEnabled = spec.animationsEnabled,
            active = state.shown,
            modifier = Modifier
                .offset {
                    // Невидимый AndroidView всё равно участвует в hit-тесте Compose и глотал бы касания
                    // экрана под собой, поэтому на паузе уводим его за пределы экрана. Размер не трогаем:
                    // после изменения размера SurfaceView с Filament перестаёт рисовать до следующего кадра Compose.
                    if (state.shown) IntOffset((bounds.left - origin.x).roundToInt(), (bounds.top - origin.y).roundToInt())
                    else IntOffset(OFFSCREEN, OFFSCREEN)
                }
                .size(with(density) { bounds.width.toDp() }, with(density) { bounds.height.toDp() }),
        )
    }
}

private const val OFFSCREEN = -100_000

private class BoundsHolder {
    var value: Rect? = null
}
