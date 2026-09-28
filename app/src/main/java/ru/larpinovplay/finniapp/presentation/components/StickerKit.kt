package ru.larpinovplay.finniapp.presentation.components

import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.semantics.clearAndSetSemantics

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.presentation.theme.FinniColors
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.window.DialogProperties

/*
 * Общий язык игровых экранов и их окон — «наклейки»: кремовая поверхность, толстая белая обводка,
 * мягкая нейтральная тень. Так выглядят главный экран (кольца, реплика, меню), дела недели, план, итоги и магазин.
 */

/** Кремовая карточка-наклейка. Тень нейтральная: цветная тень на фиолетовой комнате выглядит грязно. */
fun Modifier.creamCard(shape: Shape, elevation: Dp = 10.dp, border: Dp = 4.dp): Modifier =
    shadow(elevation, shape, ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.3f))
        .clip(shape)
        .background(FinniColors.Cream)
        .border(border, Color.White, shape)

/** Подпись поверх комнаты: белая с тёмной тенью, читается на любом месте фона. */
val OnRoomLabel = TextStyle(
    fontSize = 14.sp,
    fontWeight = FontWeight.Black,
    color = Color.White,
    shadow = Shadow(Color.Black.copy(alpha = 0.7f), Offset(0f, 2f), blurRadius = 6f),
)

/**
 * Кольцо-показатель 0–100: цвет заполняет кольцо по часовой от верха, в середине белый круг со значком.
 * Значение всегда озвучивается словами ([description]), цвет — не единственный сигнал.
 * Только показывает; нажимаемое кольцо — [MeterButton].
 */
@Composable
fun MeterRing(
    value: Int,
    color: Color,
    track: Color,
    @DrawableRes icon: Int,
    description: String,
    modifier: Modifier = Modifier,
    size: Dp = 62.dp,
    content: @Composable () -> Unit = { MeterIcon(icon, size) },
) {
    Box(modifier.meterFrame(size, description), contentAlignment = Alignment.Center) {
        MeterFace(value, color, track, size, content)
    }
}

/** То же кольцо, но кнопка: по нажатию объяснение показателя (главный экран). */
@Composable
fun MeterButton(
    value: Int,
    color: Color,
    track: Color,
    @DrawableRes icon: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 62.dp,
) {
    IconButton(onClick = onClick, modifier = modifier.meterFrame(size, description)) {
        MeterFace(value, color, track, size) { MeterIcon(icon, size) }
    }
}

private fun Modifier.meterFrame(size: Dp, description: String): Modifier = this
    .size(size)
    .shadow(8.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.3f))
    .clip(CircleShape)
    .border(3.dp, Color.White, CircleShape)
    .semantics { contentDescription = description }

@Composable
private fun MeterFace(value: Int, color: Color, track: Color, size: Dp, content: @Composable () -> Unit) {
    val amount = value.coerceIn(0, 100)
    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            drawCircle(track)
            drawArc(color, startAngle = -90f, sweepAngle = 360f * amount / 100f, useCenter = true)
            drawCircle(Color.White, radius = this.size.minDimension * 0.355f)
        }
        content()
    }
}

@Composable
private fun MeterIcon(@DrawableRes icon: Int, size: Dp) {
    Image(painterResource(icon), null, Modifier.size(size * 0.45f))
}

/** Круглая наклейка меню: тонированный круг в белой обводке. */
@Composable
fun Sticker(@DrawableRes icon: Int, tint: Color, modifier: Modifier = Modifier, size: Dp = 60.dp) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(8.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.3f))
            .clip(CircleShape)
            .background(tint)
            .border(3.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(icon), null, Modifier.size(size * 0.63f))
    }
}

/** Мягкая кнопка-пилюля: действие внутри карточки или реплики. Высота 48 dp — удобно маленькому пальцу. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = FinniColors.ActionPeach,
    ink: Color = FinniColors.ActionPeachInk,
    arrow: Boolean = false,
) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = ink),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier.height(48.dp),
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (arrow) {
            Image(painterResource(R.drawable.ic_arrow_right), null, Modifier.padding(start = 6.dp).size(16.dp), colorFilter = ColorFilter.tint(ink))
        }
    }
}

/** Круглая служебная кнопка 48 dp: закрыть, настройки. */
@Composable
fun PebbleButton(@DrawableRes icon: Int, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = FinniColors.Pebble) {
    FilledIconButton(
        onClick = onClick,
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = color),
        modifier = modifier.size(48.dp),
    ) {
        Image(painterResource(icon), contentDescription = description, modifier = Modifier.size(22.dp))
    }

}

/**
 * Шаги роста до следующей стадии: [earned] уже заработаны (зелёные), [fresh] — дела этой недели,
 * которые засчитаются в её конце (жёлтые), остальные — пустые.
 */
@Composable
fun Paws(growth: Growth, modifier: Modifier = Modifier, size: Dp = 15.dp) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        repeat(growth.total) { i ->
            val color = when {
                i < growth.earned -> FinniColors.TealBright
                i < growth.earned + growth.fresh -> FinniColors.PawNew
                else -> FinniColors.PawEmpty
            }
            Image(painterResource(R.drawable.ic_paw), null, Modifier.size(size), colorFilter = ColorFilter.tint(color))
        }
    }
}

/** Рост до следующей стадии в лапках; null — питомец уже взрослый. */
data class Growth(val total: Int, val earned: Int, val fresh: Int) {
    val left: Int get() = total - earned - fresh
}

fun Pet.growth(weekSteps: Int): Growth? {
    val next = growthStage.next ?: return null
    val total = next.minGrowthPoints - growthStage.minGrowthPoints
    val earned = growthPoints - growthStage.minGrowthPoints
    return Growth(total, earned, weekSteps.coerceAtMost(total - earned))
}

/** Треугольный хвостик облачка или карточки: белая обводка и кремовая заливка, как у самой наклейки. */
@Composable
fun BubbleTail(modifier: Modifier = Modifier, pointsLeft: Boolean = true) {
    Canvas(modifier.size(26.dp, 20.dp)) {
        val w = size.width
        val h = size.height
        fun tail(inset: Float) = Path().apply {
            val tip = if (pointsLeft) w * 0.2f else w * 0.5f
            moveTo(inset, 0f)
            lineTo(w - inset, 0f)
            lineTo(tip, h - inset * 1.4f)
            close()
        }
        drawPath(tail(0f), Color.White)
        drawPath(tail(4.dp.toPx()), FinniColors.Cream)
    }
}

/** Наклейка-кружок в карточках: тонированный круг в белой обводке с мягкой тенью. */
@Composable
fun CardSticker(@DrawableRes icon: Int, tint: Color, modifier: Modifier = Modifier, size: Dp = 64.dp, iconScale: Float = 0.56f) {
    Box(
        modifier.size(size).creamCard(CircleShape, elevation = 5.dp, border = 3.dp).background(tint),
        contentAlignment = Alignment.Center,
    ) { Image(painterResource(icon), null, Modifier.size(size * iconScale)) }
}

/** Главная кнопка карточки: бирюзовая, во всю ширину. Выключенная — серая, без тени. */
@Composable
fun TealButton(text: String, @DrawableRes icon: Int?, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = FinniColors.Teal,
            contentColor = Color.White,
            disabledContainerColor = FinniColors.Pebble,
            disabledContentColor = FinniColors.InkMuted,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp, pressedElevation = 6.dp, focusedElevation = 6.dp, hoveredElevation = 6.dp, disabledElevation = 0.dp,
        ),
        modifier = modifier.fillMaxWidth().height(56.dp),
    ) {
        // Значок — картинка, а не текст: у выключенной кнопки его приглушает alpha
        icon?.let { Image(painterResource(it), null, Modifier.padding(end = 10.dp).size(24.dp).alpha(if (enabled) 1f else 0.5f)) }
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Black)
    }

}

/** Пунктир на кремовом: отделяет части карточки. */
@Composable
fun DashedDivider(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(2.dp)) {
        drawLine(
            FinniColors.Dashed, start = Offset(0f, 0f), end = Offset(size.width, 0f),
            strokeWidth = size.height, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
        )
    }
}

/**
 * Окно-карточка по центру экрана: кремовая наклейка с прокруткой. Без [onDismiss] закрывается только
 * своей кнопкой — так план и итоги ведут ребёнка по шагам недели; с ним — ещё касанием мимо и «назад».
 */
@Composable
fun CardDialog(
    onDismiss: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = { onDismiss?.invoke() },
        properties = DialogProperties(
            dismissOnBackPress = onDismiss != null, dismissOnClickOutside = onDismiss != null, usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .creamCard(RoundedCornerShape(34.dp), elevation = 20.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
        }
    }
}

/** Заголовок карточки: крупная строка и бирюзовая подстрока, как у дел недели. */
@Composable
fun CardTitle(title: String, subtitle: String) {
    Column {
        Text(
            title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = FinniColors.Ink,
            modifier = Modifier.semantics { heading() },
        )
        Text(subtitle, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = FinniColors.Teal)
    }
}

/**
 * Монеты: жёлтая плашка-наклейка с монетой и числом — в шапках магазина и копилки.
 * Только показывает; нажимаемые монеты главного экрана — [CoinButton].
 */
@Composable
fun CoinPill(coins: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.coinFrame(coins).background(FinniColors.CoinPill).padding(CoinPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoinCount(coins, FinniColors.CoinInk)
    }
}

/** Те же монеты, но кнопка: по нажатию объяснение, откуда они берутся (главный экран). */
@Composable
fun CoinButton(coins: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = FinniColors.CoinPill, contentColor = FinniColors.CoinInk),
        contentPadding = CoinPadding,
        modifier = modifier.coinFrame(coins),
    ) {
        CoinCount(coins)
    }
}

private val CoinPadding = PaddingValues(start = 6.dp, end = 16.dp)

private fun Modifier.coinFrame(coins: Int): Modifier = this
    .height(52.dp)
    .creamCard(CircleShape, elevation = 8.dp, border = 3.dp)
    .semantics { contentDescription = "Монеты: $coins" }

/** Монета и число. Число озвучено в описании рамки, поэтому сам текст TalkBack не читает второй раз. */
@Composable
private fun CoinCount(coins: Int, color: Color = Color.Unspecified) {
    Image(painterResource(R.drawable.ic_coin), null, Modifier.padding(end = 6.dp).size(36.dp))
    Text("$coins", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color, modifier = Modifier.clearAndSetSemantics {})
}

/** Назад: круглая кремовая наклейка со стрелкой, как шестерёнка на главном экране. */
@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier.size(48.dp).creamCard(CircleShape, elevation = 8.dp, border = 3.dp)) {
        Image(
            painterResource(R.drawable.ic_arrow_right),
            contentDescription = "Назад",
            modifier = Modifier.size(22.dp).graphicsLayer(scaleX = -1f),
            colorFilter = ColorFilter.tint(FinniColors.Ink),
        )
    }
}

/** Вторая кнопка карточки рядом с [TealButton]: спокойная, серо-кремовая, во всю ширину. */
@Composable
fun SoftButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = FinniColors.Pebble, contentColor = FinniColors.InkMuted),
        modifier = modifier.fillMaxWidth().height(52.dp),
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Black)
    }
}

/** Что даёт покупка: значок и число на тонированной пилюле — «🍎 +30». Число всегда словом-цифрой, не только цветом. */
@Composable
fun EffectChip(@DrawableRes icon: Int, value: Int, tint: Color, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(CircleShape).background(tint).padding(start = 4.dp, end = 9.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Image(painterResource(icon), null, Modifier.size(18.dp))
        Text(if (value > 0) "+$value" else "$value", fontSize = 13.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink)
    }
}

/** «−» и «+» — такие же круглые кнопки, как «закрыть» у карточек. */
@Composable
fun StepButton(
    symbol: String,
    description: String,
    enabled: Boolean,
    color: Color = FinniColors.Pebble,
    ink: Color = FinniColors.Ink,
    disabledColor: Color = color.copy(alpha = color.alpha * DISABLED_ALPHA),
    disabledInk: Color = ink.copy(alpha = DISABLED_ALPHA),
    onClick: () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = color,
            contentColor = ink,
            disabledContainerColor = disabledColor,
            disabledContentColor = disabledInk,
        ),
        modifier = Modifier.size(48.dp).semantics { contentDescription = description },
    ) {
        // Знак — только картинка кнопки: TalkBack читает [description], а не «минус»
        Text(symbol, fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.clearAndSetSemantics {})
    }
}

private const val DISABLED_ALPHA = 0.4f

/** Строка «что изменится»: подпись слева, число справа; предупреждение — на тёплой пилюле и со словами. */
@Composable
fun StatRow(label: String, value: String, coin: Boolean = false, warn: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (warn) FinniColors.WarnInk else FinniColors.InkMuted, modifier = Modifier.weight(1f))
        Row(
            Modifier.clip(CircleShape).background(if (warn) FinniColors.WarnTint else Color.Transparent).padding(horizontal = if (warn) 10.dp else 0.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (coin) Image(painterResource(R.drawable.ic_coin), null, Modifier.padding(end = 4.dp).size(20.dp))
            Text(
                value, fontSize = 16.sp, fontWeight = FontWeight.Black,
                color = when {
                    warn -> FinniColors.WarnInk
                    coin -> FinniColors.CoinInk
                    else -> FinniColors.Ink
                },
            )
        }
    }
}

/** Шапка раздела поверх комнаты: «назад», белый заголовок с тенью и, справа, что-то своё раздела. */
@Composable
fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backEnabled: Boolean = true,
    trailing: @Composable () -> Unit = {},
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BackButton(onBack, enabled = backEnabled)
        Text(title, style = OnRoomLabel.copy(fontSize = 26.sp), modifier = Modifier.weight(1f).semantics { heading() })
        trailing()
    }
}

/** Шкала на кремовом: пунктирно-бежевая дорожка и цветная часть; [fraction] 0–1. */
@Composable
fun MeterBar(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 10.dp) {
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(FinniColors.Dashed)) {
        val f = fraction.coerceIn(0f, 1f)
        if (f > 0f) Box(Modifier.fillMaxWidth(f).height(height).clip(CircleShape).background(color))
    }
}

/** Бирюзовый кружок с галочкой в белой обводке: «сделано» на наклейках. */
@Composable
fun DoneBadge(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Box(
        modifier.size(size).clip(CircleShape).background(FinniColors.Teal).border(if (size >= 28.dp) 3.dp else 2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Image(painterResource(R.drawable.ic_check), null, Modifier.size(size * 0.46f)) }
}

/**
 * Подсказка Финни во время обучения: мятная плашка с лапкой — «это говорит Финни», текст и, если нужно,
 * «Пропустить шаг». Стоит прямо там, где нужно действие, а не отдельным окном.
 */
@Composable
fun CoachNote(text: String, modifier: Modifier = Modifier, onSkip: (() -> Unit)? = null, skipModifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .creamCard(RoundedCornerShape(24.dp), elevation = 8.dp)
            .background(FinniColors.CardMint)
            .padding(start = 12.dp, end = 14.dp, top = 12.dp, bottom = if (onSkip == null) 12.dp else 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CardSticker(R.drawable.ic_paw, Color.White, size = 40.dp, iconScale = 0.56f)
        Column(Modifier.weight(1f)) {
            Text("Финни", fontSize = 13.sp, fontWeight = FontWeight.Black, color = FinniColors.Teal)
            Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0B5E4F))
            onSkip?.let {
                TextButton(
                    onClick = it,
                    colors = ButtonDefaults.textButtonColors(contentColor = FinniColors.InkMuted),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.align(Alignment.End).then(skipModifier),
                ) {
                    Text("Пропустить шаг", fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

/**
 * Сообщение внизу экрана в стиле наклеек: кремовая плашка в белой обводке, текст и бирюзовая текстовая кнопка
 * (например, «Повторить»). TalkBack зачитывает его сам: область «живая».
 */
@Composable
fun FinniSnackbar(data: SnackbarData) {
    Row(
        Modifier
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .fillMaxWidth()
            .creamCard(RoundedCornerShape(24.dp), elevation = 10.dp)
            .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
            .heightIn(min = 52.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            data.visuals.message,
            style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.ExtraBold, color = FinniColors.Ink,
            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
        )
        data.visuals.actionLabel?.let { label ->
            TextButton(
                onClick = data::performAction,
                colors = ButtonDefaults.textButtonColors(contentColor = FinniColors.Teal),
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text(label, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

