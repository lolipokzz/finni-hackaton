package ru.larpinovplay.finniapp.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Какого питомца рисовать. Главный экран собирает это из состояния и отдаёт в [PetHostState]. */
data class PetSpec(
    val assetName: String,
    val tintArgb: Long?,
    val animationsEnabled: Boolean,
    val idleAnimation: String? = "Idle",
    val tapAnimation: String = "Wave",
)

/**
 * Мост между главным экраном и [PetHost]. Экран пишет сюда, что показать ([spec]) и где ([slot]),
 * и говорит, когда питомца можно показывать ([shown]); хост читает.
 *
 * Питомец живёт вне записи back stack'а: экран Home уходит из композиции, когда сверху другой экран,
 * а модель (движок Filament, загруженный glTF, фаза анимации) должна пережить это.
 */
@Stable
class PetHostState(
    /** Модель для прогрева: рисуется невидимо, пока настоящего питомца ещё нет, чтобы первый показ не ждал шейдеров. */
    val warmUp: PetSpec? = null,
) {
    /** Модель, которую нужно показать; null — модели для этого питомца нет (или экран ещё не сообщил). */
    var spec by mutableStateOf<PetSpec?>(null)

    /** Место под питомца на главном экране. */
    var slot by mutableStateOf<LayoutCoordinates?>(null)

    /** true, когда Home наверху; иначе питомец скрыт и стоит на паузе. */
    var shown by mutableStateOf(false)
}

/**
 * Рисует питомца поверх всего приложения (над экраном создания питомца и над графом навигации).
 *
 * Вид и движок Filament создаются один раз, сразу после первого кадра, пока пользователь ещё выбирает питомца:
 * это ~0.3 с работы на главном потоке, которые не должны попасть на первый показ главного экрана. Дальше вид
 * только загружает нужную модель, двигается на место и прячется. Прячем сдвигом за экран и паузой отрисовки, а не
 * через видимость: SurfaceView, ставший невидимым, теряет поверхность, и создавать её заново дорого. Сдвиг же
 * заодно уводит вид из hit-теста Compose, иначе невидимый AndroidView глотал бы касания экрана под собой.
 *
 * SurfaceView питомца лежит поверх окна и не следует за анимацией переходов Compose, поэтому переходы между
 * экранами не должны двигать содержимое (см. MainNavigation), а питомец показывается, как только Home становится
 * целью перехода, и скрывается, как только он перестаёт ею быть.
 *
 * Размер вида задаётся местом на главном экране. Пока его нет, вид имеет запасной размер: меняется он один раз,
 * при первом показе главного экрана, а не на каждом возврате.
 */
@Composable
fun PetHost(
    state: PetHostState,
    modifier: Modifier = Modifier,
    cameraDistance: Float = 3.1f,
) {
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withFrameNanos { }   // первый кадр экрана создания питомца уже на экране
        ready = true
    }
    if (!ready) return

    // Прогрев не в том же кадре, что создание вида: главный поток и так занят этим ~0.3 с
    var warm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        repeat(WARM_UP_DELAY_FRAMES) { withFrameNanos { } }
        warm = true
    }

    var origin by remember { mutableStateOf(Offset.Zero) }
    val lastBounds = remember { BoundsHolder() }

    Box(modifier = modifier
        .fillMaxSize()
        .onGloballyPositioned { origin = it.positionInWindow() }
    ) {
        // Пока главный экран не показан, вместо его модели можно прогревать любую: шейдеры общие
        val spec = state.spec ?: state.warmUp.takeIf { warm && !state.shown }

        // Слот читаем заново на каждом показе: во время анимации перехода его координаты не итоговые.
        // Когда Home ушёл, слот отсоединён; берём последнее известное место, питомец всё равно скрыт.
        val live = if (state.shown) state.slot?.takeIf { it.isAttached }?.boundsInWindow() else null
        if (live != null) lastBounds.value = live
        val bounds = live ?: lastBounds.value

        val density = LocalDensity.current
        PetModel3D(
            assetName = spec?.assetName,
            tintArgb = spec?.tintArgb,
            cameraDistance = cameraDistance,
            animationsEnabled = spec?.animationsEnabled ?: true,
            idleAnimation = spec?.idleAnimation,
            tapAnimation = spec?.tapAnimation ?: "Wave",
            active = state.shown,
            modifier = Modifier
                .offset {
                    if (live != null) IntOffset((live.left - origin.x).roundToInt(), (live.top - origin.y).roundToInt())
                    else IntOffset(OFFSCREEN, OFFSCREEN)
                }
                .size(
                    width = bounds?.let { with(density) { it.width.toDp() } } ?: FALLBACK_SIZE,
                    height = bounds?.let { with(density) { it.height.toDp() } } ?: FALLBACK_SIZE,
                ),
        )
    }
}

private const val OFFSCREEN = -100_000

private const val WARM_UP_DELAY_FRAMES = 5

private val FALLBACK_SIZE = 320.dp

private class BoundsHolder {
    var value: Rect? = null
}
