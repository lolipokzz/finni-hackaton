package ru.larpinovplay.finniapp.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import ru.larpinovplay.finniapp.R
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.tan

/**
 * Фон-комната, в которой питомец стоит на полу.
 *
 * Картинка снята в Blender той же камерой, что и 3D-питомец: камера смотрит
 * горизонтально на центр модели. Поэтому достаточно поставить кадр так, как его увидела бы камера питомца:
 * центр кадра — на центр слота питомца, масштаб — по размеру слота (слот охватывает 45° обзора, см. PetModel3D).
 * Тогда пол и горизонт совпадают с питомцем на любом экране. Тень под лапами рисует PetHost (она двигается).
 *
 * Где слот, фон узнаёт из [anchor] (экран отмечает слот питомца и себя, см. [roomPetSlot] и [roomOrigin]).
 * Экраны без питомца передают null: комната ставится так, будто питомец стоит посередине экрана.
 *
 * Купленные вещи для комнаты ([LocalRoomDecor]) — прозрачные слои, снятые той же камерой с тенью на полу и стене.
 * Каждый слой — вырезанный кусок кадра, поэтому он ставится в свою долю того же кадра и встаёт точно на место.
 * Питомец (вид поверх окна) всегда перед ними; сами вещи стоят там, где их не закрывает ни он, ни мебель.
 */
@Composable
fun RoomBackground(modifier: Modifier = Modifier, anchor: RoomAnchor? = null) {
    val image = ImageBitmap.imageResource(R.drawable.room_background)
    val owned = LocalRoomDecor.current
    val decor = RoomDecor.entries.filter { it.itemId in owned }.map { it to ImageBitmap.imageResource(it.image) }
    Canvas(modifier.fillMaxSize()) {
        val slot = anchor?.slot?.takeIf { it.width > 0f } ?: defaultSlot(size)
        val (topLeft, frame) = placeFrame(slot, size)
        drawImage(
            image = image,
            dstOffset = IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()),
            dstSize = IntSize(frame.width.roundToInt(), frame.height.roundToInt()),
            filterQuality = FilterQuality.Medium,
        )
        for ((item, layer) in decor) {
            val left = topLeft.x + item.area.left * frame.width
            val top = topLeft.y + item.area.top * frame.height
            drawImage(
                image = layer,
                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                dstSize = IntSize((item.area.width * frame.width).roundToInt(), (item.area.height * frame.height).roundToInt()),
                filterQuality = FilterQuality.Medium,
            )
        }
    }
}

/** id купленных вещей для комнаты (ShopItem.decor). Задаётся один раз на всё приложение из игры. */
val LocalRoomDecor = compositionLocalOf<Set<String>> { emptySet() }

/**
 * Вещи для комнаты: id товара, слой и где он в кадре room_background (доли ширины и высоты кадра).
 * Слои отрендерены той же камерой, что и фон; положение — то, куда их вырезали из полного кадра.
 */
private enum class RoomDecor(val itemId: String, @DrawableRes val image: Int, val area: Rect) {
    /** Кошачья лежанка слева спереди, рядом с питомцем. */
    BED("bed", R.drawable.room_bed, Rect(0.21667f, 0.57458f, 0.46083f, 0.63083f)),

    /** Детский велосипед вдоль стены справа, под тумбой. */
    BIKE("bike", R.drawable.room_bike, Rect(0.6225f, 0.52042f, 0.7775f, 0.5725f)),
}

/** Где на экране слот питомца. Экран с питомцем создаёт его, отмечает им себя и слот и отдаёт фону. */
@Stable
class RoomAnchor {
    internal var origin by mutableStateOf(Offset.Zero)
    internal var slotInRoot by mutableStateOf<Rect?>(null)

    /** Слот в координатах фона. */
    internal val slot: Rect? get() = slotInRoot?.translate(-origin)
}

@Composable
fun rememberRoomAnchor(): RoomAnchor = remember { RoomAnchor() }

/** Ставится на контейнер, который целиком занимает фон (обычно корневой Box экрана). */
fun Modifier.roomOrigin(anchor: RoomAnchor): Modifier = onGloballyPositioned { anchor.origin = it.positionInRoot() }

/** Ставится на слот 3D-питомца. */
fun Modifier.roomPetSlot(anchor: RoomAnchor): Modifier = onGloballyPositioned { anchor.slotInRoot = it.boundsInRoot() }

/**
 * Кадр по слоту: в слот стороной S камера питомца укладывает 2·tan(22.5°) по тангенсу угла, значит на единицу
 * тангенса приходится S / (2·tan 22.5°) пикселей, а весь кадр — 2·[TAN_HALF_H] × 2·[TAN_HALF_V] таких единиц.
 * Если кадр всё же не закрывает экран (очень широкий экран, крошечный слот), он увеличивается вокруг слота:
 * перспектива чуть расходится, зато без пустых полос.
 */
private fun placeFrame(slot: Rect, screen: Size): Pair<Offset, Size> {
    val center = slot.center
    val pxPerTan = slot.height / (2 * tan(Math.toRadians(PET_FOV_DEGREES / 2)).toFloat())
    var width = 2 * TAN_HALF_H * pxPerTan
    var height = 2 * TAN_HALF_V * pxPerTan
    val cover = max(
        max(center.x, screen.width - center.x) / (width / 2),
        max(center.y, screen.height - center.y) / (height / 2),
    )
    if (cover > 1f) {
        width *= cover
        height *= cover
    }
    return Offset(center.x - width / 2, center.y - height / 2) to Size(width, height)
}

/** Слот «как на главном экране», когда настоящего нет: во всю ширину, чуть ниже середины. */
private fun defaultSlot(screen: Size): Rect {
    val side = minOf(screen.width, screen.height * 0.45f)
    val center = Offset(screen.width / 2, screen.height * 0.55f)
    return Rect(center - Offset(side / 2, side / 2), Size(side, side))
}

/** Вертикальный угол обзора камеры питомца (ModelViewer), градусы. */
private const val PET_FOV_DEGREES = 45.0

/** Кадр room_background: тангенсы половины угла обзора, с которыми он отрендерен. */
private const val TAN_HALF_V = 1.6f
private const val TAN_HALF_H = 0.8f
