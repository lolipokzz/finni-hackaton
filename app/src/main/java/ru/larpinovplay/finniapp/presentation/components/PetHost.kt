package ru.larpinovplay.finniapp.presentation.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlin.math.roundToInt

/** Какого питомца рисовать. Главный экран собирает это из состояния и отдаёт в [PetHostState]. */
data class PetSpec(
    val assetName: String,
    val tintArgb: Long?,
    /** Рост на экране относительно взрослого (малыш и подросток меньше), см. PetAssets.modelScale. */
    val modelScale: Float = 1f,
    /** Раскраска: текстура шерсти из assets; null — как в модели (см. PetAssets.skinAsset). */
    val skin: String? = null,
    val animationsEnabled: Boolean,
    val soundEnabled: Boolean = true,
    /** Слушать микрофон и повторять (настройка, звук и выданное разрешение вместе). */
    val voiceEnabled: Boolean = false,
    val idleAnimation: String? = "Idle",
    val tapAnimation: String = "Wave",
    val hitAnimations: PetHitAnimations = PetHitAnimations(),
    val pettingAnimation: String? = null,
    /** Узлы надетых вещей в модели (Acc_*); остальные вещи модели скрыты. */
    val accessories: Set<String> = emptySet(),
)

/** Экраны, на которых виден питомец. Показывает тот, кто последним стал верхним экраном. */
enum class PetHostOwner { CREATION, HOME, WARDROBE }

/**
 * Мост между экранами с питомцем и [PetHost]. Экран пишет сюда, что показать ([spec]) и где ([setSlot]),
 * и говорит, когда питомца можно показывать ([show]); хост читает.
 *
 * Экранов с питомцем несколько (главный и гардероб), а вид один: слот и видимость принимаются только от
 * [owner] — экрана, который последним стал верхним. Поэтому уходящий экран, который ещё дорисовывает переход
 * или уходит из композиции, не спрячет питомца и не утащит его в свой слот.
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

    /** Место под питомца на экране [owner]. */
    var slot by mutableStateOf<LayoutCoordinates?>(null)
        private set

    /**
     * Где слот в окне при последней раскладке. Координаты слота — один и тот же объект, даже когда слот
     * поменял размер или место, поэтому хост следит за этим прямоугольником, а не за [slot].
     */
    var slotBounds by mutableStateOf<Rect?>(null)
        private set

    /** true, когда экран с питомцем наверху; иначе питомец скрыт и стоит на паузе. */
    var shown by mutableStateOf(false)
        private set

    /** Что сделать, когда на питомца нажали (кроме его собственной анимации); задаёт экран-владелец. */
    var onTap: () -> Unit = {}

    /**
     * Элементы экрана под питомцем, которые сами принимают нажатия: область в координатах окна и действие.
     * Вид питомца лежит поверх экрана и иначе забрал бы нажатие. Ставятся модификатором [petShield].
     */
    private val shields = mutableMapOf<Any, Pair<Rect, () -> Unit>>()

    internal fun setShield(key: Any, bounds: Rect, onTap: () -> Unit) {
        shields[key] = bounds to onTap
    }

    internal fun removeShield(key: Any) {
        shields.remove(key)
    }

    /**
     * Действие элемента под точкой ([x], [y] — координаты окна) или null, если там только питомец.
     * Если элементы вложены (подсказка и её кнопка), побеждает самый маленький — тот, в который целились.
     */
    fun shieldAt(x: Float, y: Float): (() -> Unit)? = shields.values
        .filter { (bounds, _) -> bounds.contains(Offset(x, y)) }
        .minByOrNull { (bounds, _) -> bounds.width * bounds.height }
        ?.second

    /** Экран, который сейчас показывает питомца. */
    var owner by mutableStateOf<PetHostOwner?>(null)
        private set

    /** [visible] = true — [owner] стал верхним экраном; false — перестал (чужой false ничего не делает). */
    fun show(owner: PetHostOwner, visible: Boolean) {
        if (visible) {
            if (this.owner != owner) {   // слот прошлого экрана здесь не годится
                slot = null
                slotBounds = null
            }
            this.owner = owner
            shown = true
        } else if (this.owner == owner) {
            shown = false
        }
    }

    fun setSlot(owner: PetHostOwner, coordinates: LayoutCoordinates) {
        if (this.owner == owner || this.owner == null) {
            slot = coordinates
            slotBounds = coordinates.boundsInWindow()
        }
    }
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
    cameraDistance: Float = PET_CAMERA_DISTANCE,
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
        // Пока экран не сообщил, какой питомец нужен (при запуске главный экран уже открыт, а игра ещё читается
        // с диска), вид стоит за экраном и ничего не рисует: пустые кадры только отодвигали загрузку настоящей
        // модели. Если ждать долго, в это время прогревается любая модель — материалы и шейдеры у всех общие
        val warming = state.spec == null
        val spec = state.spec ?: state.warmUp.takeIf { warm }

        // Слот читаем заново на каждом показе: во время анимации перехода его координаты не итоговые.
        // Когда Home ушёл, слот отсоединён; берём последнее известное место, питомец всё равно скрыт.
        val live = if (!warming && state.shown && state.slot?.isAttached == true) state.slotBounds else null
        if (live != null) lastBounds.value = live
        val bounds = live ?: lastBounds.value

        val density = LocalDensity.current
        val started = LocalLifecycleOwner.current.lifecycle.currentStateAsState().value.isAtLeast(Lifecycle.State.STARTED)
        val place = Modifier
            .offset {
                if (live != null) IntOffset((live.left - origin.x).roundToInt(), (live.top - origin.y).roundToInt())
                else IntOffset(OFFSCREEN, OFFSCREEN)
            }
            .size(
                width = bounds?.let { with(density) { it.width.toDp() } } ?: FALLBACK_SIZE,
                height = bounds?.let { with(density) { it.height.toDp() } } ?: FALLBACK_SIZE,
            )

        // Тень под лапами — под видом питомца (он лежит поверх окна), в тех же координатах. Читается при
        // отрисовке, поэтому каждый кадр перерисовывается только этот холст, без перекомпоновки
        val shadow = remember { mutableStateOf<PetShadow?>(null) }
        Canvas(place) { shadow.value?.let { drawPetShadow(it) } }

        PetModel3D(
            assetName = spec?.assetName,
            tintArgb = spec?.tintArgb,
            modelScale = spec?.modelScale ?: 1f,
            skin = spec?.skin,
            cameraDistance = cameraDistance,
            animationsEnabled = spec?.animationsEnabled ?: true,
            soundEnabled = spec?.soundEnabled ?: false,
            voiceEnabled = spec?.voiceEnabled ?: false,
            idleAnimation = spec?.idleAnimation,
            tapAnimation = spec?.tapAnimation ?: "Wave",
            hitAnimations = spec?.hitAnimations ?: PetHitAnimations(),
            pettingAnimation = spec?.pettingAnimation,
            accessories = spec?.accessories.orEmpty(),
            onShadow = { shadow.value = it },
            // Свёрнутое приложение — пауза: не рисует, молчит и не слушает микрофон; видимое (STARTED и выше) — работает
            active = !warming && state.shown && started,
            onTap = { state.onTap() },
            shieldAt = state::shieldAt,

            modifier = place,
        )
    }
}

/** Мягкая тень: размытый эллипс под питомцем и более плотные пятна контакта под лапами. */
private fun DrawScope.drawPetShadow(shadow: PetShadow) {
    softEllipse(shadow.x, shadow.y, shadow.radiusX, shadow.radiusY, SHADOW_ALPHA * shadow.strength)
    shadow.feet.forEach { softEllipse(it.x, it.y, it.radiusX, it.radiusY, FOOT_ALPHA * it.strength) }
}

private fun DrawScope.softEllipse(x: Float, y: Float, radiusX: Float, radiusY: Float, alpha: Float) {
    if (radiusX <= 0f || radiusY <= 0f || alpha <= 0f) return
    val center = Offset(x, y)
    withTransform({ scale(1f, radiusY / radiusX, pivot = center) }) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to SHADOW_COLOR.copy(alpha = alpha),
                0.5f to SHADOW_COLOR.copy(alpha = alpha * 0.8f),
                1f to Color.Transparent,
                center = center,
                radius = radiusX,
            ),
            radius = radiusX,
            center = center,
        )
    }
}

/** Тёплый тёмный, а не чистый чёрный: на цветном полу чёрная тень выглядит дырой. */
private val SHADOW_COLOR = Color(0xFF2A1430)
private const val SHADOW_ALPHA = 0.7f
private const val FOOT_ALPHA = 0.9f

/**
 * Расстояние камеры до питомца в [PetHost]: одинаковое на всех экранах, поэтому одинаков и масштаб питомца.
 * Этой же камерой снят фон-комната (room_background.jpg): при смене расстояния фон нужно перерендерить.
 */
const val PET_CAMERA_DISTANCE = 3.1f

private const val OFFSCREEN = -100_000

private const val WARM_UP_DELAY_FRAMES = 5

private val FALLBACK_SIZE = 320.dp

private class BoundsHolder {
    var value: Rect? = null
}

/**
 * Элемент под видом питомца, который сам принимает нажатия: касание над ним получает [onTap], а не питомец.
 * Нужен всему, что может оказаться на слоте питомца (облачко «…», подсказка обучения). Без [host] (превью,
 * экраны без питомца) ничего не делает. Уйдя с экрана, элемент снимает свой щит сам.
 */
fun Modifier.petShield(host: PetHostState?, onTap: () -> Unit): Modifier =
    if (host == null) this else this then PetShieldElement(host, onTap)

private data class PetShieldElement(val host: PetHostState, val onTap: () -> Unit) : ModifierNodeElement<PetShieldNode>() {
    override fun create() = PetShieldNode(host, onTap)
    override fun update(node: PetShieldNode) {
        node.host.removeShield(node)
        node.host = host
        node.onTap = onTap
    }
}

private class PetShieldNode(var host: PetHostState, var onTap: () -> Unit) : Modifier.Node(), GlobalPositionAwareModifierNode {
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) =
        host.setShield(this, coordinates.boundsInWindow()) { onTap() }

    override fun onDetach() = host.removeShield(this)
}
