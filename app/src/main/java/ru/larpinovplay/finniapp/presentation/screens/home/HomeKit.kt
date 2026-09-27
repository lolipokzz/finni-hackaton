package ru.larpinovplay.finniapp.presentation.screens.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/*
 * Общий язык главного экрана и его окон — «наклейки»: кремовая поверхность, толстая белая обводка,
 * мягкая нейтральная тень. Так выглядят верхние кольца, табличка Финни, реплика, меню, дела недели и план.
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
    shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.7f), Offset(0f, 2f), blurRadius = 6f),
)

/**
 * Кольцо-показатель 0–100: цвет заполняет кольцо по часовой от верха, в середине белый круг со значком.
 * Значение всегда озвучивается словами ([description]), цвет — не единственный сигнал.
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
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit = { Image(painterResource(icon), null, Modifier.size(size * 0.45f)) },
) {
    val amount = value.coerceIn(0, 100)
    val body: @Composable () -> Unit = {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(size)) {
                drawCircle(track)
                drawArc(color, startAngle = -90f, sweepAngle = 360f * amount / 100f, useCenter = true)
                drawCircle(Color.White, radius = this.size.minDimension * 0.355f)
            }
            content()
        }
    }
    val shape = CircleShape
    val base = modifier
        .size(size)
        .shadow(8.dp, shape, ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.3f))
        .border(3.dp, Color.White, shape)
        .semantics { contentDescription = description }
    if (onClick != null) Surface(onClick = onClick, shape = shape, color = Color.Transparent, modifier = base, content = body)
    else Surface(shape = shape, color = Color.Transparent, modifier = base, content = body)
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
    Surface(onClick = onClick, shape = CircleShape, color = color, modifier = modifier.height(48.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, color = ink, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (arrow) Image(painterResource(R.drawable.ic_arrow_right), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(ink))
        }
    }
}

/** Круглая служебная кнопка 48 dp: закрыть, настройки. */
@Composable
fun PebbleButton(@DrawableRes icon: Int, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = FinniColors.Pebble) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = color,
        modifier = modifier.size(48.dp).semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) { Image(painterResource(icon), null, Modifier.size(22.dp)) }
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
        fun tail(inset: Float) = androidx.compose.ui.graphics.Path().apply {
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
fun CardSticker(@DrawableRes icon: Int, tint: Color, modifier: Modifier = Modifier, size: Dp = 64.dp) {
    Box(
        modifier.size(size).creamCard(CircleShape, elevation = 5.dp, border = 3.dp).background(tint),
        contentAlignment = Alignment.Center,
    ) { Image(painterResource(icon), null, Modifier.size(size * 0.56f)) }
}

/** Главная кнопка карточки: бирюзовая, во всю ширину. Выключенная — серая, без тени. */
@Composable
fun TealButton(text: String, @DrawableRes icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (enabled) FinniColors.Teal else FinniColors.Pebble,
        shadowElevation = if (enabled) 6.dp else 0.dp,
        modifier = modifier.fillMaxWidth().height(56.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(icon), null, Modifier.size(24.dp).alpha(if (enabled) 1f else 0.5f))
            Text(text, fontSize = 17.sp, fontWeight = FontWeight.Black, color = if (enabled) Color.White else FinniColors.InkMuted)
        }
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
 * Окно-карточка по центру экрана (план недели, итоги): кремовая наклейка с прокруткой.
 * Закрывается только своей кнопкой — эти окна ведут ребёнка по шагам недели.
 */
@Composable
fun CardDialog(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.layout.Column(
                Modifier
                    .fillMaxWidth()
                    .creamCard(androidx.compose.foundation.shape.RoundedCornerShape(34.dp), elevation = 20.dp)
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
    androidx.compose.foundation.layout.Column {
        Text(
            title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = FinniColors.Ink,
            modifier = Modifier.semantics { heading() },
        )
        Text(subtitle, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = FinniColors.Teal)
    }
}
